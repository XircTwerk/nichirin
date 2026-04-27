package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.resources.ResourceLocation;

/**
 * Screen-space aura shader for Water Breathing.
 * Renders a rippling, flowing blue-teal rim around the screen edges.
 * Intensity (0.0 - 1.0) is driven by the player's current breath percentage.
 */
public class WaterBreathingAuraShader extends NichirinPostProcessor {

    /** Current breath percentage [0, 1]. Set every client tick by BreathingAuraHandler. */
    private float breathIntensity = 0f;

    /** Smoothed intensity value to avoid jarring jumps. */
    private float smoothedIntensity = 0f;

    private static final float SMOOTH_SPEED = 0.05f;

    @Override
    public ResourceLocation getShaderEffectId() {
        return new ResourceLocation("nichirin", "water_breathing_aura");
    }

    /**
     * Called every client tick by BreathingAuraHandler.
     * @param breathPercentage value from BreathingManager.getBreathingPercentage(), range [0, 1]
     */
    public void setBreathIntensity(float breathPercentage) {
        this.breathIntensity = Math.max(0f, Math.min(1f, breathPercentage));
    }

    @Override
    protected void beforeProcess(PoseStack viewModelStack) {
        if (effects == null) return;

        // Water aura smooths more gently - like a flowing current
        float delta = breathIntensity - smoothedIntensity;
        smoothedIntensity += delta * SMOOTH_SPEED * 20f;

        smoothedIntensity = Math.max(0f, Math.min(1f, smoothedIntensity));

        for (var effect : effects) {
            // Overall intensity
            Uniform intensity = effect.getUniform("Intensity");
            if (intensity != null) intensity.set(smoothedIntensity);

            // Wave frequency - more breath = more waves
            Uniform waveFreq = effect.getUniform("WaveFrequency");
            if (waveFreq != null) waveFreq.set(3.0f + smoothedIntensity * 5.0f);

            // Wave amplitude - more breath = bigger distortion
            Uniform waveAmp = effect.getUniform("WaveAmplitude");
            if (waveAmp != null) waveAmp.set(0.0015f + smoothedIntensity * 0.003f);

            // Color: cool blue shifting slightly greener at full intensity
            Uniform color = effect.getUniform("AuraColor");
            if (color != null) color.set(0.08f, 0.45f + smoothedIntensity * 0.1f, 1.0f);

            // Inner radius
            Uniform innerRadius = effect.getUniform("InnerRadius");
            if (innerRadius != null) innerRadius.set(0.52f + smoothedIntensity * 0.18f);
        }
    }
}