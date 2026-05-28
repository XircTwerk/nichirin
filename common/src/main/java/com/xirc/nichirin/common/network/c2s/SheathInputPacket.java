package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.system.sheathing.SheathInputAction;
import com.xirc.nichirin.common.system.sheathing.SheathingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class SheathInputPacket {
    private final SheathInputAction action;
    private final boolean shiftDown;

    public SheathInputPacket(SheathInputAction action, boolean shiftDown) {
        this.action = action;
        this.shiftDown = shiftDown;
    }

    public SheathInputPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(SheathInputAction.class);
        this.shiftDown = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeBoolean(shiftDown);
    }

    public void handle(ServerPlayer player) {
        SheathingManager.handleInput(player, action, shiftDown);
    }
}
