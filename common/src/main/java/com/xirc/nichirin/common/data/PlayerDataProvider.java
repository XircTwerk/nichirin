package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.network.s2c.ProgressionSyncPacket;
import com.xirc.nichirin.common.network.util.MovesetSyncPacket;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provides and manages player data including movesets, progression, and statistics
 * Uses Architectury events for cross-platform compatibility
 */
public class PlayerDataProvider {

    private static final Map<UUID, PlayerData> PLAYER_DATA = new HashMap<>();

    /**
     * Gets or creates player data for a player
     */
    public static PlayerData getData(Player player) {
        PlayerData data = PLAYER_DATA.computeIfAbsent(player.getUUID(), k -> new PlayerData());
        // Set player reference for modifiers and statistics
        data.getMovesetData().setPlayer(player);
        return data;
    }

    /**
     * Gets moveset data for a player
     */
    public static MovesetData getMovesetData(Player player) {
        PlayerData data = getData(player);
        // Ensure player reference is set
        data.getMovesetData().setPlayer(player);
        return data.getMovesetData();
    }

    /**
     * Legacy getter for backwards compatibility
     * @deprecated Use getMovesetData() instead
     */
    @Deprecated
    public static MovesetData getBreathingStyleData(Player player) {
        return getMovesetData(player);
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
            data.getMovesetData().setPlayer(serverPlayer);

            // Re-apply all modifiers after loading
            var moveset = data.getMovesetData().getMoveset();
            if (moveset != null) {
                // Only apply modifiers for breathing techniques
                if (moveset.isBreathingMoveset()) {
                    moveset.applyAllModifiers(serverPlayer);
                }
                // Record moveset as equipped for time tracking
                data.getStatistics().onMovesetEquipped(moveset.getMovesetId());
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
                data.getMovesetData().cleanup();

                // Save data before removing from memory
                savePlayerData(serverPlayer);
            }

            // Clean up memory AFTER saving
            PLAYER_DATA.remove(serverPlayer.getUUID());
        });

        // Handle player respawn
        PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd) -> {
            // Only process server players
            if (newPlayer.level().isClientSide()) return;

            ServerPlayer serverPlayer = (ServerPlayer) newPlayer;

            // IMPORTANT: Don't reload from disk, data should persist in memory
            // Just ensure player reference is correct and reapply modifiers
            PlayerData data = getData(serverPlayer);
            data.getMovesetData().setPlayer(serverPlayer);

            var moveset = data.getMovesetData().getMoveset();
            if (moveset != null) {
                // Only apply modifiers for breathing techniques
                if (moveset.isBreathingMoveset()) {
                    moveset.applyAllModifiers(serverPlayer);
                }
                data.getStatistics().onMovesetEquipped(moveset.getMovesetId());
            }

            // Sync to client
            syncToClient(serverPlayer);
        });

        // Handle player clone (dimension change)
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wasDeath) -> {
            // Only process server players
            if (newPlayer.level().isClientSide()) return;

            ServerPlayer serverPlayer = (ServerPlayer) newPlayer;

            PlayerData oldData = PLAYER_DATA.get(oldPlayer.getUUID());

            if (oldData != null) {
                // Update time tracking and cleanup old player's modifiers
                oldData.getStatistics().updateTimeTracking();
                oldData.getMovesetData().cleanup();

                // Create new data for new player and copy from old
                PlayerData newData = new PlayerData();
                newData.copyFrom(oldData);

                // Set the new player reference
                newData.getMovesetData().setPlayer(serverPlayer);

                // Store the new data
                PLAYER_DATA.put(serverPlayer.getUUID(), newData);

                // Apply modifiers to new player
                var moveset = newData.getMovesetData().getMoveset();
                if (moveset != null) {
                    // Only apply modifiers for breathing techniques
                    if (moveset.isBreathingMoveset()) {
                        moveset.applyAllModifiers(serverPlayer);
                    }
                    newData.getStatistics().onMovesetEquipped(moveset.getMovesetId());
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
                        // Update time tracking for equipped movesets
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
            data.getMovesetData().cleanup();
        }
        PLAYER_DATA.remove(player.getUUID());
    }

    public static void clearAll() {
        // Cleanup all modifiers and update time tracking
        for (PlayerData data : PLAYER_DATA.values()) {
            data.getStatistics().updateTimeTracking();
            data.getMovesetData().cleanup();
        }
        PLAYER_DATA.clear();
    }

    /**
     * Syncs moveset data to client
     */
    private static void syncToClient(ServerPlayer player) {
        try {
            PlayerData data = getData(player);
            String movesetId = data.getMovesetData().getMovesetId();

            // Send moveset sync (renamed from BreathingStyleSyncPacket)
            MovesetSyncPacket.sendToPlayer(player, movesetId);

            ProgressionSyncPacket.sendToPlayer(player);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates player moveset data and syncs to client
     */
    public static void updateAndSync(ServerPlayer player, String movesetId) {
        try {
            PlayerData data = getData(player);
            String previousMoveset = data.getMovesetData().getMovesetId();

            data.getMovesetData().setMovesetId(movesetId);
            savePlayerData(player);
            syncToClient(player);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Records technique usage for statistics
     */
    public static void recordTechniqueUsage(Player player, String movesetId) {
        try {
            getData(player).getStatistics().recordTechniqueUsage(movesetId);
        } catch (Exception e) {
            System.err.println("Failed to record technique usage: " + e.getMessage());
        }
    }

    /**
     * Records damage dealt for statistics
     */
    public static void recordDamageDealt(Player player, String movesetId, int damage) {
        try {
            getData(player).getStatistics().recordDamageDealt(movesetId, damage);
        } catch (Exception e) {
            System.err.println("Failed to record damage dealt: " + e.getMessage());
        }
    }

    /**
     * Updates combo tracking
     */
    public static void updateComboChain(Player player, String movesetId, int comboLength) {
        try {
            getData(player).getStatistics().updateComboChain(movesetId, comboLength);
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
            data.getMovesetData().cleanup();
        }
        PLAYER_DATA.clear();
    }

    /**
     * Forces a sync for all online players (useful for debugging)
     */
    public static void forceSync(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncToClient(player);
        }
    }
}