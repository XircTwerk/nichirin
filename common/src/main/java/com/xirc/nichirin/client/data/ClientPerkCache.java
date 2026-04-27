package com.xirc.nichirin.client.data;

import com.xirc.nichirin.common.system.perks.PerkData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Client-side cache of the local player's {@link PerkData}, populated by
 * {@link com.xirc.nichirin.common.network.s2c.PerkSyncPacket}.
 *
 * <p>Never write to this directly — always go through {@code PerkActionPacket}
 * which round-trips through the server and triggers a fresh sync.</p>
 */
@Environment(EnvType.CLIENT)
public final class ClientPerkCache {

    private static final PerkData DATA = new PerkData();

    /** Replaces the cached data with a fresh copy received from the server. */
    public static void update(PerkData incoming) {
        DATA.copyFrom(incoming);
    }

    /** Read-only view of the cached perk state. */
    public static PerkData get() {
        return DATA;
    }

    private ClientPerkCache() {}
}
