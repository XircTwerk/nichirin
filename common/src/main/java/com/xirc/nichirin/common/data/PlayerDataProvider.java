package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.network.s2c.ProgressionSyncPacket;
import com.xirc.nichirin.common.network.util.BreathingStyleSyncPacket;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provides and manages player data including breathing styles, progression, and statistics
 * Uses Architectury events for cross-platform compatibility
 * FIXED: All instanceof ServerPlayer checks removed to prevent compiler warnings
 */
public class PlayerDataProvider {

    private static final Map<UUID, PlayerData> PLAYER_DATA = new HashMap<>();

    /**
     * Gets or creates player data for a player
     */
    public static PlayerData getData(Player player) {
        PlayerData data = PLAYER_DATA.computeIfAbsent(player.getUUID(), k -> new PlayerData());
        // Set player reference for modifiers and statistics
        data.getBreathingStyleData().setPlayer(player);
        return data;
    }

    /**
     * Gets breathing style data for a player (for backwards compatibility)
     */
    public static BreathingStyleData getBreathingStyleData(Player player) {
        PlayerData data = getData(player);
        // Ensure player reference is set
        data.getBreathingStyleData().setPlayer(player);
        return data.getBreathingStyleData();
    }

    /**
     * Registers event handlers
     */
    public static void register() {
        // Handle player join
        PlayerEvent.PLAYER_JOIN.register(player -> {
            // Only process server players
            if (player.level().isClientSide()) return;

            ServerPlayer serverPlayer = (ServerPlayer) player;

            // Load data from custom storage FIRST
            PlayerDataStorage.loadPlayerData(serverPlayer);

            // Get the loaded data and set player reference
            PlayerData data = getData(serverPlayer);
            data.getBreathingStyleData().setPlayer(serverPlayer);

            // Re-apply all modifiers after loading
            var moveset = data.getBreathingStyleData().getMoveset();
            if (moveset != null) {
                moveset.applyAllModifiers(serverPlayer);
                // Record style as equipped for time tracking
                data.getStatistics().onStyleEquipped(moveset.getMovesetId());
                System.out.println("DEBUG: Re-applied all modifiers on player join for " + serverPlayer.getName().getString());
            }

            // CRITICAL: Sync to client AFTER data is loaded and applied
            syncToClient(serverPlayer);
        });

        // Handle player quit - save data and cleanup
        PlayerEvent.PLAYER_QUIT.register(player -> {
            // Only process server players
            if (player.level().isClientSide()) return;

            ServerPlayer serverPlayer = (ServerPlayer) player;

            // Update time tracking and cleanup modifiers before saving
            PlayerData data = PLAYER_DATA.get(serverPlayer.getUUID());
            if (data != null) {
                data.getStatistics().updateTimeTracking();
                data.getBreathingStyleData().cleanup();

                // Save data before removing from memory
                savePlayerData(serverPlayer);
            }

            // Clean up memory AFTER saving
            PLAYER_DATA.remove(serverPlayer.getUUID());
        });

        // Handle player respawn - FIXED: Proper data handling
        PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd) -> {
            // Only process server players
            if (newPlayer.level().isClientSide()) return;

            ServerPlayer serverPlayer = (ServerPlayer) newPlayer;

            // IMPORTANT: Don't reload from disk, data should persist in memory
            // Just ensure player reference is correct and reapply modifiers
            PlayerData data = getData(serverPlayer);
            data.getBreathingStyleData().setPlayer(serverPlayer);

            var moveset = data.getBreathingStyleData().getMoveset();
            if (moveset != null) {
                moveset.applyAllModifiers(serverPlayer);
                data.getStatistics().onStyleEquipped(moveset.getMovesetId());
                System.out.println("DEBUG: Re-applied all modifiers on player respawn for " + serverPlayer.getName().getString());
            }

            // Sync to client
            syncToClient(serverPlayer);
        });

        // Handle player clone (dimension change) - FIXED: Better data copying
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wasDeath) -> {
            // Only process server players
            if (newPlayer.level().isClientSide()) return;

            ServerPlayer serverPlayer = (ServerPlayer) newPlayer;

            PlayerData oldData = PLAYER_DATA.get(oldPlayer.getUUID());

            if (oldData != null) {
                // Update time tracking and cleanup old player's modifiers
                oldData.getStatistics().updateTimeTracking();
                oldData.getBreathingStyleData().cleanup();

                // Create new data for new player and copy from old
                PlayerData newData = new PlayerData();
                newData.copyFrom(oldData);

                // Set the new player reference
                newData.getBreathingStyleData().setPlayer(serverPlayer);

                // Store the new data
                PLAYER_DATA.put(serverPlayer.getUUID(), newData);

                // Apply modifiers to new player
                var moveset = newData.getBreathingStyleData().getMoveset();
                if (moveset != null) {
                    moveset.applyAllModifiers(serverPlayer);
                    newData.getStatistics().onStyleEquipped(moveset.getMovesetId());
                    System.out.println("DEBUG: Applied all modifiers on player clone for " + serverPlayer.getName().getString());
                }

                // Save to persistent data
                savePlayerData(serverPlayer);

                // Sync to client
                syncToClient(serverPlayer);
            }

            // Clean up old player data if it was a death
            if (wasDeath) {
                PLAYER_DATA.remove(oldPlayer.getUUID());
            }
        });

        // Save data periodically and update time tracking
        TickEvent.SERVER_POST.register((server) -> {
            if (server.getTickCount() % 1200 == 0) { // Every minute
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    PlayerData data = PLAYER_DATA.get(player.getUUID());
                    if (data != null) {
                        // Update time tracking for equipped styles
                        data.getStatistics().updateTimeTracking();
                        // Save periodically
                        savePlayerData(player);
                    }
                }
            }
        });
    }

    /**
     * Saves player data to persistent storage
     */
    private static void savePlayerData(ServerPlayer player) {
        try {
            PlayerDataStorage.savePlayerData(player);
        } catch (Exception e) {
            System.err.println("Failed to save player data for " + player.getName().getString() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void clearData(Player player) {
        // Cleanup modifiers and update time tracking before clearing
        PlayerData data = PLAYER_DATA.get(player.getUUID());
        if (data != null) {
            data.getStatistics().updateTimeTracking();
            data.getBreathingStyleData().cleanup();
        }
        PLAYER_DATA.remove(player.getUUID());
    }

    public static void clearAll() {
        // Cleanup all modifiers and update time tracking
        for (PlayerData data : PLAYER_DATA.values()) {
            data.getStatistics().updateTimeTracking();
            data.getBreathingStyleData().cleanup();
        }
        PLAYER_DATA.clear();
    }

    /**
     * Syncs breathing style data to client - IMPROVED with null checks
     */
    private static void syncToClient(ServerPlayer player) {
        try {
            PlayerData data = getData(player);
            String movesetId = data.getBreathingStyleData().getMovesetId();

            // Send breathing style sync
            BreathingStyleSyncPacket.sendToPlayer(player, movesetId);

            ProgressionSyncPacket.sendToPlayer(player);

            System.out.println("DEBUG: Synced moveset '" + movesetId + "' to client for " + player.getName().getString());
        } catch (Exception e) {
            System.err.println("Failed to sync data to client for " + player.getName().getString() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates player breathing style data and syncs to client - IMPROVED with validation
     */
    public static void updateAndSync(ServerPlayer player, String movesetId) {
        try {
            PlayerData data = getData(player);
            String previousMoveset = data.getBreathingStyleData().getMovesetId();

            data.getBreathingStyleData().setMovesetId(movesetId);
            savePlayerData(player);
            syncToClient(player);

            System.out.println("DEBUG: Updated moveset from '" + previousMoveset + "' to '" + movesetId + "' for " + player.getName().getString());
        } catch (Exception e) {
            System.err.println("Failed to update and sync for " + player.getName().getString() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Records technique usage for statistics
     */
    public static void recordTechniqueUsage(Player player, String styleId) {
        try {
            getData(player).getStatistics().recordTechniqueUsage(styleId);
        } catch (Exception e) {
            System.err.println("Failed to record technique usage: " + e.getMessage());
        }
    }

    /**
     * Records damage dealt for statistics
     */
    public static void recordDamageDealt(Player player, String styleId, int damage) {
        try {
            getData(player).getStatistics().recordDamageDealt(styleId, damage);
        } catch (Exception e) {
            System.err.println("Failed to record damage dealt: " + e.getMessage());
        }
    }

    /**
     * Updates combo tracking
     */
    public static void updateComboChain(Player player, String styleId, int comboLength) {
        try {
            getData(player).getStatistics().updateComboChain(styleId, comboLength);
        } catch (Exception e) {
            System.err.println("Failed to update combo chain: " + e.getMessage());
        }
    }

    /**
     * Resets combo chain
     */
    public static void resetComboChain(Player player) {
        try {
            getData(player).getStatistics().resetComboChain();
        } catch (Exception e) {
            System.err.println("Failed to reset combo chain: " + e.getMessage());
        }
    }

    /**
     * Records successful dodge
     */
    public static void recordSuccessfulDodge(Player player) {
        try {
            getData(player).getStatistics().recordSuccessfulDodge();
        } catch (Exception e) {
            System.err.println("Failed to record successful dodge: " + e.getMessage());
        }
    }

    /**
     * Records successful block
     */
    public static void recordSuccessfulBlock(Player player) {
        try {
            getData(player).getStatistics().recordSuccessfulBlock();
        } catch (Exception e) {
            System.err.println("Failed to record successful block: " + e.getMessage());
        }
    }

    /**
     * Clears all cached data (for mod reload/testing)
     */
    public static void clearCache() {
        // Cleanup all modifiers and update time tracking
        for (PlayerData data : PLAYER_DATA.values()) {
            data.getStatistics().updateTimeTracking();
            data.getBreathingStyleData().cleanup();
        }
        PLAYER_DATA.clear();
        System.out.println("DEBUG: Cleared all player data cache");
    }

    /**
     * Forces a sync for all online players (useful for debugging)
     */
    public static void forceSync(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncToClient(player);
        }
        System.out.println("DEBUG: Forced sync for all online players");
    }
}