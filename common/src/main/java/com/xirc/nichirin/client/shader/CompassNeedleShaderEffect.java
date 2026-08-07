package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.resources.ResourceLocation;

/** Subtle six-second fighting-spirit perception grade used only by Compass Needle's owner. */
public final class CompassNeedleShaderEffect extends NichirinPostProcessor {
    private float durationSeconds = 6.0f;
    private float elapsed;

    @Override
    public ResourceLocation getShaderEffectId() {
        return ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "compass_needle");
    }

    @Override
    protected void beforeProcess(PoseStack viewModelStack) {
        if (effects == null || effects.length == 0) return;
        elapsed += MC.getTimer().getRealtimeDeltaTicks() / 20.0f;
        float fadeIn = Math.min(1.0f, elapsed / 0.22f);
        float fadeOut = Math.min(1.0f, (durationSeconds - elapsed) / 0.45f);
        setUniform("CompassTime", elapsed);
        setUniform("Intensity", Math.max(0.0f, Math.min(fadeIn, fadeOut)));
        if (elapsed >= durationSeconds) setActive(false);
    }

    public void trigger() {
        trigger(6.0f);
    }

    public void trigger(float durationSeconds) {
        elapsed = 0.0f;
        this.durationSeconds = Math.max(0.5f, durationSeconds);
        setActive(true);
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        if (!active) elapsed = 0.0f;
    }

    private void setUniform(String name, float value) {
        Uniform uniform = effects[0].getUniform(name);
        if (uniform != null) uniform.set(value);
    }
}
