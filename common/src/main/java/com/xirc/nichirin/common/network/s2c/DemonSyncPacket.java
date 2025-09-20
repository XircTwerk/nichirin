package com.xirc.nichirin.common.network.s2c;

import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;

/**
 * S2C packet to sync demon blood data to client for display
 */
@Getter
public class DemonSyncPacket {

    private final int bloodPoints;
    private final boolean isDemon;

    public DemonSyncPacket(int bloodPoints, boolean isDemon) {
        this.bloodPoints = bloodPoints;
        this.isDemon = isDemon;
    }

    public DemonSyncPacket(FriendlyByteBuf buf) {
        this.bloodPoints = buf.readInt();
        this.isDemon = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(bloodPoints);
        buf.writeBoolean(isDemon);
    }

    public void handleClient() {
        // Update client-side blood display
        com.xirc.nichirin.client.gui.DemonBloodGui.updateBloodPoints(bloodPoints, isDemon);
    }
}