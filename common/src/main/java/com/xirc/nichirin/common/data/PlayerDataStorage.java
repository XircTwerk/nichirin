package com.xirc.nichirin.common.data;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.system.sheathing.SheathingManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import net.minecraft.server.MinecraftServer;

/**
 * Handles persistent storage of player data including breathing styles and progression
 * Cleaned up unused methods
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
            if (!dataDir.exists() && !dataDir.mkdirs()) {
                BreathOfNichirin.LOGGER.error("Failed to create data directory: {}", dataDir.getPath());
                return;
            }

            File playerFile = new File(dataDir, player.getUUID().toString() + FILE_SUFFIX);
            CompoundTag tag = new CompoundTag();

            // Get the complete player data
            PlayerData data = PlayerDataProvider.getData(player);

            // Save all data using the PlayerData save method
            CompoundTag playerDataTag = data.save();
            tag.put("PlayerData", playerDataTag);
            tag.put("SheathingData", SheathingManager.savePlayerData(player));

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

                if (tag.contains("SheathingData")) {
                    SheathingManager.loadPlayerData(player, tag.getCompound("SheathingData"));
                }
            }

        } catch (IOException e) {
            BreathOfNichirin.LOGGER.error("Failed to load player data for {}", player.getName().getString(), e);
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
