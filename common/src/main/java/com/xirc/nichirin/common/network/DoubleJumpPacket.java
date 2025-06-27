package com.xirc.nichirin.common.network;

import com.xirc.nichirin.common.system.slayerabilities.PlayerDoubleJump;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class DoubleJumpPacket {

    public DoubleJumpPacket() {
        // Empty constructor needed for packet registration
    }

    public DoubleJumpPacket(FriendlyByteBuf buf) {
        // No data to read for this packet
    }

    public void toBytes(FriendlyByteBuf buf) {
        // No data to write for this packet
    }

    public void handle(ServerPlayer player) {
        System.out.println("=== SERVER RECEIVED DOUBLE JUMP PACKET ===");
        System.out.println("Player: " + player.getName().getString());
        System.out.println("Player on ground: " + player.onGround());

        // Server receives this packet when client wants to double jump
        // Validate on server side to prevent cheating
        if (PlayerDoubleJump.canDoubleJump(player)) {
            System.out.println("SERVER: Processing double jump request");
            PlayerDoubleJump.tryDoubleJump(player);
        } else {
            System.out.println("SERVER: Rejecting double jump request - validation failed");
        }
    }
}