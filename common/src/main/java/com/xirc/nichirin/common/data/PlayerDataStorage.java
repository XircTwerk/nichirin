package com.xirc.nichirin.common.data;

import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Handles persistent storage of player data including breathing styles and progression
 */
public class PlayerDataStorage {

    private static final String DATA_FOLDER = "nichirin_player_data";
    private static final String FILE_SUFFIX = ".dat";

    /**
     * Saves player data to disk
     */
    public static void savePlayerData(ServerPlayer player) {
        try {
            File dataDir = getDataDirectory(player.server);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            File playerFile = new File(dataDir, player.getUUID().toString() + FILE_SUFFIX);
            CompoundTag tag = new CompoundTag();

            // Get the complete player data
            PlayerData data = PlayerDataProvider.getData(player);

            // Save all data using the PlayerData save method
            CompoundTag playerDataTag = data.save();
            tag.put("PlayerData", playerDataTag);

            // Also save legacy breathing style data for backwards compatibility
            tag.put("BreathingStyle", data.getBreathingStyleData().save());

            // Write to file
            NbtIo.writeCompressed(tag, playerFile);

        } catch (IOException e) {
            BreathOfNichirin.LOGGER.error("Failed to save player data for {}", player.getName().getString(), e);
        }
    }

    /**
     * Loads player data from disk
     */
    public static void loadPlayerData(ServerPlayer player) {
        try {
            File dataDir = getDataDirectory(player.server);
            File playerFile = new File(dataDir, player.getUUID().toString() + FILE_SUFFIX);

            if (playerFile.exists()) {
                CompoundTag tag = NbtIo.readCompressed(playerFile);
                PlayerData data = PlayerDataProvider.getData(player);

                // Try to load new format first
                if (tag.contains("PlayerData")) {
                    data.load(tag.getCompound("PlayerData"));
                }
                // Fall back to legacy format for backwards compatibility
                else if (tag.contains("BreathingStyle")) {
                    data.getBreathingStyleData().load(tag.getCompound("BreathingStyle"));
                    // Initialize progression with default values
                    // (progression data will be empty for existing players)
                }
            }

        } catch (IOException e) {
            BreathOfNichirin.LOGGER.error("Failed to load player data for {}", player.getName().getString(), e);
        }
    }

    /**
     * Deletes player data file (for cleanup)
     */
    public static void deletePlayerData(MinecraftServer server, UUID playerId) {
        try {
            File dataDir = getDataDirectory(server);
            File playerFile = new File(dataDir, playerId.toString() + FILE_SUFFIX);

            if (playerFile.exists()) {
                playerFile.delete();
            }

        } catch (Exception e) {
            BreathOfNichirin.LOGGER.error("Failed to delete player data for {}", playerId, e);
        }
    }

    /**
     * Gets the data directory for storing player files
     */
    private static File getDataDirectory(MinecraftServer server) {
        // Get world save directory
        File worldDir = server.getWorldPath(LevelResource.ROOT).toFile();
        return new File(worldDir, DATA_FOLDER);
    }
}