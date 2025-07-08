package com.xirc.nichirin.common.data;

import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Provides and manages breathing style data for players
 * Uses Architectury events for cross-platform compatibility
 */
public class PlayerDataProvider {

    private static final Map<UUID, BreathingStyleData> PLAYER_DATA = new HashMap<>();
    private static final String PERSISTENT_TAG_KEY = "NichirinBreathingStyle";

    /**
     * Gets or creates breathing style data for a player
     */
    public static BreathingStyleData getData(Player player) {
        return PLAYER_DATA.computeIfAbsent(player.getUUID(), k -> new BreathingStyleData());
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

                // Sync to client
                syncToClient(serverPlayer);
            }
        });

        // Handle player quit - save data
        PlayerEvent.PLAYER_QUIT.register(player -> {
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
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
                syncToClient(serverPlayer);
            }
        });

        // Handle player clone (dimension change)
        PlayerEvent.PLAYER_CLONE.register((oldPlayer, newPlayer, wasDeath) -> {
            if (wasDeath) {
                // Copy data from old player to new player
                BreathingStyleData oldData = getData(oldPlayer);
                BreathingStyleData newData = getData(newPlayer);
                newData.copyFrom(oldData);

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
        PLAYER_DATA.remove(player.getUUID());
    }

    public static void clearAll() {
        PLAYER_DATA.clear();
    }

    /**
     * Syncs breathing style data to client
     */
    private static void syncToClient(ServerPlayer player) {
        BreathingStyleData data = getData(player);
        // Send sync packet
        BreathingStyleSyncPacket.sendToPlayer(player, data.getMovesetId());
    }

    /**
     * Updates player data and syncs to client
     */
    public static void updateAndSync(ServerPlayer player, String movesetId) {
        BreathingStyleData data = getData(player);
        data.setMovesetId(movesetId);
        savePlayerData(player);
        syncToClient(player);
    }

    /**
     * Clears all cached data (for mod reload/testing)
     */
    public static void clearCache() {
        PLAYER_DATA.clear();
    }
}