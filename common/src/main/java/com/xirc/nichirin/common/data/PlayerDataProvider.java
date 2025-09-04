package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.network.BreathingStyleSyncPacket;
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
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                // Load data from custom storage
                PlayerDataStorage.loadPlayerData(serverPlayer);

                // Set player reference and apply modifiers
                PlayerData data = getData(serverPlayer);
                data.getBreathingStyleData().setPlayer(serverPlayer);

                // Re-apply all modifiers after loading
                var moveset = data.getBreathingStyleData().getMoveset();
                if (moveset != null) {
                    moveset.applyAllModifiers(serverPlayer);
                    // Record style as equipped for time tracking
                    data.getStatistics().onStyleEquipped(moveset.getMovesetId());
                    System.out.println("DEBUG: Re-applied all modifiers on player join");
                }

                // Sync to client
                syncToClient(serverPlayer);
            }
        });

        // Handle player quit - save data and cleanup
        PlayerEvent.PLAYER_QUIT.register(player -> {
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;

                // Update time tracking and cleanup modifiers before saving
                PlayerData data = PLAYER_DATA.get(player.getUUID());
                if (data != null) {
                    data.getStatistics().updateTimeTracking();
                    data.getBreathingStyleData().cleanup();
                }

                savePlayerData(serverPlayer);
                // Clean up memory
                PLAYER_DATA.remove(player.getUUID());
            }
        });

        // Handle player respawn
        PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd) -> {
            if (newPlayer instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) newPlayer;
                // Data should persist through respawn automatically

                // Re-apply all modifiers after respawn
                PlayerData data = getData(serverPlayer);
                var moveset = data.getBreathingStyleData().getMoveset();
                if (moveset != null) {
                    moveset.applyAllModifiers(serverPlayer);
                    // Re-record style as equipped
                    data.getStatistics().onStyleEquipped(moveset.getMovesetId());
                    System.out.println("DEBUG: Re-applied all modifiers on player respawn");
                }

                syncToClient(serverPlayer);
            }
        });

        // Handle player clone (dimension change)
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wasDeath) -> {
            if (wasDeath) {
                // Copy data from old player to new player
                PlayerData oldData = getData(oldPlayer);
                PlayerData newData = getData(newPlayer);

                // Update time tracking and cleanup old player's modifiers
                oldData.getStatistics().updateTimeTracking();
                oldData.getBreathingStyleData().cleanup();

                newData.copyFrom(oldData);

                // Apply modifiers to new player
                var moveset = newData.getBreathingStyleData().getMoveset();
                if (moveset != null) {
                    moveset.applyAllModifiers(newPlayer);
                    newData.getStatistics().onStyleEquipped(moveset.getMovesetId());
                    System.out.println("DEBUG: Applied all modifiers on player clone");
                }

                // Save to persistent data
                if (newPlayer instanceof ServerPlayer) {
                    ServerPlayer serverPlayer = (ServerPlayer) newPlayer;
                    savePlayerData(serverPlayer);
                }
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
                    }
                    savePlayerData(player);
                }
            }
        });
    }

    /**
     * Saves player data to persistent storage
     */
    private static void savePlayerData(ServerPlayer player) {
        // Use custom storage system
        PlayerDataStorage.savePlayerData(player);
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
     * Syncs breathing style data to client
     */
    private static void syncToClient(ServerPlayer player) {
        PlayerData data = getData(player);
        // Send sync packet (only breathing style data for now, progression is server-side)
        BreathingStyleSyncPacket.sendToPlayer(player, data.getBreathingStyleData().getMovesetId());
    }

    /**
     * Updates player breathing style data and syncs to client
     */
    public static void updateAndSync(ServerPlayer player, String movesetId) {
        PlayerData data = getData(player);
        data.getBreathingStyleData().setMovesetId(movesetId);
        savePlayerData(player);
        syncToClient(player);
    }

    /**
     * Records technique usage for statistics
     */
    public static void recordTechniqueUsage(Player player, String styleId) {
        getData(player).getStatistics().recordTechniqueUsage(styleId);
    }

    /**
     * Records damage dealt for statistics
     */
    public static void recordDamageDealt(Player player, String styleId, int damage) {
        getData(player).getStatistics().recordDamageDealt(styleId, damage);
    }

    /**
     * Updates combo tracking
     */
    public static void updateComboChain(Player player, String styleId, int comboLength) {
        getData(player).getStatistics().updateComboChain(styleId, comboLength);
    }

    /**
     * Resets combo chain
     */
    public static void resetComboChain(Player player) {
        getData(player).getStatistics().resetComboChain();
    }

    /**
     * Records successful dodge
     */
    public static void recordSuccessfulDodge(Player player) {
        getData(player).getStatistics().recordSuccessfulDodge();
    }

    /**
     * Records successful block
     */
    public static void recordSuccessfulBlock(Player player) {
        getData(player).getStatistics().recordSuccessfulBlock();
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
    }
}