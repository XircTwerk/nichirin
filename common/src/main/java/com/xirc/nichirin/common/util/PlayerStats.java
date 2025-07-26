package com.xirc.nichirin.common.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.stats.Stats;

/**
 * Player statistics utility - accesses client-side stats directly
 */
public class PlayerStats {

    private static boolean statsRequested = false;
    private static long lastStatsRequest = 0;
    private static final long STATS_REQUEST_COOLDOWN = 5000; // 5 seconds

    /**
     * Request stats from server if not already requested recently
     */
    private static void ensureStatsLoaded() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Only request stats if we haven't done so recently
        if (!statsRequested || (currentTime - lastStatsRequest) > STATS_REQUEST_COOLDOWN) {
            try {
                // Request stats from server
                ClientPacketListener connection = mc.getConnection();
                if (connection != null) {
                    connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
                    statsRequested = true;
                    lastStatsRequest = currentTime;
                }
            } catch (Exception e) {
                // If we can't request stats, that's okay
            }
        }
    }

    /**
     * Get mobs killed (client-side)
     */
    public static int getMobsKilled() {
        ensureStatsLoaded();

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getStats() != null) {
                // Use the direct MOB_KILLS stat instead of summing all entities
                return mc.player.getStats().getValue(Stats.CUSTOM.get(Stats.MOB_KILLS));
            }
        } catch (Exception e) {
            // If we can't get stats, return 0
        }
        return 0;
    }

    /**
     * Get deaths (client-side)
     */
    public static int getDeaths() {
        ensureStatsLoaded();

        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.getStats() != null) {
                return mc.player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
            }
        } catch (Exception e) {
            // If we can't get stats, return 0
        }
        return 0;
    }

    /**
     * Check if stats are available and loaded (client-side)
     */
    public static boolean areStatsLoaded() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.player.getStats() == null) {
                return false;
            }

            // Check if we actually have some stats data
            // If both deaths and mob kills are 0, and no stats request has been made,
            // the stats might not be loaded yet
            int deaths = mc.player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
            int mobKills = mc.player.getStats().getValue(Stats.CUSTOM.get(Stats.MOB_KILLS));

            // If we have requested stats and still have 0/0, consider them loaded (just happens to be 0)
            return statsRequested || deaths > 0 || mobKills > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Force request stats from server
     */
    public static void requestStatsFromServer() {
        statsRequested = false; // Reset flag to force request
        ensureStatsLoaded();
    }

    /**
     * Initialize and request initial stats
     */
    public static void initialize() {
        statsRequested = false;
        lastStatsRequest = 0;
        ensureStatsLoaded();
    }
}