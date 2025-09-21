package com.xirc.nichirin.common.system;

import com.xirc.nichirin.common.data.MovesetHelper;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

/**
 * Handles all demon-related mechanics and passives
 */
public class DemonManager {

    // Track blood points per player (0-10)
    static final Map<UUID, Integer> playerBloodPoints = new HashMap<>();

    // Track last regen tick to prevent spam
    private static final Map<UUID, Long> lastRegenTick = new HashMap<>();

    // Track exhaustion accumulation for blood drain
    private static final Map<UUID, Float> accumulatedExhaustion = new HashMap<>();

    // Prevent infinite recursion during loading
    private static final Set<UUID> loadingPlayers = new HashSet<>();

    // Constants
    private static final int MAX_BLOOD_POINTS = 10;
    private static final int REGEN_INTERVAL = 20; // 1 second in ticks
    private static final float EXHAUSTION_THRESHOLD = 6.0f; // Slower than normal hunger (4.0f)
    private static final int SUN_FIRE_DURATION = 40; // 2 seconds of fire

    /**
     * Check if a player is a demon (has demon moveset)
     */
    public static boolean isDemon(Player player) {
        return MovesetHelper.hasDemonMoveset(player);
    }

    /**
     * Gets blood points for a player
     */
    public static int getBloodPoints(Player player) {
        return playerBloodPoints.getOrDefault(player.getUUID(), MAX_BLOOD_POINTS);
    }

