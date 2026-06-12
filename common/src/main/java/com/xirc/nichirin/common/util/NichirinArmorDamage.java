package com.xirc.nichirin.common.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NichirinArmorDamage {
    public static final float ARMOR_DAMAGE_MULTIPLIER = 0.0625f;

    private static final ThreadLocal<Boolean> REDUCE_ARMOR_DAMAGE =
            ThreadLocal.withInitial(() -> false);

    // Last ACTUAL damage (health + absorption delta) dealt to each victim via hurt(), keyed by
    // victim UUID and stamped with the game time so stale records never leak into later hits.
    // Read by ComboIntegration so the combo bar shows real post-armor damage, not the raw amount.
    private static final Map<UUID, ActualDamage> LAST_ACTUAL = new ConcurrentHashMap<>();

    private record ActualDamage(long gameTime, float amount) {}

    private NichirinArmorDamage() {
    }

    public static boolean hurt(LivingEntity target, DamageSource source, float amount) {
        boolean previous = REDUCE_ARMOR_DAMAGE.get();
        REDUCE_ARMOR_DAMAGE.set(true);
        try {
            float before = target.getHealth() + target.getAbsorptionAmount();
            boolean result = target.hurt(source, amount);
            float dealt = Math.max(0.0f, before - (target.getHealth() + target.getAbsorptionAmount()));
            LAST_ACTUAL.put(target.getUUID(), new ActualDamage(target.level().getGameTime(), dealt));
            return result;
        } finally {
            REDUCE_ARMOR_DAMAGE.set(previous);
        }
    }

    /**
     * Actual damage dealt to {@code target} by a hurt() call this tick, or {@code fallback} when
     * no fresh measurement exists. Consumes the record so it can't be double-counted.
     */
    public static float actualDamageOr(LivingEntity target, float fallback) {
        ActualDamage record = LAST_ACTUAL.remove(target.getUUID());
        if (record == null || record.gameTime != target.level().getGameTime()) {
            return fallback;
        }
        return record.amount;
    }

    public static float scaleArmorDamage(DamageSource source, float amount) {
        return shouldReduceArmorDamage(source) ? amount * ARMOR_DAMAGE_MULTIPLIER : amount;
    }

    private static boolean shouldReduceArmorDamage(DamageSource source) {
        return REDUCE_ARMOR_DAMAGE.get();
    }
}
