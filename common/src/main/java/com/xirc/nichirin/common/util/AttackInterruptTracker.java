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
        if (!cfg.moveInterrupts.projectileDamageInterruptsMoves && isProjectile(source)) return false;
        if (!cfg.moveInterrupts.explosionDamageInterruptsMoves && isExplosion(source)) return false;
        if (!cfg.moveInterrupts.magicDamageInterruptsMoves && isMagic(source)) return false;
        if (!cfg.moveInterrupts.drowningDamageInterruptsMoves && source.is(DamageTypes.DROWN)) return false;
        if (!cfg.moveInterrupts.starvationDamageInterruptsMoves && source.is(DamageTypes.STARVE)) return false;
        return true;
    }

    private static boolean isFire(DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypes.IN_FIRE)
                || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA);
    }

    private static boolean isProjectile(DamageSource source) {
        return source.is(DamageTypeTags.IS_PROJECTILE)
                || source.is(DamageTypes.ARROW)
                || source.is(DamageTypes.TRIDENT);
    }

    private static boolean isExplosion(DamageSource source) {
        return source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypes.EXPLOSION)
                || source.is(DamageTypes.PLAYER_EXPLOSION);
    }

    private static boolean isMagic(DamageSource source) {
        return source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC);
    }
}