    /**
     * Sets blood points for a player
     */
    public static void setBloodPoints(Player player, int bloodPoints) {
        // Prevent recursive calls during loading
        if (loadingPlayers.contains(player.getUUID())) {
            playerBloodPoints.put(player.getUUID(), Math.max(0, Math.min(bloodPoints, MAX_BLOOD_POINTS)));
            return;
        }

        bloodPoints = Math.max(0, Math.min(bloodPoints, MAX_BLOOD_POINTS));
        playerBloodPoints.put(player.getUUID(), bloodPoints);

        // Sync to client if on server
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            int halfBloodPoints = com.xirc.nichirin.common.event.DemonFoodHandler.getHalfBloodPoints(player);
            com.xirc.nichirin.registry.NichirinPacketRegistry.sendDemonSync(serverPlayer, bloodPoints, halfBloodPoints, true);
        }
    }

    /**
     * Sets blood points directly without sync (for loading)
     */
    public static void setBloodPointsDirectly(Player player, int bloodPoints) {
        loadingPlayers.add(player.getUUID());
        try {
            playerBloodPoints.put(player.getUUID(), Math.max(0, Math.min(bloodPoints, MAX_BLOOD_POINTS)));
        } finally {
            loadingPlayers.remove(player.getUUID());
        }
    }

    /**
     * Adds blood points (from killing/biting)
     */
    public static void addBloodPoints(Player player, int amount) {
        int current = getBloodPoints(player);
        setBloodPoints(player, current + amount);
    }

    /**
     * Removes blood points (from exhaustion)
     */
    public static void removeBloodPoints(Player player, int amount) {
        int current = getBloodPoints(player);
        setBloodPoints(player, current - amount);
    }

    /**
     * Tick method to be called every game tick for demon players
     */
    public static void tickDemon(Player player) {
        if (!isDemon(player)) return;

        // Handle sun damage (now sets on fire)
        handleSunDamage(player);

        // Handle blood regeneration
        handleBloodRegeneration(player);

        // Apply infinite stamina and maintain full hunger
        applyInfiniteStamina(player);

        // Handle blood drain from exhaustion
        handleBloodDrain(player);
    }

    /**
     * Handles sun damage for demons - now sets them on fire
     */
    private static void handleSunDamage(Player player) {
        Level level = player.level();

        // Only damage during day, not raining, can see sky
        if (level.isDay() && !level.isRaining() && !level.isThundering() &&
                level.canSeeSky(player.blockPosition())) {

            // Set demon on fire instead of direct damage
            player.setSecondsOnFire(SUN_FIRE_DURATION / 20); // Convert ticks to seconds
        }
    }

    /**
     * Handles blood-based regeneration
     */
    private static void handleBloodRegeneration(Player player) {
        long currentTime = player.level().getGameTime();
        UUID playerUUID = player.getUUID();

        Long lastRegen = lastRegenTick.get(playerUUID);
        if (lastRegen == null || currentTime - lastRegen >= REGEN_INTERVAL) {

            int bloodPoints = getBloodPoints(player);
            float currentHealth = player.getHealth();
            float maxHealth = player.getMaxHealth();

            if (currentHealth < maxHealth && bloodPoints > 0) {
                float regenRate = getRegenRate(bloodPoints);

                if (regenRate > 0) {
                    player.heal(regenRate);
                    lastRegenTick.put(playerUUID, currentTime);
                }
            }
        }
    }

    /**
     * Gets regeneration rate based on blood points
     */
    private static float getRegenRate(int bloodPoints) {
        if (bloodPoints == 0) return 0.0f;
        if (bloodPoints <= 5) return 1.5f; // 1.5 health/s for 1-5 blood
        return 3.0f; // 3 health/s for 6-10 blood
    }

    /**
     * Applies infinite stamina and maintains full hunger for demons
     */
    private static void applyInfiniteStamina(Player player) {
        FoodData foodData = player.getFoodData();

        // Remove exhaustion (existing functionality)
        if (foodData.getExhaustionLevel() > 0) {
            foodData.setExhaustion(0.0f);
        }

        // Maintain full hunger and saturation for demons
        if (foodData.getFoodLevel() < 20) {
            foodData.setFoodLevel(20); // Full hunger (20/20)
        }

        if (foodData.getSaturationLevel() < 20.0f) {
            foodData.setSaturation(20.0f); // Full saturation
        }
    }

    /**
     * Handles blood drain from exhaustion accumulation
     */
    private static void handleBloodDrain(Player player) {
        UUID playerUUID = player.getUUID();
        FoodData foodData = player.getFoodData();

        // Accumulate exhaustion (but don't let it affect hunger since we reset it)
        float currentExhaustion = foodData.getExhaustionLevel();
        if (currentExhaustion > 0) {
            float accumulated = accumulatedExhaustion.getOrDefault(playerUUID, 0.0f);
            accumulated += currentExhaustion;

            // Check if we've accumulated enough to drain blood
            if (accumulated >= EXHAUSTION_THRESHOLD) {
                removeBloodPoints(player, 1);
                accumulated -= EXHAUSTION_THRESHOLD;
            }

            accumulatedExhaustion.put(playerUUID, accumulated);
        }
    }

    /**
     * Called when a mob is killed to potentially award blood
     */
    public static void onMobKilled(Player player, LivingEntity killed) {
        if (!isDemon(player)) return;

        // Award blood for killing mobs that have blood
        if (hasBlood(killed)) {
            addBloodPoints(player, 1);
        }
    }

    /**
     * Called when bite attack hits to award blood
     */
    public static void onBiteHit(Player player, LivingEntity target) {
        if (!isDemon(player)) return;

        // More efficient blood gain from biting
        if (hasBlood(target)) {
            addBloodPoints(player, 2);
        }
    }

    /**
     * Checks if an entity has blood (not undead, not mechanical)
     */
    private static boolean hasBlood(LivingEntity entity) {
        // Most living entities have blood except undead
        return !entity.getType().is(EntityTypeTags.SKELETONS) &&
                !entity.getType().is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES);
    }

    /**
     * Clean up player data when they disconnect
     */
    public static void cleanupPlayer(Player player) {
        UUID playerUUID = player.getUUID();
        playerBloodPoints.remove(playerUUID);
        lastRegenTick.remove(playerUUID);
        accumulatedExhaustion.remove(playerUUID);
        loadingPlayers.remove(playerUUID);
    }

    /**
     * Clear all data (for mod reload/testing)
     */
    public static void clearAll() {
        playerBloodPoints.clear();
        lastRegenTick.clear();
        accumulatedExhaustion.clear();
        loadingPlayers.clear();
    }
}