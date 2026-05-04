package com.xirc.nichirin.common.system;

import com.xirc.nichirin.common.entity.MovesetCapableNPC;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NPCResourceManager {

    private static final Map<UUID, Integer> npcBloodPoints   = new HashMap<>();
    private static final Map<UUID, Float>   npcBreathGauge   = new HashMap<>();
    private static final Map<UUID, Float>   npcStamina       = new HashMap<>();

    private static final Map<UUID, Long> lastBloodRegenTick   = new HashMap<>();
    private static final Map<UUID, Long> lastBreathRegenTick  = new HashMap<>();
    private static final Map<UUID, Long> lastStaminaRegenTick = new HashMap<>();

    private static final Map<UUID, Boolean> doubleJumpUsed = new HashMap<>();

    private static final int   BLOOD_REGEN_INTERVAL   = 20;
    private static final int   BREATH_REGEN_INTERVAL  = 10;
    private static final int   STAMINA_REGEN_INTERVAL = 8;
    private static final float BASE_BREATH_REGEN      = 2.0f;
    private static final float BASE_STAMINA_REGEN     = 3.0f;


    public static void tickNPC(MovesetCapableNPC npc) {
        if (npc == null) return;
        LivingEntity entity = npc.asLivingEntity();
        if (entity.level().isClientSide) return;

        if (npc.getMoveset() != null && npc.getMoveset().isDemonMoveset()) {
            tickBloodRegen(npc, entity);
        }
        if (npc.getMoveset() != null) {
            tickBreathRegen(npc, entity);
            tickStaminaRegen(npc, entity);
        }
    }


    private static void tickBloodRegen(MovesetCapableNPC npc, LivingEntity entity) {
        if (!npc.canRegenBlood()) return;
        if (entity.hasEffect(NichirinEffectRegistry.STUNNED.get())) return;
        if (entity.getRemainingFireTicks() > 0) return;

        long now = entity.level().getGameTime();
        UUID id  = npc.getEntityUUID();
        Long last = lastBloodRegenTick.get(id);

        if (last == null || now - last >= BLOOD_REGEN_INTERVAL) {
            int   blood  = npc.getBloodPoints();
            float health = entity.getHealth();
            float max    = entity.getMaxHealth();

            if (health < max && blood > 0) {
                float rate = bloodRegenRate(blood) * npc.getBloodRegenMultiplier();
                if (rate > 0) {
                    entity.heal(rate);
                    lastBloodRegenTick.put(id, now);
                }
            }
        }
    }

    private static float bloodRegenRate(int bloodPoints) {
        if (bloodPoints == 0) return 0f;
        return 0.3f + (bloodPoints - 1) * 0.188f;
    }

    public static int getBloodPoints(UUID id, int defaultMax) {
        return npcBloodPoints.getOrDefault(id, defaultMax);
    }

    public static void setBloodPoints(UUID id, int value, int max) {
        npcBloodPoints.put(id, Math.max(0, Math.min(value, max)));
    }

    public static void removeBloodPoints(MovesetCapableNPC npc, int amount) {
        npc.setBloodPoints(Math.max(0, npc.getBloodPoints() - amount));
    }

    public static void addBloodPoints(MovesetCapableNPC npc, int amount) {
        npc.setBloodPoints(Math.min(npc.getMaxBloodPoints(), npc.getBloodPoints() + amount));
    }


    private static void tickBreathRegen(MovesetCapableNPC npc, LivingEntity entity) {
        float breath = npc.getBreathGauge();
        float max    = npc.getMaxBreathGauge();
        if (breath >= max) return;

        long now  = entity.level().getGameTime();
        UUID id   = npc.getEntityUUID();
        Long last = lastBreathRegenTick.get(id);

        if (last == null || now - last >= BREATH_REGEN_INTERVAL) {
            float rate = BASE_BREATH_REGEN * npc.getBreathRegenMultiplier();
            npc.setBreathGauge(Math.min(max, breath + rate));
            lastBreathRegenTick.put(id, now);
        }
    }

    public static float getBreathGauge(UUID id, float defaultMax) {
        return npcBreathGauge.getOrDefault(id, defaultMax);
    }

    public static void setBreathGauge(UUID id, float value, float max) {
        npcBreathGauge.put(id, Math.max(0f, Math.min(value, max)));
    }

    public static boolean consumeBreath(MovesetCapableNPC npc, float amount) {
        float current = npc.getBreathGauge();
        if (current < amount) return false;
        npc.setBreathGauge(current - amount);
        return true;
    }


    private static void tickStaminaRegen(MovesetCapableNPC npc, LivingEntity entity) {
        float stamina = npc.getStamina();
        float max     = npc.getMaxStamina();
        if (stamina >= max) return;

        long now  = entity.level().getGameTime();
        UUID id   = npc.getEntityUUID();
        Long last = lastStaminaRegenTick.get(id);

        if (last == null || now - last >= STAMINA_REGEN_INTERVAL) {
            float rate = BASE_STAMINA_REGEN * npc.getStaminaRegenMultiplier();
            npc.setStamina(Math.min(max, stamina + rate));
            lastStaminaRegenTick.put(id, now);
        }
    }

    public static float getStamina(UUID id, float defaultMax) {
        return npcStamina.getOrDefault(id, defaultMax);
    }

    public static void setStamina(UUID id, float value, float max) {
        npcStamina.put(id, Math.max(0f, Math.min(value, max)));
    }

    public static boolean consumeStamina(MovesetCapableNPC npc, float amount) {
        float current = npc.getStamina();
        if (current < amount) return false;
        npc.setStamina(current - amount);
        return true;
    }


    public static boolean canDoubleJump(UUID id) {
        return !doubleJumpUsed.getOrDefault(id, false);
    }

    public static void markDoubleJumped(UUID id) {
        doubleJumpUsed.put(id, true);
    }

    public static void resetDoubleJump(UUID id) {
        doubleJumpUsed.put(id, false);
    }


    public static void cleanupNPC(UUID id) {
        npcBloodPoints.remove(id);
        npcBreathGauge.remove(id);
        npcStamina.remove(id);
        lastBloodRegenTick.remove(id);
        lastBreathRegenTick.remove(id);
        lastStaminaRegenTick.remove(id);
        doubleJumpUsed.remove(id);
    }

    public static void clearAll() {
        npcBloodPoints.clear();
        npcBreathGauge.clear();
        npcStamina.clear();
        lastBloodRegenTick.clear();
        lastBreathRegenTick.clear();
        lastStaminaRegenTick.clear();
        doubleJumpUsed.clear();
    }
}
