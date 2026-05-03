package com.xirc.nichirin.client.shader;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Impact Shake + Flash post-process effect.
 *
 * Sequence (all values in seconds):
 *   0.000 – 0.050  Hard white-out flash (instant)
 *   0.050 – 0.175  Bloom hold: center bright, edges clear first
 *   0.175 – 0.350  Fast fade to clear
 *   0.070+         Screen shake kicks in (delayed past flash), decays fast
 */
public class ImpactShakeShaderEffect extends NichirinPostProcessor {

    // Singleton
    private static final ImpactShakeShaderEffect INSTANCE = new ImpactShakeShaderEffect();
    public static ImpactShakeShaderEffect getInstance() { return INSTANCE; }
    public ImpactShakeShaderEffect() {}

    // Timing (seconds)
    /** Total duration of the effect in seconds. */
    private static final float DURATION        = 0.60f;
    /** How long the flash + bloom phase lasts (Progress 0 → 0.35). */
    private static final float FLASH_DURATION  = 0.35f;
    /** Delay before shake starts, so flash reads first. */
    private static final float SHAKE_DELAY     = 0.07f;
    /** Fade-in ticks for Intensity. */
    private static final int   FADE_IN_TICKS   = 1;   // near-instant
    /** Fade-out ticks for Intensity. */
    private static final int   FADE_OUT_TICKS  = 6;

    // State
    private float elapsedSeconds  = 0f;
    private float intensity       = 0f;
    /** Scales the shake amplitude. Pass >1.0 for harder hits. */
    private float shakeMagnitude  = 1.0f;

    // Shake internals
    /** Max shake offset in pixels at magnitude 1.0. */
    private static final float MAX_SHAKE_PX    = 10f;
    /** Decay rate for exponential shake envelope. Higher = faster decay. */
    private static final float SHAKE_DECAY     = 13f;
    /** Max chromatic split in UV units. */
    private static final float MAX_CHROMA_SPLIT = 0.006f;

    @Override
    public ResourceLocation getShaderEffectId() {
        return new ResourceLocation(BreathOfNichirin.MOD_ID, "impact_shake");
    }

    @Override
    protected void beforeProcess(PoseStack viewModelStack) {
        if (effects == null || effects.length == 0) return;

        Minecraft mc = Minecraft.getInstance();
        float frameDelta = mc.getDeltaFrameTime() / 20.0f; // seconds per frame
        elapsedSeconds += frameDelta;

        // Progress uniform (0..1)
        float progress = Math.min(elapsedSeconds / DURATION, 1.0f);
        setUniform("Progress", progress);

        // Intensity (fade in/out)
        int elapsedTicks = (int)(elapsedSeconds * 20f);
        int totalTicks   = (int)(DURATION * 20f);
        if (elapsedTicks <= FADE_IN_TICKS) {
            intensity = (float) elapsedTicks / FADE_IN_TICKS;
        } else if (elapsedTicks > totalTicks - FADE_OUT_TICKS) {
            intensity = (float)(totalTicks - elapsedTicks) / FADE_OUT_TICKS;
        } else {
            intensity = 1.0f;
        }
        intensity = Math.max(0f, Math.min(1f, intensity));
        setUniform("Intensity", intensity);

        // Shake (delayed, exponential decay)
        float shakeT   = Math.max(0f, elapsedSeconds - SHAKE_DELAY);
        float shakeAmp = shakeMagnitude * MAX_SHAKE_PX * (float)Math.exp(-shakeT * SHAKE_DECAY);

        // Pseudo-random offsets using frame-bucketed sin hash
        int   frameBucket = (int)(elapsedSeconds * 60f);
        float rx = (float)(Math.sin(frameBucket * 127.1 + 311.7) * 43758.5453 % 1.0);
        float ry = (float)(Math.sin((frameBucket + 100) * 127.1 + 311.7) * 43758.5453 % 1.0);
        float shakeX = (rx * 2f - 1f) * shakeAmp;
        float shakeY = (ry * 2f - 1f) * shakeAmp * 0.55f;

        // Convert from pixels to UV space
        int screenW = mc.getWindow().getScreenWidth();
        int screenH = mc.getWindow().getScreenHeight();
        setUniform("ShakeX", screenW > 0 ? shakeX / screenW : 0f);
        setUniform("ShakeY", screenH > 0 ? shakeY / screenH : 0f);

        // Chromatic split (proportional to shake amp)
        float normalizedAmp = Math.min(shakeAmp / MAX_SHAKE_PX, 1.0f);
        setUniform("ChromaSplit", normalizedAmp * MAX_CHROMA_SPLIT);

        // Done?
        if (elapsedSeconds >= DURATION) {
            setActive(false);
        }
    }

    // Public API

    /**
     * Fire the effect with default magnitude (1.0).
     * Safe to call from client-side attack handlers.
     */
    public void trigger() {
        trigger(1.0f);
    }

    /**
     * Fire the effect with a custom shake magnitude.
     * @param magnitude  1.0 = normal punch. 1.5 = Eight-Layered Demon Core.
     *                   0.5 = light M1 hit. Values above 2.0 not recommended.
     */
    public void trigger(float magnitude) {
        elapsedSeconds   = 0f;
        intensity        = 0f;
        shakeMagnitude   = Math.max(0f, magnitude);
        setActive(true);
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(active);
        if (!active) {
            elapsedSeconds = 0f;
            intensity      = 0f;
        }
    }

    private void setUniform(String name, float value) {
        Uniform u = effects[0].getUniform(name);
        if (u != null) u.set(value);
    }
}
