package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.system.movement.MovementContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Movement input packet - sent when MB5 is pressed
 */
public class MovementInputPacket {

    public MovementInputPacket() {}

    public MovementInputPacket(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(ServerPlayer player) {
        if (player != null) {
            MovementContext.handleMovementInput(player);
        }
    }
}