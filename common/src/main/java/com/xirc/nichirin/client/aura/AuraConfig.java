package com.xirc.nichirin.client.aura;

/**
 * Runtime-tunable aura visuals shared across all auras unless overridden by an AuraInstance.
 * Pure data holder, server-safe.
 *
 * The aura system is 2D-only — the disc renders as a flat camera-billboarded pixelated bulb
 * at the entity centre. There is no longer a 3D mesh renderer.
 */
public final class AuraConfig {
    public static float opacityMultiplier  = 1.35f;
    public static float brightness         = 1.15f;
    public static float pulseAmplitude     = 0.08f;
    public static float waveAnimAmplitude  = 0.8f;
    public static float waveBase           = 0.6f;
    public static float lobeCountLow       = 4.0f;
    public static float lobeCountHigh      = 7.0f;
    public static float animationSpeed     = 1.0f;
    public static int   pixelize2dGridSize = 32;

    private AuraConfig() {}
}
