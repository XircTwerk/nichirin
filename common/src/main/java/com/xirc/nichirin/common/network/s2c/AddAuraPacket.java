package com.xirc.nichirin.common.network.s2c;

/**
 * S2C marker. Payload layout (encoded in AuraManager, decoded in NichirinPacketRegistry):
 *   - UUID hostEntityId
 *   - AuraInstance.write(...)  // id, rgba, radius, pulse, distortion, rotation, lifetime
 *
 * Client handler routes to EntityAuraTracker.addAura.
 */
public final class AddAuraPacket {
    private AddAuraPacket() {}
}
