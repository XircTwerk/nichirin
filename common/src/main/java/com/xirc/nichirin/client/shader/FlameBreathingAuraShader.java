package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.resources.ResourceLocation;

/**
 * Screen-space aura shader for Flame Breathing.
 * Renders a fiery, heat-distortion rim around the screen edges.
 * Intensity (0.0 - 1.0) is driven by the player's current breath percentage.
 */
public class FlameBreathingAuraShader extends NichirinPostProcessor {

    /** Current breath percentage [0, 1]. Set every client tick by BreathingAuraHandler. */
    private float breathIntensity = 0f;

    /** Smoothed intensity value to avoid jarring jumps. */
    private float smoothedIntensity = 0f;

    private static final float SMOOTH_SPEED = 0.06f;

    @Override
    public ResourceLocation getShaderEffectId() {
        return new ResourceLocation("nichirin", "flame_breathing_aura");
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

        // Smooth the intensity so it eases in/out rather than snapping
        float delta = breathIntensity - smoothedIntensity;
        smoothedIntensity += delta * SMOOTH_SPEED * 20f; // scale by 20 since time is in seconds

        // Clamp
        smoothedIntensity = Math.max(0f, Math.min(1f, smoothedIntensity));

        for (var effect : effects) {
            // How strong the aura is overall
            Uniform intensity = effect.getUniform("Intensity");
            if (intensity != null) intensity.set(smoothedIntensity);

            // Pulse speed scales with intensity - more breath = faster pulse
            Uniform pulseSpeed = effect.getUniform("PulseSpeed");
            if (pulseSpeed != null) pulseSpeed.set(0.8f + smoothedIntensity * 1.4f);

            // Color: deep orange-red for flame
            Uniform color = effect.getUniform("AuraColor");
            if (color != null) color.set(1.0f, 0.28f + smoothedIntensity * 0.12f, 0.0f);

            // Inner glow radius shrinks when intensity is low
            Uniform innerRadius = effect.getUniform("InnerRadius");
            if (innerRadius != null) innerRadius.set(0.55f + smoothedIntensity * 0.15f);
        }
    }
}