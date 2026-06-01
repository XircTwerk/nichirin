package com.xirc.nichirin.common.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public final class NichirinArmorDamage {
    public static final float ARMOR_DAMAGE_MULTIPLIER = 0.0625f;

    private static final ThreadLocal<Boolean> REDUCE_ARMOR_DAMAGE =
            ThreadLocal.withInitial(() -> false);

    private NichirinArmorDamage() {
    }

    public static boolean hurt(LivingEntity target, DamageSource source, float amount) {
        boolean previous = REDUCE_ARMOR_DAMAGE.get();
        REDUCE_ARMOR_DAMAGE.set(true);
        try {
            return target.hurt(source, amount);
        } finally {
            REDUCE_ARMOR_DAMAGE.set(previous);
        }
    }

    public static float scaleArmorDamage(DamageSource source, float amount) {
        return shouldReduceArmorDamage(source) ? amount * ARMOR_DAMAGE_MULTIPLIER : amount;
    }

    private static boolean shouldReduceArmorDamage(DamageSource source) {
        return REDUCE_ARMOR_DAMAGE.get();
    }
}
