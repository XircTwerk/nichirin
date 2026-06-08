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
        // Dormant — kept for the dormant 3D-mesh aura code. AuraConfig was slimmed down so
        // the pixelize/blockSize/posterize fields are no longer present here.
        return String.format(
                "overlay=%s color=(%.2f,%.2f,%.2f,%.2f)",
                overlayEnabled, debugR, debugG, debugB, debugA);
    }
}
