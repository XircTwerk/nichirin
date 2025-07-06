package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.network.BreathingSyncPacket;
import com.xirc.nichirin.common.network.SyncBreathPacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages breathing power for breathing techniques (separate from stamina)
 */
public class BreathingManager {

    private static final Map<UUID, BreathingData> playerBreathing = new HashMap<>();

    // Default values for breathing
    private static final float DEFAULT_MAX_BREATH = 100f;
    private static final float DEFAULT_REGEN_RATE = 0.8f; // Slower than stamina
    private static final int DEFAULT_REGEN_DELAY = 80; // 4 seconds (longer than stamina)
    private static final float MIN_REGEN_THRESHOLD = 0.1f;

    /**
     * Updates breathing for a player (call this every tick on SERVER)
     */
    public static void tick(Player player) {
        if (player == null || player.level().isClientSide) return;

        BreathingData data = getOrCreateData(player);

        // Always increment time since use
        data.timeSinceUse++;

        // Regeneration logic (slower than stamina)
        if (data.timeSinceUse >= data.regenDelay && data.current < data.max) {
            float regenAmount = data.regenRate;

            // Slow down regen as we approach max
            float missingBreath = data.max - data.current;
            if (missingBreath < 10f) {
                regenAmount *= (missingBreath / 10f);
            }

            data.current = Math.min(data.max, data.current + regenAmount);

            // Stop micro-regeneration near max
            if (data.max - data.current < MIN_REGEN_THRESHOLD) {
                data.current = data.max;
            }

            // Sync to client every few ticks during regen
            if (data.timeSinceUse % 5 == 0) {
                syncToClient(player, data);
            }
        }
    }

    /**
     * Consumes breathing power for a technique
     * @return true if successful, false if insufficient breath
     */
    public static boolean consume(Player player, float amount) {
        if (player == null) return false;

        BreathingData data = getOrCreateData(player);
        if (data.current >= amount) {
            data.current = Math.max(0, data.current - amount);
            data.timeSinceUse = 0; // Reset regeneration timer

            // Immediate sync on consumption
            syncToClient(player, data);
            return true;
        }
        return false;
    }

    /**
     * Checks if player has enough breath
     */
    public static boolean hasBreath(Player player, float amount) {
        if (player == null) return false;
        BreathingData data = getOrCreateData(player);
        return data.current >= amount;
    }

    /**
     * Gets current breath
     */
    public static float getBreath(Player player) {
        if (player == null) return 0;
        return getOrCreateData(player).current;
    }

    /**
     * Gets max breath
     */
    public static float getMaxBreath(Player player) {
        if (player == null) return DEFAULT_MAX_BREATH;
        return getOrCreateData(player).max;
    }

    /**
     * Restores breath instantly
     */
    public static void restore(Player player, float amount) {
        if (player == null) return;

        BreathingData data = getOrCreateData(player);
        data.current = Math.min(data.max, data.current + amount);
        syncToClient(player, data);
    }

    /**
     * Fully restores breath
     */
    public static void restoreFull(Player player) {
        if (player == null) return;

        BreathingData data = getOrCreateData(player);
        data.current = data.max;
        data.timeSinceUse = data.regenDelay;
        syncToClient(player, data);
    }

    /**
     * Sets max breath and adjusts current if needed
     */
    public static void setMaxBreath(Player player, float max) {
        if (player == null) return;

        BreathingData data = getOrCreateData(player);
        data.max = Math.max(1, max);
        data.current = Math.min(data.current, data.max);
        syncToClient(player, data);
    }

    /**
     * Set breath regeneration rate
     */
    public static void setRegenRate(Player player, float regenRate) {
        if (player == null) return;
        BreathingData data = getOrCreateData(player);
        data.regenRate = Math.max(0.1f, regenRate);
    }

    /**
     * Set regeneration delay
     */
    public static void setRegenDelay(Player player, int delayTicks) {
        if (player == null) return;
        BreathingData data = getOrCreateData(player);
        data.regenDelay = Math.max(0, delayTicks);
    }

    /**
     * Gets breathing percentage (0.0 to 1.0)
     */
    public static float getBreathingPercentage(Player player) {
        if (player == null) return 0f;
        BreathingData data = getOrCreateData(player);
        return data.current / data.max;
    }

    /**
     * Force sync breathing to client
     */
    public static void forceSyncToClient(Player player) {
        if (player == null) return;
        BreathingData data = getOrCreateData(player);
        syncToClient(player, data);
    }

    /**
     * Saves breathing data to NBT
     */
    public static void save(Player player, CompoundTag tag) {
        if (player == null) return;
        BreathingData data = playerBreathing.get(player.getUUID());
        if (data != null) {
            CompoundTag breathingTag = new CompoundTag();
            breathingTag.putFloat("current", data.current);
            breathingTag.putFloat("max", data.max);
            breathingTag.putFloat("regenRate", data.regenRate);
            breathingTag.putInt("regenDelay", data.regenDelay);
            breathingTag.putInt("timeSinceUse", data.timeSinceUse);
            tag.put("BreathingData", breathingTag);
        }
    }

    /**
     * Loads breathing data from NBT
     */
    public static void load(Player player, CompoundTag tag) {
        if (player == null || !tag.contains("BreathingData")) return;

        CompoundTag breathingTag = tag.getCompound("BreathingData");
        BreathingData data = new BreathingData(
                breathingTag.getFloat("max"),
                breathingTag.getFloat("regenRate"),
                breathingTag.getInt("regenDelay")
        );
        data.current = breathingTag.getFloat("current");
        data.timeSinceUse = breathingTag.getInt("timeSinceUse");

        playerBreathing.put(player.getUUID(), data);
        syncToClient(player, data);
    }

    /**
     * Cleans up data for disconnected players
     */
    public static void cleanupPlayer(Player player) {
        if (player != null) {
            playerBreathing.remove(player.getUUID());
        }
    }

    /**
     * Syncs breathing data to client
     */
    private static void syncToClient(Player player, BreathingData data) {
        if (player instanceof ServerPlayer serverPlayer) {
            // Create and send the breathing sync packet
            SyncBreathPacket packet = new SyncBreathPacket(player.getId(), data.current, data.max);
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }
    }

    private static BreathingData getOrCreateData(Player player) {
        return playerBreathing.computeIfAbsent(player.getUUID(),
                uuid -> new BreathingData(DEFAULT_MAX_BREATH, DEFAULT_REGEN_RATE, DEFAULT_REGEN_DELAY));
    }

    private static class BreathingData {
        float current;
        float max;
        float regenRate;
        int regenDelay;
        int timeSinceUse;

        BreathingData(float max, float regenRate, int regenDelay) {
            this.current = max;
            this.max = max;
            this.regenRate = regenRate;
            this.regenDelay = regenDelay;
            this.timeSinceUse = regenDelay;
        }
    }
}