package com.xirc.nichirin.common.network.c2s;

import com.xirc.nichirin.common.system.movement.MovementContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Movement input packet - sent when MB5 is pressed
 */
public class MovementInputPacket {

    public MovementInputPacket() {
        // Empty constructor for client->server packet
    }

    public MovementInputPacket(FriendlyByteBuf buf) {
        // No data to read - this is just a trigger packet
    }

    public void toBytes(FriendlyByteBuf buf) {
        // No data to write - this is just a trigger packet
    }

    public void handle(ServerPlayer player) {
        // Handle on server side
        if (player != null) {
            MovementContext.handleMovementInput(player);
        }
    }
}