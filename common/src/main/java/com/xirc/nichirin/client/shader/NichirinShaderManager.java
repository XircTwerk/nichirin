package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager for all Nichirin post-processing effects
 */
public class NichirinShaderManager {
    private static final NichirinShaderManager INSTANCE = new NichirinShaderManager();
    private final List<NichirinPostProcessor> processors = new ArrayList<>();

    private boolean didCopyDepth = false;

    private NichirinShaderManager() {}

    public static NichirinShaderManager getInstance() {
        return INSTANCE;
    }

    /**
     * Register a post processor to be managed
     * Call this during client initialization
     */
    public void register(NichirinPostProcessor processor) {
        if (!processors.contains(processor)) {
            processors.add(processor);
        }
    }

    /**
     * Copy depth buffer for all active processors
     * Should be called once per frame after world rendering
     */
    public void copyDepthBuffer() {
        if (didCopyDepth) return;

        processors.forEach(NichirinPostProcessor::copyDepthBuffer);
        didCopyDepth = true;
    }

    /**
     * Process all active shaders
     * Call this at the end of rendering
     */
    public void processAll(PoseStack viewModelStack) {
        copyDepthBuffer(); // Ensure depth is copied

        for (NichirinPostProcessor processor : processors) {
            if (processor.isActive()) {
                processor.process(viewModelStack);
            }
        }

        didCopyDepth = false; // Reset for next frame
    }

    /**
     * Handle window resize for all processors
     */
    public void resize(int width, int height) {
        processors.forEach(p -> p.resize(width, height));
    }

    /**
     * Reload all shaders (useful for resource pack changes)
     */
    public void reloadAll() {
        processors.forEach(NichirinPostProcessor::loadShader);
    }

    /**
     * Get a specific processor by class
     */
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