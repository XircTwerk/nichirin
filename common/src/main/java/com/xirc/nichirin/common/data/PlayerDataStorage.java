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
 * Handles persistent storage of player breathing style data
 */
public class PlayerDataStorage {

    private static final String DATA_FOLDER = "nichirin_player_data";
    private static final String FILE_SUFFIX = ".dat";

    /**
     * Saves player breathing style data to disk
     */
    public static void savePlayerData(ServerPlayer player) {
        try {
            File dataDir = getDataDirectory(player.server);
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }

            File playerFile = new File(dataDir, player.getUUID().toString() + FILE_SUFFIX);
            CompoundTag tag = new CompoundTag();

            // Get the breathing style data
            BreathingStyleData data = PlayerDataProvider.getData(player);
            tag.put("BreathingStyle", data.save());

            // Write to file
            NbtIo.writeCompressed(tag, playerFile);

        } catch (IOException e) {
            BreathOfNichirin.LOGGER.error("Failed to save player data for {}", player.getName().getString(), e);
        }
    }

    /**
     * Loads player breathing style data from disk
     */
    public static void loadPlayerData(ServerPlayer player) {
        try {
            File dataDir = getDataDirectory(player.server);
            File playerFile = new File(dataDir, player.getUUID().toString() + FILE_SUFFIX);

            if (playerFile.exists()) {
                CompoundTag tag = NbtIo.readCompressed(playerFile);

                if (tag.contains("BreathingStyle")) {
                    BreathingStyleData data = PlayerDataProvider.getData(player);
                    data.load(tag.getCompound("BreathingStyle"));
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