package com.xirc.nichirin.common.network.s2c;

/**
 * S2C marker. Payload: UUID hostEntityId, UUID instanceId.
 * Client handler routes to EntityAuraTracker.removeAura.
 */
public final class RemoveAuraPacket {
    private RemoveAuraPacket() {}
}
