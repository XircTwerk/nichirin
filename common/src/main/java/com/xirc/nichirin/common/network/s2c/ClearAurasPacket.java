package com.xirc.nichirin.common.network.s2c;

/**
 * S2C marker. Payload: UUID hostEntityId.
 * Client handler routes to EntityAuraTracker.clearAuras.
 */
public final class ClearAurasPacket {
    private ClearAurasPacket() {}
}
