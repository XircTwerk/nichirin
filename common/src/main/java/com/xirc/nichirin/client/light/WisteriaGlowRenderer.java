package com.xirc.nichirin.client.light;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xirc.nichirin.client.aura.AuraRenderTypes;
import com.xirc.nichirin.client.shader.NichirinShaderInjection;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Geometry fallback for the wisteria colored lighting when the vanilla core-shader injection is
 * unavailable (Sodium replaces the terrain pipeline; Iris/Oculus replace the shaders entirely).
 * Draws a soft camera-facing radial-gradient halo at each tracked wisteria light so the trees
 * still visibly emit their purple glow on those pipelines. On vanilla the shader injection tints
 * actual surfaces, so this renderer stays off there to avoid doubling the effect.
 */
@Environment(EnvType.CLIENT)
public final class WisteriaGlowRenderer {

    private static final ResourceLocation WHITE_TEX =
            ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final int SEGMENTS = 24;
    // Halo size relative to the light's (faded) reach, and how strong the disc centre reads.
    private static final float HALO_SCALE = 0.45f;
    private static final float CENTER_ALPHA = 0.30f;

    private WisteriaGlowRenderer() {}

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        // Vanilla pipeline: the core-shader injection already lights real geometry.
        if (NichirinShaderInjection.injectionEnabled()) return;
        int count = WisteriaLightData.getLightCount();
        if (count <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        RenderType glowType = AuraRenderTypes.auraTranslucentNoDepthWrite(WHITE_TEX);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(glowType);

        Vec3 camPos = camera.getPosition();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();
        float r = WisteriaLightData.getRed();
        float g = WisteriaLightData.getGreen();
        float b = WisteriaLightData.getBlue();
        float strength = WisteriaLightData.getStrength();
        int packedLight = LightTexture.FULL_BRIGHT;

        for (int i = 0; i < count; i++) {
            float radius = WisteriaLightData.getLightRadius(i) * HALO_SCALE;
            if (radius <= 0.1f) continue;
            // Fade the halo in with the light's own radius fade, scaled by day/night strength.
            float alpha = CENTER_ALPHA * strength
                    * Math.min(1.0f, WisteriaLightData.getLightRadius(i) / WisteriaLightData.getMaxRadius());
            if (alpha <= 0.01f) continue;

            poseStack.pushPose();
            poseStack.translate(
                    WisteriaLightData.getLightX(i) - camPos.x,
                    WisteriaLightData.getLightY(i) - camPos.y,
                    WisteriaLightData.getLightZ(i) - camPos.z);
            Matrix4f mat = poseStack.last().pose();

            // Camera-facing fan: degenerate quads (center, center, edge_i, edge_i+1) with the
            // centre at full alpha easing to zero at the rim — reads as a soft light halo.
            for (int s = 0; s < SEGMENTS; s++) {
                float a0 = (float) (2.0 * Math.PI * s / SEGMENTS);
                float a1 = (float) (2.0 * Math.PI * (s + 1) / SEGMENTS);
                float x0 = (float) (Math.cos(a0) * radius), y0 = (float) (Math.sin(a0) * radius);
                float x1 = (float) (Math.cos(a1) * radius), y1 = (float) (Math.sin(a1) * radius);

                vert(vc, mat, 0, 0, up, left, r, g, b, alpha, packedLight);
                vert(vc, mat, 0, 0, up, left, r, g, b, alpha, packedLight);
                vert(vc, mat, x0, y0, up, left, r, g, b, 0.0f, packedLight);
                vert(vc, mat, x1, y1, up, left, r, g, b, 0.0f, packedLight);
            }
            poseStack.popPose();
        }
        buffers.endBatch(glowType);
    }

    private static void vert(VertexConsumer vc, Matrix4f mat, float lx, float ly,
                             Vector3f up, Vector3f left,
                             float r, float g, float b, float a, int packedLight) {
        float x = left.x() * lx + up.x() * ly;
        float y = left.y() * lx + up.y() * ly;
        float z = left.z() * lx + up.z() * ly;
        vc.addVertex(mat, x, y, z)
                .setColor(r, g, b, a)
                .setUv(0.5f, 0.5f)
                .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0, 1, 0);
    }
}
