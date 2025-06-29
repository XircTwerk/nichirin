package com.xirc.nichirin.common.network;

import com.xirc.nichirin.common.system.slayerabilities.PlayerDoubleJump;
import com.xirc.nichirin.common.util.StaminaManager;
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
        System.out.println("On ground: " + player.onGround());

        // Debug stamina state BEFORE attempting double jump
        float currentStamina = StaminaManager.getStamina(player);
        float maxStamina = StaminaManager.getMaxStamina(player);
        float staminaCost = PlayerDoubleJump.getStaminaCost();

        System.out.println("=== STAMINA STATE BEFORE DOUBLE JUMP ===");
        System.out.println("Current stamina: " + currentStamina);
        System.out.println("Max stamina: " + maxStamina);
        System.out.println("Required stamina: " + staminaCost);
        System.out.println("Has enough stamina: " + StaminaManager.hasStamina(player, staminaCost));

        // Let tryDoubleJump handle ALL validation and execution
        PlayerDoubleJump.tryDoubleJump(player);

        // Debug stamina state AFTER attempting double jump
        float newStamina = StaminaManager.getStamina(player);
        System.out.println("=== STAMINA STATE AFTER DOUBLE JUMP ===");
        System.out.println("New stamina: " + newStamina);
        System.out.println("Stamina consumed: " + (currentStamina - newStamina));

        // Force sync to ensure client gets the update
        if (currentStamina != newStamina) {
            StaminaManager.forceSyncToClient(player);
            System.out.println("SERVER: Force synced stamina to client");
        }
    }
}