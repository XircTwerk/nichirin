package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.network.s2c.StaminaSyncPacket;
import com.xirc.nichirin.common.system.perks.PerkManager;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side stamina manager. Demons, creative-mode players, and players with
 * the Unlimited Stamina config option always have full stamina.
 */
public class StaminaManager {

    private static final Map<UUID, StaminaData> playerStamina = new HashMap<>();

    private static final float DEFAULT_MAX_STAMINA = 100f;
    private static final float DEFAULT_REGEN_RATE = 1.2f;
    private static final int DEFAULT_REGEN_DELAY = 20; // 1 second before regen kicks in after use
    private static final float MIN_REGEN_THRESHOLD = 0.1f;

    private static boolean hasUnlimitedStamina(Player player) {
        if (player == null) return false;
        if (player.isCreative()) return true;
        if (MovesetHelper.hasDemonMoveset(player)) return true;
        if (NichirinModConfig.get().stamina.unlimitedStamina) return true;
        return false;
    }

    /**
     * Updates stamina for a player (call this every tick on SERVER)
     */
    public static void tick(Player player) {
        if (player == null || player.level().isClientSide) return;

        // Demons and creative players always have full stamina
        if (hasUnlimitedStamina(player)) {
            StaminaData data = getOrCreateData(player);
            if (data.current != data.max) {
                data.current = data.max;
                data.timeSinceUse = data.regenDelay;
                syncToClient(player, data);
            }
            return;
        }

        StaminaData data = getOrCreateData(player);

        // second_wind tick
        if (player instanceof ServerPlayer sp) {
            PerkManager.tickSecondWind(sp);
            // Detect stamina depletion → trigger second_wind
            boolean depleted = data.current <= 0f;
            if (PerkManager.checkAndUpdateDepletionState(sp, depleted) && depleted) {
                float instant = PerkManager.triggerSecondWind(sp);
                if (instant > 0f) {
                    data.current = Math.min(data.max, data.current + instant);
                    syncToClient(player, data);
                }
            }
        }

        // Always increment time since use
        data.timeSinceUse++;

        // iron_core: adds extra delay ticks before regen kicks in
        int effectiveDelay = data.regenDelay;
        if (player instanceof ServerPlayer sp) {
            effectiveDelay += PerkManager.getIronCoreDelayBonus(sp);
        }

        // Enhanced regeneration logic
        if (data.timeSinceUse >= effectiveDelay && data.current < data.max) {
            float regenAmount = NichirinModConfig.get().stamina.staminaRegenRate / 20.0f;

            // Apply perk regen multiplier (iron_core, enduring_spirit LEGENDARY, second_wind)
            if (player instanceof ServerPlayer sp) {
                regenAmount *= PerkManager.getStaminaRegenMultiplier(sp);
            }

            // Scale regen by hunger: empty = 0x, full (20) = 2x
            float hungerMultiplier = player.getFoodData().getFoodLevel() / 10.0f;
            regenAmount *= hungerMultiplier;

            float missingStamina = data.max - data.current;
            if (missingStamina < 10f) {
                regenAmount *= (missingStamina / 10f);
            }

            data.current = Math.min(data.max, data.current + regenAmount);

            if (data.max - data.current < MIN_REGEN_THRESHOLD) {
                data.current = data.max;
            }

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

        // Demons and creative players never consume stamina
        if (hasUnlimitedStamina(player)) {
            return true;
        }

        StaminaData data = getOrCreateData(player);

        // Apply perk cost multiplier (enduring_spirit, lightfoot, stamina_seeker flaw)
        if (player instanceof ServerPlayer sp) {
            amount *= PerkManager.getStaminaCostMultiplier(sp);
        }

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

        // Demons and creative players always have unlimited stamina
        if (hasUnlimitedStamina(player)) {
            return true;
        }

        StaminaData data = getOrCreateData(player);
        return data.current >= amount;
    }

    /**
     * Gets current stamina
     */
    public static float getStamina(Player player) {
        if (player == null) return 0;

        StaminaData data = getOrCreateData(player);

        // Demons and creative players always show full stamina
        if (hasUnlimitedStamina(player)) {
            return data.max;
        }

        return data.current;
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

        // Demons and creative players are always at full stamina anyway
        if (hasUnlimitedStamina(player)) {
            return;
        }

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

        // Demons and creative players should be at full stamina
        if (hasUnlimitedStamina(player)) {
            data.current = data.max;
        }

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

        // Demons and creative players always show 100%
        if (hasUnlimitedStamina(player)) {
            return 1.0f;
        }

        StaminaData data = getOrCreateData(player);
        return data.current / data.max;
    }

    /**
     * Force sync stamina to client
     */
    public static void forceSyncToClient(Player player) {
        if (player == null) return;
        StaminaData data = getOrCreateData(player);

        // Make sure demons and creative players show full stamina
        if (hasUnlimitedStamina(player)) {
            data.current = data.max;
        }

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

        // Ensure demons and creative players start with full stamina
        if (hasUnlimitedStamina(player)) {
            data.current = data.max;
        }

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
        StaminaData data = playerStamina.computeIfAbsent(player.getUUID(),
                uuid -> new StaminaData(DEFAULT_MAX_STAMINA, DEFAULT_REGEN_RATE, DEFAULT_REGEN_DELAY));

        // Ensure demons and creative players have full stamina
        if (hasUnlimitedStamina(player)) {
            data.current = data.max;
        }

        return data;
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