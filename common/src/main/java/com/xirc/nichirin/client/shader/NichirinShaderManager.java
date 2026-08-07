package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager for all Nichirin post-processing effects.
 *
 * Depth handling:
 *   MC's main render target exposes its depth buffer as a regular GL texture
 *   via RenderTarget.depthBufferId.  We bind that texture as "DiffuseDepthSampler"
 *   on every active processor before running shader passes.
 *
 *   This works because:
 *   - processAll() runs at TAIL of LevelRenderer.renderLevel() — all geometry is
 *     done, sky pixels have depth 1.0, world pixels have depth < 1.0.
 *   - During PostChain processing, RenderSystem.disableDepthTest() is called, so
 *     the depth buffer is not written to.  The depth values we bind are stable.
 *   - No custom mixin or freeze/snapshot needed.
 */
public class NichirinShaderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(NichirinShaderManager.class);
    private static final NichirinShaderManager INSTANCE = new NichirinShaderManager();
    private final List<NichirinPostProcessor> processors = new ArrayList<>();
    private Camera frameCamera;
    private Matrix4f frameFrustumMatrix = new Matrix4f();

    private NichirinShaderManager() {}

    public static NichirinShaderManager getInstance() {
        return INSTANCE;
    }

    public void register(NichirinPostProcessor processor) {
        if (!processors.contains(processor)) {
            processors.add(processor);
        }
    }

    public void setFrameContext(Camera camera, Matrix4f frustumMatrix) {
        this.frameCamera = camera;
        this.frameFrustumMatrix = new Matrix4f(frustumMatrix);
    }

    public Camera getFrameCamera() {
        return frameCamera;
    }

    public Matrix4f getFrameFrustumMatrix() {
        return new Matrix4f(frameFrustumMatrix);
    }

    /**
     * Bind MC's main depth texture to all active processors.
     * Called at the start of processAll(), before any shader pass runs.
     */
    /**
     * Process all active shaders.
     * Call this at the end of rendering (TAIL of LevelRenderer.renderLevel).
     */
    public void processAll(PoseStack viewModelStack) {
        for (NichirinPostProcessor processor : processors) {
            if (!isEnabledScreenShader(processor)) {
                continue;
            }

            try {
                processor.process(viewModelStack);
            } catch (RuntimeException exception) {
                processor.setActive(false);
                LOGGER.error("Disabled failing shader {}", processor.getClass().getSimpleName(), exception);
            }
        }
    }

    private boolean isEnabledScreenShader(NichirinPostProcessor processor) {
        // World-space VFX must not rewrite the finished frame. Sampling the main depth attachment
        // from a post chain is driver-dependent and corrupted the skybox on some render paths.
        return false;
    }

    public void resize(int width, int height) {
        processors.forEach(p -> p.resize(width, height));
    }

    public void reloadAll() {
        processors.forEach(NichirinPostProcessor::loadShader);
    }

    /** Deactivate every screen shader (e.g. on death) so nothing lingers on the screen. */
    public void clearAll() {
        processors.forEach(p -> p.setActive(false));
    }

    @SuppressWarnings("unchecked")
    public <T extends NichirinPostProcessor> T getProcessor(Class<T> clazz) {
        for (NichirinPostProcessor processor : processors) {
            if (clazz.isInstance(processor)) {
                return (T) processor;
            }
        }
        return null;
    }
}
