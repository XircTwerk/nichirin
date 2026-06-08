package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;

/**
 * Screen-space block quantization + posterization + low-amplitude fractal noise overlay.
 *
 * DORMANT — not currently registered in BreathOfNichirinClient nor wired into any tick or
 * render hook. The class and its shader assets are kept here so a future iteration of the
 * 3D-mesh aura can re-enable the screen-space pixelization pass without rewriting it. To
 * re-activate: register via NichirinShaderManager and drive setActive(true|false) from a
 * config flag each tick.
 *
 * Hard-coded uniform values below are placeholders; if you wire this back up you'll want
 * a config holder again to drive BlockSize / PosterizeLevels.
 */
public class AuraPixelizationShader extends NichirinPostProcessor {

    private float blockSize = 4.0f;
    private float posterizeLevels = 6.0f;

    @Override
    public ResourceLocation getShaderEffectId() {
        return ResourceLocation.fromNamespaceAndPath("nichirin", "aura_pixelize");
    }

    public void setBlockSize(float v) { this.blockSize = v; }
    public void setPosterizeLevels(float v) { this.posterizeLevels = v; }

    @Override
    protected void beforeProcess(PoseStack viewModelStack) {
        if (effects == null) return;

        for (var effect : effects) {
            Uniform b = effect.getUniform("BlockSize");
            if (b != null) b.set(blockSize);

            Uniform p = effect.getUniform("PosterizeLevels");
            if (p != null) p.set(posterizeLevels);

            Uniform t = effect.getUniform("Time");
            if (t != null) t.set((float) (System.currentTimeMillis() % 100000L) / 1000.0f);
        }
    }
}
