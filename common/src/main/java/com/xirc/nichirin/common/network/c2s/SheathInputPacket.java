package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.system.sheathing.SheathInputAction;
import com.xirc.nichirin.common.system.sheathing.SheathingManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class SheathInputPacket {
    private final SheathInputAction action;
    private final boolean shiftDown;
    private final int heldTicks;

    public SheathInputPacket(SheathInputAction action, boolean shiftDown) {
        this(action, shiftDown, 0);
    }

    public SheathInputPacket(SheathInputAction action, boolean shiftDown, int heldTicks) {
        this.action = action;
        this.shiftDown = shiftDown;
        this.heldTicks = heldTicks;
    }

    public SheathInputPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(SheathInputAction.class);
        this.shiftDown = buf.readBoolean();
        this.heldTicks = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeBoolean(shiftDown);
        buf.writeInt(heldTicks);
    }

    public void handle(ServerPlayer player) {
        SheathingManager.handleInput(player, action, shiftDown, heldTicks);
    }
}