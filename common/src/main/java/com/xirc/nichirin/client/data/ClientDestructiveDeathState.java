package com.xirc.nichirin.client.data;

/**
 * Client-side mirror of the server's per-player Destructive Death flags. Read by HUD/UI to show
 * which Destructive Death toggles are active.
 *
 * <p>Plain POJO state — no client-only class references — so it lives in the {@code client/data}
 * package by convention but can be safely loaded on the dedicated server too.</p>
 */
public final class ClientDestructiveDeathState {

    public static boolean shockwaveEnabled;
    public static boolean overdriveEnabled;
    public static boolean compassActive;
    public static boolean compassOverdrive;
    public static long compassExpiryTick;

    private ClientDestructiveDeathState() {}

    public static void update(boolean shockwave, boolean overdrive,
                              boolean compass, boolean compassOd, long expiry) {
        shockwaveEnabled = shockwave;
        overdriveEnabled = overdrive;
        compassActive = compass;
        compassOverdrive = compassOd;
        compassExpiryTick = expiry;
    }
}
