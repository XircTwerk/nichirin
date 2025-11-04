package com.xirc.nichirin.common.system;

import com.xirc.nichirin.common.entity.MovesetCapableNPC;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages blood and breath resources for NPCs with movesets
 * Simplified version of player systems for NPC use
 */
public class NPCResourceManager {

    // Track blood points per NPC
    private static final Map<UUID, Integer> npcBloodPoints = new HashMap<>();

    // Track breath gauge per NPC
    private static final Map<UUID, Float> npcBreathGauge = new HashMap<>();

    // Track last regen tick
    private static final Map<UUID, Long> lastBloodRegenTick = new HashMap<>();
    private static final Map<UUID, Long> lastBreathRegenTick = new HashMap<>();

    // Simplified constants for NPCs
    private static final int BLOOD_REGEN_INTERVAL = 20; // 1 second (slower than players)
    private static final int BREATH_REGEN_INTERVAL = 10; // 0.5 seconds
    private static final float BASE_BREATH_REGEN = 2.0f; // Faster than players

    /**
     * Tick an NPC's resource systems
     */
    public static void tickNPC(MovesetCapableNPC npc) {
        if (npc == null) return;

        LivingEntity entity = npc.asLivingEntity();
        if (entity.level().isClientSide) return;

        // Tick blood regen for demon NPCs
        if (npc.getMoveset() != null && npc.getMoveset().isDemonMoveset()) {
            tickBloodRegen(npc, entity);
        }

        // Tick breath regen for all NPCs with movesets
        if (npc.getMoveset() != null) {
            tickBreathRegen(npc, entity);
        }
    }

    /**
     * Handle blood regeneration for demon NPCs
     */
    private static void tickBloodRegen(MovesetCapableNPC npc, LivingEntity entity) {
        if (!npc.canRegenBlood()) return;

        // Block regen if stunned
        if (entity.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return;
        }

        // Block regen if on fire
        if (entity.getRemainingFireTicks() > 0) {
            return;
        }

        long currentTime = entity.level().getGameTime();
        UUID entityUUID = npc.getEntityUUID();
        Long lastRegen = lastBloodRegenTick.get(entityUUID);

        if (lastRegen == null || currentTime - lastRegen >= BLOOD_REGEN_INTERVAL) {
            int bloodPoints = npc.getBloodPoints();
            float currentHealth = entity.getHealth();
            float maxHealth = entity.getMaxHealth();

            if (currentHealth < maxHealth && bloodPoints > 0) {
                float regenRate = getBloodRegenRate(bloodPoints) * npc.getBloodRegenMultiplier();

                if (regenRate > 0) {
                    entity.heal(regenRate);
                    lastBloodRegenTick.put(entityUUID, currentTime);
                }
            }
        }
    }

    /**
     * Handle breath regeneration for NPCs
     */
    private static void tickBreathRegen(MovesetCapableNPC npc, LivingEntity entity) {
        long currentTime = entity.level().getGameTime();
        UUID entityUUID = npc.getEntityUUID();
        Long lastRegen = lastBreathRegenTick.get(entityUUID);

        if (lastRegen == null || currentTime - lastRegen >= BREATH_REGEN_INTERVAL) {
            float breath = npc.getBreathGauge();
            float maxBreath = npc.getMaxBreathGauge();

            if (breath < maxBreath) {
                float regenRate = BASE_BREATH_REGEN * npc.getBreathRegenMultiplier();
                float newBreath = Math.min(maxBreath, breath + regenRate);
                npc.setBreathGauge(newBreath);
                lastBreathRegenTick.put(entityUUID, currentTime);
            }
        }
    }

    /**
     * Gets blood regeneration rate based on blood points
     */
    private static float getBloodRegenRate(int bloodPoints) {
        if (bloodPoints == 0) return 0.0f;
        // Simplified scaling: 0.3 to 2.0 hp/s
        return 0.3f + (bloodPoints - 1) * 0.188f;
    }

    /**
     * Gets blood points for an NPC
     */
    public static int getBloodPoints(UUID entityUUID, int defaultMax) {
        return npcBloodPoints.getOrDefault(entityUUID, defaultMax);
    }

    /**
     * Sets blood points for an NPC
     */
    public static void setBloodPoints(UUID entityUUID, int bloodPoints, int maxBlood) {
        npcBloodPoints.put(entityUUID, Math.max(0, Math.min(bloodPoints, maxBlood)));
    }

    /**
     * Gets breath gauge for an NPC
     */
    public static float getBreathGauge(UUID entityUUID, float defaultMax) {
        return npcBreathGauge.getOrDefault(entityUUID, defaultMax);
    }

    /**
     * Sets breath gauge for an NPC
     */
    public static void setBreathGauge(UUID entityUUID, float breath, float maxBreath) {
        npcBreathGauge.put(entityUUID, Math.max(0, Math.min(breath, maxBreath)));
    }

    /**
     * Consume breath for an NPC move
     */
    public static boolean consumeBreath(MovesetCapableNPC npc, float amount) {
        float currentBreath = npc.getBreathGauge();
        if (currentBreath >= amount) {
            npc.setBreathGauge(currentBreath - amount);
            return true;
        }
        return false;
    }

    /**
     * Remove blood points from an NPC
     */
    public static void removeBloodPoints(MovesetCapableNPC npc, int amount) {
        int current = npc.getBloodPoints();
        npc.setBloodPoints(Math.max(0, current - amount));
    }

    /**
     * Add blood points to an NPC
     */
    public static void addBloodPoints(MovesetCapableNPC npc, int amount) {
        int current = npc.getBloodPoints();
        int max = npc.getMaxBloodPoints();
        npc.setBloodPoints(Math.min(max, current + amount));
    }

    /**
     * Clean up NPC data
     */
    public static void cleanupNPC(UUID entityUUID) {
        npcBloodPoints.remove(entityUUID);
        npcBreathGauge.remove(entityUUID);
        lastBloodRegenTick.remove(entityUUID);
        lastBreathRegenTick.remove(entityUUID);
    }

    /**
     * Clear all data
     */
    public static void clearAll() {
        npcBloodPoints.clear();
        npcBreathGauge.clear();
        lastBloodRegenTick.clear();
        lastBreathRegenTick.clear();
    }
}