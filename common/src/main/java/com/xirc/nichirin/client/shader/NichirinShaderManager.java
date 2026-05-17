package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.vertex.PoseStack;
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
    private static final NichirinShaderManager INSTANCE = new NichirinShaderManager();
    private final List<NichirinPostProcessor> processors = new ArrayList<>();

    private NichirinShaderManager() {}

    public static NichirinShaderManager getInstance() {
        return INSTANCE;
    }

    public void register(NichirinPostProcessor processor) {
        if (!processors.contains(processor)) {
            processors.add(processor);
        }
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
        // Shaders temporarily disabled
    }

    public void resize(int width, int height) {
        processors.forEach(p -> p.resize(width, height));
    }

    public void reloadAll() {
        processors.forEach(NichirinPostProcessor::loadShader);
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
