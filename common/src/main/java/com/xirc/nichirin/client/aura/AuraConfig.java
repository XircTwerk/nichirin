package com.xirc.nichirin.client.aura;

/**
 * Runtime-tunable aura visuals shared across all auras unless overridden by an AuraInstance.
 * Pure data holder, server-safe. Layered wisp tuning lives here rather than in render code.
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

    public static int wispCount = 12;
    public static int outerWispCount = 4;
    public static int middleWispCount = 5;
    public static int innerWispCount = 3;
    public static int verticalSegmentCount = 10;
    public static float minimumHeight = 0.46f;
    public static float maximumHeight = 2.68f;
    public static float minimumWidth = 0.14f;
    public static float maximumWidth = 0.38f;
    public static float minimumRadius = 0.30f;
    public static float maximumRadius = 0.92f;
    public static float swayAmount = 0.13f;
    public static float swaySpeed = 1.1f;
    public static float upwardMovementSpeed = 0.72f;
    public static float overallRotationSpeed = 0.05f;
    public static int fragmentCount = 9;
    public static float fragmentSpawnRate = 0.34f;
    public static float fragmentMinimumLifetime = 0.55f;
    public static float fragmentMaximumLifetime = 1.35f;
    public static float fragmentMinimumSize = 0.045f;
    public static float fragmentMaximumSize = 0.11f;
    public static float maximumRenderDistance = 64.0f;
    public static float auraScale = 0.74f;
    public static float roundedEnvelopeVerticalScale = 1.44f;
    public static float pixelSize = 1.0f / 16.0f;
    public static float playerPixelSize = 1.0f / 8.0f;

    private AuraConfig() {}
}
