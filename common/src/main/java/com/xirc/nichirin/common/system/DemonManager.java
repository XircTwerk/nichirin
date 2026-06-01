package com.xirc.nichirin.common.system;

import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.event.system.DemonFoodHandler;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Difficulty;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Manages demon passives: sunlight, blood points, blood drain, and regen. */
public class DemonManager {

    static final Map<UUID, Integer> playerBloodPoints = new HashMap<>();
    private static final Map<UUID, Long> lastRegenTick = new HashMap<>();
    private static final Map<UUID, Long> lastBloodDrainTick = new HashMap<>();

    private static final Set<UUID> loadingPlayers = new HashSet<>();

    private static final int REGEN_INTERVAL = 15;
    private static final int SUN_FIRE_DURATION = 40;

    /**
     * Check if a player is a demon (has demon moveset)
     */
    public static boolean isDemon(Player player) {
        return MovesetHelper.hasDemonMoveset(player);
    }

    /**
     * Gets blood points for a player. New demons spawn with one blood point below the
     * configured max — see project rules: "when you're a demon you spawn at 1 less than max blood".
     */
    public static int getBloodPoints(Player player) {
        int max = NichirinModConfig.get().demon.maxBloodPoints;
        return playerBloodPoints.getOrDefault(player.getUUID(), Math.max(0, max - 1));
    }

