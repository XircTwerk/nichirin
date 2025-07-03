package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.network.StaminaSyncPacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Enhanced stamina manager with proper regeneration and networking
 */
public class StaminaManager {

    private static final Map<UUID, StaminaData> playerStamina = new HashMap<>();

    // Enhanced default values
    private static final float DEFAULT_MAX_STAMINA = 100f;
    private static final float DEFAULT_REGEN_RATE = 1.2f; // Per tick when regenerating
    private static final int DEFAULT_REGEN_DELAY = 60; // 3 seconds at 20 TPS
    private static final float MIN_REGEN_THRESHOLD = 0.1f; // Stop regen when this close to max

    /**
     * Updates stamina for a player (call this every tick on SERVER)
     */
    public static void tick(Player player) {
        if (player == null || player.level().isClientSide) return;

        StaminaData data = getOrCreateData(player);

        // Always increment time since use
        data.timeSinceUse++;

        // Enhanced regeneration logic
        if (data.timeSinceUse >= data.regenDelay && data.current < data.max) {
            float regenAmount = data.regenRate;

            // Slow down regen as we approach max (smoother feel)
            float missingStamina = data.max - data.current;
            if (missingStamina < 10f) {
                regenAmount *= (missingStamina / 10f);
            }

            data.current = Math.min(data.max, data.current + regenAmount);

            // Stop micro-regeneration near max
            if (data.max - data.current < MIN_REGEN_THRESHOLD) {
                data.current = data.max;
            }

            // Sync to client every few ticks during regen (optimization)
            if (data.timeSinceUse % 4 == 0) {
                syncToClient(player, data);
            }
        }
    }

    /**
     * Consumes stamina for an action
     * @return true if successful, false if insufficient stamina
     */
    public static boolean consume(Player player, float amount) {
        if (player == null) return false;

        StaminaData data = getOrCreateData(player);
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
     * Checks if player has enough stamina
     */
    public static boolean hasStamina(Player player, float amount) {
        if (player == null) return false;
        StaminaData data = getOrCreateData(player);
        return data.current >= amount;
    }

    /**
     * Gets current stamina
     */
    public static float getStamina(Player player) {
        if (player == null) return 0;
        return getOrCreateData(player).current;
    }

    /**
     * Gets max stamina
     */
    public static float getMaxStamina(Player player) {
        if (player == null) return DEFAULT_MAX_STAMINA;
        return getOrCreateData(player).max;
    }

    /**
     * Restores stamina instantly
     */
    public static void restore(Player player, float amount) {
        if (player == null) return;

        StaminaData data = getOrCreateData(player);
        data.current = Math.min(data.max, data.current + amount);
        syncToClient(player, data);
    }

    /**
     * Fully restores stamina
     */
    public static void restoreFull(Player player) {
        if (player == null) return;

        StaminaData data = getOrCreateData(player);
        data.current = data.max;
        data.timeSinceUse = data.regenDelay; // Allow immediate regen if consumed again
        syncToClient(player, data);
    }

    /**
     * Sets max stamina and adjusts current if needed
     */
    public static void setMaxStamina(Player player, float max) {
        if (player == null) return;

        StaminaData data = getOrCreateData(player);
        data.max = Math.max(1, max);
        data.current = Math.min(data.current, data.max);
        syncToClient(player, data);
    }

    /**
     * Enhanced regen rate setting
     */
    public static void setRegenRate(Player player, float regenRate) {
        if (player == null) return;
        StaminaData data = getOrCreateData(player);
        data.regenRate = Math.max(0.1f, regenRate);
    }

    /**
     * Set regeneration delay
     */
    public static void setRegenDelay(Player player, int delayTicks) {
        if (player == null) return;
        StaminaData data = getOrCreateData(player);
        data.regenDelay = Math.max(0, delayTicks);
    }

    /**
     * Gets stamina percentage (0.0 to 1.0)
     */
    public static float getStaminaPercentage(Player player) {
        if (player == null) return 0f;
        StaminaData data = getOrCreateData(player);
        return data.current / data.max;
    }

    /**
     * Force sync stamina to client
     */
    public static void forceSyncToClient(Player player) {
        if (player == null) return;
        StaminaData data = getOrCreateData(player);
        syncToClient(player, data);
    }

    /**
     * Saves stamina data to NBT
     */
    public static void save(Player player, CompoundTag tag) {
        if (player == null) return;
        StaminaData data = playerStamina.get(player.getUUID());
        if (data != null) {
            CompoundTag staminaTag = new CompoundTag();
            staminaTag.putFloat("current", data.current);
            staminaTag.putFloat("max", data.max);
            staminaTag.putFloat("regenRate", data.regenRate);
            staminaTag.putInt("regenDelay", data.regenDelay);
            staminaTag.putInt("timeSinceUse", data.timeSinceUse);
            tag.put("StaminaData", staminaTag);
        }
    }

    /**
     * Loads stamina data from NBT
     */
    public static void load(Player player, CompoundTag tag) {
        if (player == null || !tag.contains("StaminaData")) return;

        CompoundTag staminaTag = tag.getCompound("StaminaData");
        StaminaData data = new StaminaData(
                staminaTag.getFloat("max"),
                staminaTag.getFloat("regenRate"),
                staminaTag.getInt("regenDelay")
        );
        data.current = staminaTag.getFloat("current");
        data.timeSinceUse = staminaTag.getInt("timeSinceUse");

        playerStamina.put(player.getUUID(), data);
        syncToClient(player, data);
    }

    /**
     * Cleans up data for disconnected players
     */
    public static void cleanupPlayer(Player player) {
        if (player != null) {
            playerStamina.remove(player.getUUID());
        }
    }

    /**
     * Syncs stamina data to client using your packet system
     */
    private static void syncToClient(Player player, StaminaData data) {
        if (player instanceof ServerPlayer serverPlayer) {
            // Create and send the stamina sync packet
            StaminaSyncPacket packet = new StaminaSyncPacket(player.getId(), data.current, data.max);
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }
    }

    private static StaminaData getOrCreateData(Player player) {
        return playerStamina.computeIfAbsent(player.getUUID(),
                uuid -> new StaminaData(DEFAULT_MAX_STAMINA, DEFAULT_REGEN_RATE, DEFAULT_REGEN_DELAY));
    }

    private static class StaminaData {
        float current;
        float max;
        float regenRate;
        int regenDelay;
        int timeSinceUse;

        StaminaData(float max, float regenRate, int regenDelay) {
            this.current = max;
            this.max = max;
            this.regenRate = regenRate;
            this.regenDelay = regenDelay;
            this.timeSinceUse = regenDelay;
        }
    }
}