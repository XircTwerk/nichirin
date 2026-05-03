package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.resources.ResourceLocation;

/**
 * Screen-space post-processing aura for Flame Breathing.
 * Renders a fiery, heat-distorting rim around the screen edges.
 * Intensity (0.0–1.0) is supplied by BreathingAuraShaderHandler and is
 * strongest when the player's breath is most depleted.
 */
public class FlameBreathingAuraShader extends NichirinPostProcessor {

    private float breathIntensity = 0f;
    private float smoothedIntensity = 0f;

    private static final float SMOOTH_SPEED = 0.06f;

    @Override
    public ResourceLocation getShaderEffectId() {
        return new ResourceLocation("nichirin", "flame_breathing_aura");
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

            // Pulse quickens as intensity rises
            Uniform pulseSpeed = effect.getUniform("PulseSpeed");
            if (pulseSpeed != null) pulseSpeed.set(0.8f + smoothedIntensity * 1.4f);

            // Shift from deep orange toward a brighter yellow-orange at peak
            Uniform color = effect.getUniform("AuraColor");
            if (color != null) color.set(1.0f, 0.28f + smoothedIntensity * 0.12f, 0.0f);

            // Rim expands as intensity rises
            Uniform innerRadius = effect.getUniform("InnerRadius");
            if (innerRadius != null) innerRadius.set(0.55f + smoothedIntensity * 0.15f);
        }
    }
}