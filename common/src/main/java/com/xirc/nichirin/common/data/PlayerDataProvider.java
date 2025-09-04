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
 * Provides and manages player data including breathing styles and progression
 * Uses Architectury events for cross-platform compatibility
 */
public class PlayerDataProvider {

    private static final Map<UUID, PlayerData> PLAYER_DATA = new HashMap<>();
    private static final String PERSISTENT_TAG_KEY = "NichirinPlayerData";

    /**
     * Gets or creates player data for a player
     */
    public static PlayerData getData(Player player) {
        PlayerData data = PLAYER_DATA.computeIfAbsent(player.getUUID(), k -> new PlayerData());
        // Set player reference for speed modifiers
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

                // Set player reference and apply speed modifiers
                PlayerData data = getData(serverPlayer);
                data.getBreathingStyleData().setPlayer(serverPlayer);

                // Re-apply speed modifier after loading (in case it was lost)
                var moveset = data.getBreathingStyleData().getMoveset();
                if (moveset != null) {
                    moveset.applySpeedModifier(serverPlayer);
                    System.out.println("DEBUG: Re-applied speed modifier on player join");
                }

                // Sync to client
                syncToClient(serverPlayer);
            }
        });

        // Handle player quit - save data and cleanup
        PlayerEvent.PLAYER_QUIT.register(player -> {
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;

                // Cleanup speed modifiers before saving
                PlayerData data = PLAYER_DATA.get(player.getUUID());
                if (data != null) {
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

                // Re-apply speed modifiers after respawn
                PlayerData data = getData(serverPlayer);
                var moveset = data.getBreathingStyleData().getMoveset();
                if (moveset != null) {
                    moveset.applySpeedModifier(serverPlayer);
                    System.out.println("DEBUG: Re-applied speed modifier on player respawn");
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

                // Cleanup old player's speed modifiers
                oldData.getBreathingStyleData().cleanup();

                newData.copyFrom(oldData);

                // Apply speed modifiers to new player
                var moveset = newData.getBreathingStyleData().getMoveset();
                if (moveset != null) {
                    moveset.applySpeedModifier(newPlayer);
                    System.out.println("DEBUG: Applied speed modifier on player clone");
                }

                // Save to persistent data
                if (newPlayer instanceof ServerPlayer) {
                    ServerPlayer serverPlayer = (ServerPlayer) newPlayer;
                    savePlayerData(serverPlayer);
                }
            }
        });

        // Save data periodically for safety using server tick
        TickEvent.SERVER_POST.register((server) -> {
            if (server.getTickCount() % 1200 == 0) { // Every minute
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
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
        // Cleanup speed modifiers before clearing
        PlayerData data = PLAYER_DATA.get(player.getUUID());
        if (data != null) {
            data.getBreathingStyleData().cleanup();
        }
        PLAYER_DATA.remove(player.getUUID());
    }

    public static void clearAll() {
        // Cleanup all speed modifiers
        for (PlayerData data : PLAYER_DATA.values()) {
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
     * Clears all cached data (for mod reload/testing)
     */
    public static void clearCache() {
        // Cleanup all speed modifiers
        for (PlayerData data : PLAYER_DATA.values()) {
            data.getBreathingStyleData().cleanup();
        }
        PLAYER_DATA.clear();
    }
}