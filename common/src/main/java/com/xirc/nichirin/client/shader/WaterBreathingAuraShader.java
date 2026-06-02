package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.resources.ResourceLocation;

/**
 * Screen-space post-processing aura for Water Breathing.
 * Renders a rippling, flowing blue-teal rim around the screen edges.
 * Intensity (0.0–1.0) is supplied by BreathingAuraShaderHandler and is
 * strongest when the player's breath is most depleted.
 */
public class WaterBreathingAuraShader extends NichirinPostProcessor {

    private float breathIntensity = 0f;
    private float smoothedIntensity = 0f;

    private static final float SMOOTH_SPEED = 0.05f;

    @Override
    public ResourceLocation getShaderEffectId() {
        return ResourceLocation.fromNamespaceAndPath("nichirin", "water_breathing_aura");
    }

    public void setBreathIntensity(float intensity) {
        this.breathIntensity = Math.max(0f, Math.min(1f, intensity));
    }

    @Override
    protected void beforeProcess(PoseStack viewModelStack) {
        if (effects == null) return;

        float delta = breathIntensity - smoothedIntensity;
        smoothedIntensity += delta * SMOOTH_SPEED * 20f;
        smoothedIntensity = Math.max(0f, Math.min(1f, smoothedIntensity));

        for (var effect : effects) {
            Uniform intensity = effect.getUniform("Intensity");
            if (intensity != null) intensity.set(smoothedIntensity);

            // More waves and stronger distortion as intensity rises
            Uniform waveFreq = effect.getUniform("WaveFrequency");
            if (waveFreq != null) waveFreq.set(3.0f + smoothedIntensity * 5.0f);

            Uniform waveAmp = effect.getUniform("WaveAmplitude");
            if (waveAmp != null) waveAmp.set(0.0015f + smoothedIntensity * 0.003f);

            // Shift from deep blue toward cyan-teal at peak intensity
            Uniform color = effect.getUniform("AuraColor");
            if (color != null) color.set(0.08f, 0.45f + smoothedIntensity * 0.1f, 1.0f);

            Uniform innerRadius = effect.getUniform("InnerRadius");
            if (innerRadius != null) innerRadius.set(0.52f + smoothedIntensity * 0.18f);
        }
    }
}