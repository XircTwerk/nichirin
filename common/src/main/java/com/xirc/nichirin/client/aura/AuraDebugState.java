package com.xirc.nichirin.client.aura;

/** Client-local debug toggles for the aura system. Pure data holder, server-safe. */
public final class AuraDebugState {
    public static boolean overlayEnabled = false;
    public static float debugR = 0.2f, debugG = 0.7f, debugB = 1.0f, debugA = 0.35f;

    private AuraDebugState() {}

    public static void resetColor() {
        debugR = 0.2f; debugG = 0.7f; debugB = 1.0f; debugA = 0.35f;
    }

    public static String summary() {
        return String.format(
                "overlay=%s color=(%.2f,%.2f,%.2f,%.2f) pixelize=%s blockSize=%.1f posterize=%d",
                overlayEnabled, debugR, debugG, debugB, debugA,
                AuraConfig.pixelizationEnabled, AuraConfig.pixelBlockSize, AuraConfig.posterizationLevels);
    }
}
