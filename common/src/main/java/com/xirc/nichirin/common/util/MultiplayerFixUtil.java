package com.xirc.nichirin.common.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SIMPLE FIX: Just track basic multiplayer state that's needed
 * No overengineering, just fix the core issues
 */
public class MultiplayerFixUtil {

    // Track attack wheel states server-side (fixes input blocking)
    private static final Map<UUID, Boolean> attackWheelStates = new ConcurrentHashMap<>();

    /**
     * Update attack wheel state (called from client and server)
     */
    public static void setAttackWheelOpen(Player player, boolean isOpen) {
        if (player.level().isClientSide) {
            // Client side - could send packet here if needed later
            return;
        }

        // Server side - just track the state
        attackWheelStates.put(player.getUUID(), isOpen);
        System.out.println("DEBUG: MultiplayerFix - " + player.getName().getString() +
                " wheel: " + (isOpen ? "OPEN" : "CLOSED"));
    }

    /**
     * Check if attack wheel is open (SERVER SIDE ONLY)
     */
    public static boolean isAttackWheelOpen(Player player) {
        if (player.level().isClientSide) {
            return false; // Client should use its own state
        }

        return attackWheelStates.getOrDefault(player.getUUID(), false);
    }

    /**
     * Clean up when player leaves
     */
    public static void cleanupPlayer(Player player) {
        attackWheelStates.remove(player.getUUID());
    }
}