    /**
     * Sets blood points for a player
     */
    public static void setBloodPoints(Player player, int bloodPoints) {
        int max = NichirinModConfig.get().demon.maxBloodPoints;
        if (loadingPlayers.contains(player.getUUID())) {
            playerBloodPoints.put(player.getUUID(), Math.max(0, Math.min(bloodPoints, max)));
            return;
        }

        bloodPoints = Math.max(0, Math.min(bloodPoints, max));
        playerBloodPoints.put(player.getUUID(), bloodPoints);
        if (bloodPoints >= max && DemonFoodHandler.getHalfBloodPoints(player) > 0) {
            DemonFoodHandler.setHalfBloodPointsDirectly(player, 0);
        }

        // Sync to client if on server
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            int halfBloodPoints = DemonFoodHandler.getHalfBloodPoints(player);
            NichirinPacketRegistry.sendDemonSync(serverPlayer, bloodPoints, halfBloodPoints, true);
        }
    }

    /**
     * Sets blood points directly without sync (for loading)
     */
    public static void setBloodPointsDirectly(Player player, int bloodPoints) {
        loadingPlayers.add(player.getUUID());
        try {
            int max = NichirinModConfig.get().demon.maxBloodPoints;
            playerBloodPoints.put(player.getUUID(), Math.max(0, Math.min(bloodPoints, max)));
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

        if (player.level().getDifficulty() == Difficulty.PEACEFUL
                && NichirinModConfig.get().demon.peacefulModeMaxBlood) {
            DemonFoodHandler.setHalfBloodPointsDirectly(player, 0);
            setBloodPoints(player, NichirinModConfig.get().demon.maxBloodPoints);
        }

        // Handle sun damage (now sets on fire)
        handleSunDamage(player);

        // Handle extra fire damage
        handleFireDamage(player);

        // Handle blood regeneration (checks for stun and fire)
        handleBloodRegeneration(player);

        // Handle blood drain BEFORE resetting exhaustion so the reading is accurate
        handleBloodDrain(player);

        // Apply infinite stamina and maintain full hunger
        applyInfiniteStamina(player);
    }

    private static void handleSunDamage(Player player) {
        if (player.isCreative()) return;
        if (!NichirinModConfig.get().demon.burnInSunlight) return;
        Level level = player.level();
        if (level.isDay() && !level.isRaining() && !level.isThundering()
                && level.canSeeSky(player.blockPosition())) {
            player.setSecondsOnFire(SUN_FIRE_DURATION / 20);
        }
    }

    private static void handleFireDamage(Player player) {
        if (player.isCreative()) return;
        if (player.getRemainingFireTicks() > 0 && player.getRemainingFireTicks() % 20 == 0) {
            // Sunlight chews through 20% of max health every second — keep demon players honest.
            float damage = player.getMaxHealth() * 0.20f;
            player.hurt(player.damageSources().magic(), damage);
        }
    }

    /**
     * Handles blood-based regeneration with stun check and fire check
     */
    private static void handleBloodRegeneration(Player player) {
        long currentTime = player.level().getGameTime();
        UUID playerUUID = player.getUUID();
        boolean hasStunEffect = player.hasEffect(NichirinEffectRegistry.STUNNED.get());
        if (hasStunEffect) {
            return; // Block regen during any stun
        }
        int fireTicks = player.getRemainingFireTicks();
        if (fireTicks > 0) {
            return; // Block regen while burning
        }

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
     * Gets regeneration rate based on blood points with gradual scaling
     * 10 blood: 3.0 hp/s
     * 5 blood: 1.5 hp/s
     * 1 blood: 0.5 hp/s
     */
    private static float getRegenRate(int bloodPoints) {
        if (bloodPoints == 0) return 0.0f;

        // Gradual scaling from 0.5 to 3.0 hp/s
        // Formula: 0.5 + (bloodPoints - 1) * 0.277778
        // This gives: 1→0.5, 2→0.78, 3→1.06, 4→1.33, 5→1.5, 6→1.78, 7→2.06, 8→2.33, 9→2.61, 10→3.0
        return 0.5f + (bloodPoints - 1) * 0.277778f;
    }

    /**
     * Applies infinite stamina and maintains full hunger ONLY for demons
     * BUT prevents natural regeneration - demons only heal via blood regen
     */
    private static void applyInfiniteStamina(Player player) {
        // Double-check demon status to prevent non-demons from getting benefits
        if (!isDemon(player)) return;

        FoodData foodData = player.getFoodData();

        // Remove exhaustion (existing functionality)
        if (foodData.getExhaustionLevel() > 0) {
            foodData.setExhaustion(0.0f);
        }

        // Maintain full hunger for demons BUT keep saturation at 0 to prevent vanilla natural regen
        if (foodData.getFoodLevel() < 20) {
            foodData.setFoodLevel(20); // Full hunger (20/20)
        }
        // Demons should only heal via blood regeneration, not vanilla mechanics
        if (foodData.getSaturationLevel() > 0.0f) {
            foodData.setSaturation(0.0f); // No saturation = no vanilla regen
        }
    }

    /**
     * Periodic blood drain is disabled by design: demons should ONLY lose blood from taking
     * damage. The config flag {@code demon.bloodDrainEnabled} is left in place so a server
     * owner could re-enable it via mixin / fork, but the tick handler no longer triggers it.
     * Kept as a no-op (rather than deleted) so save data with {@code lastBloodDrainTick}
     * keys still loads cleanly.
     */
    private static void handleBloodDrain(Player player) {
        // intentionally empty — blood is now lost only from damage, see DemonFoodHandler.
    }

    /**
     * Called when a mob is killed to potentially award blood
     */
    public static void onMobKilled(Player player, LivingEntity killed) {
        if (!isDemon(player)) return;

        if (hasBlood(killed)) {
            addBloodPoints(player, NichirinModConfig.get().demon.bloodPointsOnKill);
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
                entity.getMobType() != MobType.UNDEAD;
    }

    /**
     * Clean up player data when they disconnect
     */
    public static void cleanupPlayer(Player player) {
        UUID playerUUID = player.getUUID();
        playerBloodPoints.remove(playerUUID);
        lastRegenTick.remove(playerUUID);
        lastBloodDrainTick.remove(playerUUID);
        loadingPlayers.remove(playerUUID);
    }

    public static void clearAll() {
        playerBloodPoints.clear();
        lastRegenTick.clear();
        lastBloodDrainTick.clear();
        loadingPlayers.clear();
    }
}
