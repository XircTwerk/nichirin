package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.config.NichirinModConfig;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AttackInterruptTracker {
    private static final Map<UUID, Long> LAST_INTERRUPTING_HURT = new ConcurrentHashMap<>();

    private AttackInterruptTracker() {
    }

    public static void record(LivingEntity entity, DamageSource source) {
        if (entity.level().isClientSide || !shouldInterrupt(source)) return;
        LAST_INTERRUPTING_HURT.put(entity.getUUID(), entity.level().getGameTime());
    }

    public static boolean wasInterruptedThisTick(LivingEntity entity) {
        return LAST_INTERRUPTING_HURT.getOrDefault(entity.getUUID(), Long.MIN_VALUE) == entity.level().getGameTime();
    }

    private static boolean shouldInterrupt(DamageSource source) {
        NichirinModConfig cfg = NichirinModConfig.get();
        if (!cfg.moveInterrupts.fireDamageInterruptsMoves && isFire(source)) return false;
        if (!cfg.moveInterrupts.fallDamageInterruptsMoves && source.is(DamageTypes.FALL)) return false;
        return true;
    }

    private static boolean isFire(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA);
    }
}
