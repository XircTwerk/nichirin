package com.xirc.nichirin.common.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global system to block ALL katana inputs when attack wheel is open
 * Works across client and server to prevent any input leakage
 */
public class GlobalInputBlocker {

    // Track blocked players globally - works on both client and server
    private static final Map<UUID, Long> blockedPlayers = new ConcurrentHashMap<>();
    private static final int BLOCK_DURATION_TICKS = 10; // Extended block duration

    /**
     * Block all inputs for a player (called when wheel opens)
     */
    public static void blockPlayer(UUID playerId) {
        blockedPlayers.put(playerId, System.currentTimeMillis());
        System.out.println("DEBUG: GlobalInputBlocker - Player " + playerId + " blocked");
    }

    /**
     * Unblock a player after delay (called when wheel closes)
     */
    public static void unblockPlayerDelayed(UUID playerId) {
        // Keep blocking for a short period to prevent race conditions
        blockedPlayers.put(playerId, System.currentTimeMillis() + (BLOCK_DURATION_TICKS * 50)); // 50ms per tick
        System.out.println("DEBUG: GlobalInputBlocker - Player " + playerId + " unblocked with delay");
    }

    /**
     * Check if a player's inputs should be blocked
     */
    public static boolean isPlayerBlocked(UUID playerId) {
        Long blockTime = blockedPlayers.get(playerId);
        if (blockTime == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - blockTime < (BLOCK_DURATION_TICKS * 50)) {
            return true; // Still blocked
        }

        // Block expired, remove it
        blockedPlayers.remove(playerId);
        return false;
    }

    /**
     * Force unblock a player immediately
     */
    public static void forceUnblockPlayer(UUID playerId) {
        blockedPlayers.remove(playerId);
        System.out.println("DEBUG: GlobalInputBlocker - Player " + playerId + " force unblocked");
    }

    /**
     * Cleanup old entries periodically
     */
    public static void cleanup() {
        long currentTime = System.currentTimeMillis();
        blockedPlayers.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > (BLOCK_DURATION_TICKS * 100)); // Double the timeout for cleanup
    }
}