package com.xirc.nichirin.common.network;

import com.xirc.nichirin.common.system.slayerabilities.PlayerDoubleJump;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Packet sent from client to server to request a double jump
 */
public class DoubleJumpPacket {

    public DoubleJumpPacket() {
        // Empty constructor for packet
    }

    public DoubleJumpPacket(FriendlyByteBuf buf) {
        // No data to read
    }

    public void toBytes(FriendlyByteBuf buf) {
        // No data to write
    }

    /**
     * Handle the packet on the server
     */
    public void handle(ServerPlayer player) {
        System.out.println("=== SERVER RECEIVED DOUBLE JUMP REQUEST ===");
        System.out.println("Player: " + player.getName().getString());

        // Just perform the double jump - let PlayerDoubleJump handle all validation and stamina consumption
        PlayerDoubleJump.tryDoubleJump(player);
    }
}