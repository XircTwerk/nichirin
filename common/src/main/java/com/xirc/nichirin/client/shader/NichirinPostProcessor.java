package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.shaders.Uniform;
import com.xirc.nichirin.mixin_logic.NichirinDepthHolder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;


/**
 * Base class for post-processing shader effects
 */
public abstract class NichirinPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(NichirinPostProcessor.class);
    protected static final Minecraft MC = Minecraft.getInstance();

    public static int getMainDepthTexId() {
        return ((NichirinDepthHolder) MC.getMainRenderTarget()).nichirin$getDepthTexId();
    }

    protected PostChain shaderEffect;
    protected EffectInstance[] effects;

    private boolean initialized = false;
    private boolean active = false;
    protected double time = 0.0;

    /**
     * @return The shader effect ID (e.g., "nichirin:my_effect" -> "nichirin:shaders/post/my_effect.json")
     */
    public abstract ResourceLocation getShaderEffectId();

    /**
     * Called once when the processor is first initialized
     */
    public void init() {
        loadShader();

        initialized = true;
    }

    /**
     * Load or reload the shader from resources
     */
    public final void loadShader() {
        if (shaderEffect != null) {
            shaderEffect.close();
            shaderEffect = null;
        }

        try {
            ResourceLocation id = getShaderEffectId();
            ResourceLocation file = new ResourceLocation(
                    id.getNamespace(),
                    "shaders/post/" + id.getPath() + ".json"
            );

            LOGGER.debug("Loading shader from: {}", file);

            shaderEffect = new PostChain(
                    MC.getTextureManager(),
                    MC.getResourceManager(),
                    MC.getMainRenderTarget(),
                    file
            );

            shaderEffect.resize(MC.getWindow().getScreenWidth(), MC.getWindow().getScreenHeight());

            // Try to access passes field - first try direct access (if access widener works)
            try {
                effects = new EffectInstance[shaderEffect.passes.size()];
                for (int i = 0; i < shaderEffect.passes.size(); i++) {
                    effects[i] = shaderEffect.passes.get(i).getEffect();
                }
            } catch (IllegalAccessError e) {
                // Fallback to reflection if access widener didn't work
                LOGGER.debug("Access widener not working, using reflection fallback");
                try {
                    var passesField = PostChain.class.getDeclaredField("passes");
                    passesField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    var passesList = (List<PostPass>) passesField.get(shaderEffect);
                    effects = new EffectInstance[passesList.size()];
                    for (int i = 0; i < passesList.size(); i++) {
                        effects[i] = passesList.get(i).getEffect();
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to access PostChain.passes", ex);
                }
            }

            LOGGER.debug("Shader loaded successfully! Effects count: {}", effects.length);

        } catch (Exception ex) {
            LOGGER.error("Failed to load shader: {}", getShaderEffectId(), ex);
        }
    }

    /**
     * Bind a frozen depth texture as the DiffuseDepthSampler for all passes.
     * Called by NichirinShaderManager after it freezes the main render target's depth.
     */
    public final void bindDepthTexture(int texId) {
        if (effects == null || texId == 0) return;
        for (EffectInstance effect : effects) {
            effect.setSampler("DiffuseDepthSampler", () -> texId);
        }
    }

    /**
     * Handle window resize
     */
    public void resize(int width, int height) {
        if (shaderEffect != null) {
            shaderEffect.resize(width, height);
        }
    }

    /**
     * Apply common uniforms (camera position, matrices, time, etc.)
     */
    protected void applyCommonUniforms(PoseStack viewModelStack) {
        if (effects == null) return;

        // Time uniform
        for (EffectInstance effect : effects) {
            Uniform timeUniform = effect.getUniform("Time");
            if (timeUniform != null) {
                timeUniform.set((float) time);
            }

            // Camera position
            Uniform camPosUniform = effect.getUniform("CameraPosition");
            if (camPosUniform != null) {
                Vector3f pos = MC.gameRenderer.getMainCamera().getPosition().toVector3f();
                camPosUniform.set(pos);
            }

            // Inverse view matrix
            Uniform invViewMatUniform = effect.getUniform("InverseViewMatrix");
            if (invViewMatUniform != null) {
                Matrix4f invViewMat = new Matrix4f(viewModelStack.last().pose());
                invViewMat.invert();
                invViewMatUniform.set(invViewMat);
            }

            // Inverse projection matrix
            Uniform invProjMatUniform = effect.getUniform("InverseProjectionMatrix");
            if (invProjMatUniform != null) {
                Matrix4f invProjMat = new Matrix4f(RenderSystem.getProjectionMatrix());
                invProjMat.invert();
                invProjMatUniform.set(invProjMat);
            }

            // FOV
            Uniform fovUniform = effect.getUniform("FOV");
            if (fovUniform != null) {
                // Use public method instead
                float fov = (float) Math.toRadians(MC.options.fov().get());
                fovUniform.set(fov);
            }
        }
    }

    /**
     * Main render method - called every frame
     */
    public final void process(PoseStack viewModelStack) {
        if (!active) return;

        if (!initialized) {
            LOGGER.debug("Initializing shader on first process call");
            init();
        }

        if (shaderEffect == null) {
            LOGGER.error("Shader effect is null, cannot process!");
            return;
        }

        // Bind MC's depth buffer every frame so DiffuseDepthSampler is always current.
        // Must happen after init() since effects[] is populated there.
        bindDepthTexture(getMainDepthTexId());

        time += MC.getDeltaFrameTime() / 20.0;

        applyCommonUniforms(viewModelStack);
        beforeProcess(viewModelStack);

        if (!active) return;

        // Match the GL state vanilla uses for its own post effects.
        // Depth testing and blending left over from world rendering will
        // either kill the fullscreen quad (depth fail) or blend it against
        // black â€” both produce a black screen.
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.resetTextureMatrix();

        shaderEffect.process(MC.getFrameTime());

        // Restore the main render target as the active draw FBO and reset
        // the GL viewport, then re-enable depth testing for any subsequent
        // world rendering that may still need it.
        MC.getMainRenderTarget().bindWrite(true);
        RenderSystem.enableDepthTest();

        afterProcess();
    }

    /**
     * Override to set custom uniforms before rendering
     */
    protected void beforeProcess(PoseStack viewModelStack) {}

    /**
     * Override to clean up after rendering
     */
    protected void afterProcess() {}

    /**
     * Activate this effect
     */
    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            time = 0.0;
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean isInitialized() {
        return initialized;
    }
}
