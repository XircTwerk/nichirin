package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Screen-space blur + mist-tint effect applied when the local player has the Blurry mob effect.
 * Intensity smoothly fades in/out for a non-jarring transition.
 */
public class MistBlurShaderEffect extends NichirinPostProcessor {

    private float targetIntensity = 0f;
    private float smoothedIntensity = 0f;

    // Exponential smooth rate (units: 1/second).
    // Half-life ≈ ln(2) / rate.  2.0 → ~0.35s half-life (full transition in ~1s).
    private static final float FADE_IN_RATE  = 2.0f;
    private static final float FADE_OUT_RATE = 1.5f; // slightly slower fade-out feels nicer

    @Override
    public ResourceLocation getShaderEffectId() {
        return ResourceLocation.fromNamespaceAndPath("nichirin", "mist_blur");
    }

    /** Called by MistBlurShaderHandler each tick. */
    public void setTargetIntensity(float intensity) {
        this.targetIntensity = Math.max(0f, Math.min(1f, intensity));
    }

    @Override
    protected void beforeProcess(PoseStack viewModelStack) {
        if (effects == null) return;

        // Framerate-independent exponential smoothing.
        // dt is in seconds; multiply by appropriate rate depending on direction.
        float dt = Minecraft.getInstance().getTimer().getRealtimeDeltaTicks() / 20.0f;
        float rate = (targetIntensity > smoothedIntensity) ? FADE_IN_RATE : FADE_OUT_RATE;
        smoothedIntensity += (targetIntensity - smoothedIntensity) * rate * dt;
        smoothedIntensity = Math.max(0f, Math.min(1f, smoothedIntensity));

        // Deactivate cleanly once fully faded out
        if (smoothedIntensity < 0.002f && targetIntensity == 0f) {
            setActive(false);
            smoothedIntensity = 0f;
            return;
        }

        for (var effect : effects) {
            Uniform intensity = effect.getUniform("Intensity");
            if (intensity != null) intensity.set(smoothedIntensity);

            // Mist color: cool blue-grey
            Uniform color = effect.getUniform("MistColor");
            if (color != null) color.set(0.72f, 0.85f, 0.95f);
        }
    }

    public float getSmoothedIntensity() {
        return smoothedIntensity;
    }
}