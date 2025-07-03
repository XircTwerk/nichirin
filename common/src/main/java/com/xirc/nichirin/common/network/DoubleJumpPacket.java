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

        // Debug stamina state BEFORE attempting double jump
        float currentStamina = StaminaManager.getStamina(player);
        float maxStamina = StaminaManager.getMaxStamina(player);
        float staminaCost = PlayerDoubleJump.getStaminaCost();

        // Let tryDoubleJump handle ALL validation and execution
        PlayerDoubleJump.tryDoubleJump(player);

        // Debug stamina state AFTER attempting double jump
        float newStamina = StaminaManager.getStamina(player);

        // Force sync to ensure client gets the update
        if (currentStamina != newStamina) {
            StaminaManager.forceSyncToClient(player);
        }
    }
}