package com.xirc.nichirin.common.network.s2c;

import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C packet to sync demon blood data to client for display
 * Updated to work with mixin-based rendering system
 */
@Getter
public class DemonSyncPacket {

    private final int bloodPoints;
    private final int halfBloodPoints;
    private final boolean isDemon;

    public DemonSyncPacket(int bloodPoints, int halfBloodPoints, boolean isDemon) {
        this.bloodPoints = bloodPoints;
        this.halfBloodPoints = halfBloodPoints;
        this.isDemon = isDemon;
    }

    public DemonSyncPacket(FriendlyByteBuf buf) {
        this.bloodPoints = buf.readInt();
        this.halfBloodPoints = buf.readInt();
        this.isDemon = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(bloodPoints);
        buf.writeInt(halfBloodPoints);
        buf.writeBoolean(isDemon);
    }

    public void handleClient() {
        // Update client-side blood display using DemonComponent
        // This works with the new mixin-based rendering
        com.xirc.nichirin.common.system.DemonComponent.updateBloodFromSync(bloodPoints, halfBloodPoints, isDemon);
    }
}