package com.xirc.nichirin.client.light;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.shader.NichirinPostProcessor;
import com.xirc.nichirin.client.shader.NichirinShaderInjection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.lang.reflect.Method;

/**
 * Screen-space wisteria colored lighting for pipelines where the vanilla core-shader injection
 * cannot run (Sodium/Embeddium replace the terrain shaders). Reconstructs world positions from
 * the depth buffer and applies the same per-light falloff as the injected GLSL, so the trees
 * cast real colored light on terrain and entities regardless of the chunk renderer.
 *
 * <p>When Iris/Oculus has a shaderpack ACTIVE the pack owns lighting and framebuffers, so this
 * pass steps aside (detected via the Iris API through reflection). Without a pack, Iris runs the
 * Sodium pipeline and this pass applies normally.</p>
 */
@Environment(EnvType.CLIENT)
public final class WisteriaLightShaderEffect extends NichirinPostProcessor {

    public static final WisteriaLightShaderEffect INSTANCE = new WisteriaLightShaderEffect();
    private static final int SHADER_LIGHTS = 16;

    private Matrix4f frameFrustum = new Matrix4f();
    private Matrix4f frameProjection = new Matrix4f();
    private int lastWidth = -1;
    private int lastHeight = -1;

    // Iris API lookup, resolved once via reflection so there is no hard dependency.
    private Object irisApi;
    private Method irisPackInUse;
    private boolean irisChecked;

    private WisteriaLightShaderEffect() {
    }

    @Override
    public ResourceLocation getShaderEffectId() {
        return ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "wisteria_light");
    }

    /** Called at the tail of the level render with the frame's real view/projection matrices. */
    public void renderFrame(Matrix4f frustumMatrix, Matrix4f projectionMatrix) {
        if (NichirinShaderInjection.injectionEnabled()) return; // vanilla path lights geometry itself
        if (!WisteriaLightData.hasLights()) return;
        if (isIrisShaderPackInUse()) return;

        this.frameFrustum = new Matrix4f(frustumMatrix);
        this.frameProjection = new Matrix4f(projectionMatrix);
        setActive(true);
        process(new PoseStack());
        setActive(false);
    }

    @Override
    protected void beforeProcess(PoseStack viewModelStack) {
        if (effects == null || effects.length == 0) return;

        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getScreenWidth();
        int height = mc.getWindow().getScreenHeight();
        if (width != lastWidth || height != lastHeight) {
            lastWidth = width;
            lastHeight = height;
            if (shaderEffect != null) shaderEffect.resize(width, height);
        }

        Matrix4f invViewProj = new Matrix4f(frameProjection).mul(frameFrustum).invert();
        Vector3f camPos = mc.gameRenderer.getMainCamera().getPosition().toVector3f();
        int count = Math.min(SHADER_LIGHTS, WisteriaLightData.getLightCount());

        for (EffectInstance effect : effects) {
            Uniform inv = effect.getUniform("InvViewProj");
            if (inv != null) inv.set(invViewProj);
            Uniform cam = effect.getUniform("CamPos");
            if (cam != null) cam.set(camPos);
            Uniform color = effect.getUniform("LightColor");
            if (color != null) {
                color.set(WisteriaLightData.getRed(), WisteriaLightData.getGreen(),
                        WisteriaLightData.getBlue(), WisteriaLightData.getStrength());
            }
            for (int i = 0; i < SHADER_LIGHTS; i++) {
                Uniform light = effect.getUniform("Light" + i);
                if (light == null) continue;
                if (i < count) {
                    light.set(WisteriaLightData.getLightX(i), WisteriaLightData.getLightY(i),
                            WisteriaLightData.getLightZ(i), WisteriaLightData.getLightRadius(i));
                } else {
                    light.set(0.0f, 0.0f, 0.0f, 0.0f);
                }
            }
        }
    }

    private boolean isIrisShaderPackInUse() {
        if (!irisChecked) {
            irisChecked = true;
            try {
                Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                irisApi = apiClass.getMethod("getInstance").invoke(null);
                irisPackInUse = apiClass.getMethod("isShaderPackInUse");
            } catch (Throwable ignored) {
                // Iris not installed — nothing to defer to.
            }
        }
        if (irisPackInUse == null) return false;
        try {
            return (Boolean) irisPackInUse.invoke(irisApi);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
