package com.xirc.nichirin.common.system;

import com.xirc.nichirin.common.network.s2c.StanceSyncPacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stance system for defensive actions - separate from stamina
 * Used for blocking, parrying, and other defensive techniques
 */
public class StanceManager {

    private static final Map<UUID, StanceData> playerStance = new HashMap<>();

    // Stance configuration (different from stamina)
    private static final float DEFAULT_MAX_STANCE = 100f; // Lower than stamina
    private static final float DEFAULT_REGEN_RATE = 0.8f; // Slower than stamina (was 1.2`f)
    private static final int DEFAULT_REGEN_DELAY = 80;
    private static final float MIN_REGEN_THRESHOLD = 0.1f;

    /**
     * Updates stance for a player (call this every tick on SERVER)
     */
    public static void tick(Player player) {
        if (player == null || player.level().isClientSide) return;

        StanceData data = getOrCreateData(player);

        // Always increment time since use
        data.timeSinceUse++;

        // Slower regeneration than stamina
        if (data.timeSinceUse >= data.regenDelay && data.current < data.max) {
            float regenAmount = data.regenRate;

            // Slow down regen as we approach max
            float missingStance = data.max - data.current;
            if (missingStance < 8f) { // Different threshold than stamina
                regenAmount *= (missingStance / 8f);
            }

            data.current = Math.min(data.max, data.current + regenAmount);

            // Stop micro-regeneration near max
            if (data.max - data.current < MIN_REGEN_THRESHOLD) {
                data.current = data.max;
            }

            // Sync to client every few ticks during regen
            if (data.timeSinceUse % 6 == 0) { // Less frequent than stamina
                syncToClient(player, data);
            }
        }
    }

    /**
     * Consumes stance for defensive actions
     * @return true if successful, false if insufficient stance
     */
    public static boolean consume(Player player, float amount) {
        if (player == null) return false;

        StanceData data = getOrCreateData(player);
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
     * Checks if player has enough stance
     */
    public static boolean hasStance(Player player, float amount) {
        if (player == null) return false;
        StanceData data = getOrCreateData(player);
        return data.current >= amount;
    }

    /**
     * Gets current stance
     */
    public static float getStance(Player player) {
        if (player == null) return 0;
        return getOrCreateData(player).current;
    }

    /**
     * Gets max stance
     */
    public static float getMaxStance(Player player) {
        if (player == null) return DEFAULT_MAX_STANCE;
        return getOrCreateData(player).max;
    }

    /**
     * Restores stance instantly
     */
    public static void restore(Player player, float amount) {
        if (player == null) return;

        StanceData data = getOrCreateData(player);
        data.current = Math.min(data.max, data.current + amount);
        syncToClient(player, data);
    }

    /**
     * Fully restores stance
     */
    public static void restoreFull(Player player) {
        if (player == null) return;

        StanceData data = getOrCreateData(player);
        data.current = data.max;
        data.timeSinceUse = data.regenDelay;
        syncToClient(player, data);
    }

    /**
     * Sets max stance and adjusts current if needed
     */
    public static void setMaxStance(Player player, float max) {
        if (player == null) return;

        StanceData data = getOrCreateData(player);
        data.max = Math.max(1, max);
        data.current = Math.min(data.current, data.max);
        syncToClient(player, data);
    }

    /**
     * Set regeneration rate
     */
    public static void setRegenRate(Player player, float regenRate) {
        if (player == null) return;
        StanceData data = getOrCreateData(player);
        data.regenRate = Math.max(0.1f, regenRate);
    }

    /**
     * Set regeneration delay
     */
    public static void setRegenDelay(Player player, int delayTicks) {
        if (player == null) return;
        StanceData data = getOrCreateData(player);
        data.regenDelay = Math.max(0, delayTicks);
    }

    /**
     * Gets stance percentage (0.0 to 1.0)
     */
    public static float getStancePercentage(Player player) {
        if (player == null) return 0f;
        StanceData data = getOrCreateData(player);
        return data.current / data.max;
    }

    /**
     * Force sync stance to client
     */
    public static void forceSyncToClient(Player player) {
        if (player == null) return;
        StanceData data = getOrCreateData(player);
        syncToClient(player, data);
    }

    /**
     * Saves stance data to NBT
     */
    public static void save(Player player, CompoundTag tag) {
        if (player == null) return;
        StanceData data = playerStance.get(player.getUUID());
        if (data != null) {
            CompoundTag stanceTag = new CompoundTag();
            stanceTag.putFloat("current", data.current);
            stanceTag.putFloat("max", data.max);
            stanceTag.putFloat("regenRate", data.regenRate);
            stanceTag.putInt("regenDelay", data.regenDelay);
            stanceTag.putInt("timeSinceUse", data.timeSinceUse);
            tag.put("StanceData", stanceTag);
        }
    }

    /**
     * Loads stance data from NBT
     */
    public static void load(Player player, CompoundTag tag) {
        if (player == null || !tag.contains("StanceData")) return;

        CompoundTag stanceTag = tag.getCompound("StanceData");
        StanceData data = new StanceData(
                stanceTag.getFloat("max"),
                stanceTag.getFloat("regenRate"),
                stanceTag.getInt("regenDelay")
        );
        data.current = stanceTag.getFloat("current");
        data.timeSinceUse = stanceTag.getInt("timeSinceUse");

        playerStance.put(player.getUUID(), data);
        syncToClient(player, data);
    }

    /**
     * Cleans up data for disconnected players
     */
    public static void cleanupPlayer(Player player) {
        if (player != null) {
            playerStance.remove(player.getUUID());
        }
    }

    /**
     * Syncs stance data to client using packet system
     */
    private static void syncToClient(Player player, StanceData data) {
        if (player instanceof ServerPlayer serverPlayer) {
            StanceSyncPacket packet = new StanceSyncPacket(player.getId(), data.current, data.max);
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }
    }

    private static StanceData getOrCreateData(Player player) {
        return playerStance.computeIfAbsent(player.getUUID(),
                uuid -> new StanceData(DEFAULT_MAX_STANCE, DEFAULT_REGEN_RATE, DEFAULT_REGEN_DELAY));
    }

    private static class StanceData {
        float current;
        float max;
        float regenRate;
        int regenDelay;
        int timeSinceUse;

        StanceData(float max, float regenRate, int regenDelay) {
            this.current = max;
            this.max = max;
            this.regenRate = regenRate;
            this.regenDelay = regenDelay;
            this.timeSinceUse = regenDelay;
        }
    }
}