package com.xirc.nichirin.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Burning status effect for Flame Breathing attacks
 * Sets enemies on fire for the duration of the effect
 */
public class BurningStatusEffect extends MobEffect {

    public BurningStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF6600); // Orange color for fire
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Skip creative mode players
        if (entity instanceof Player player && player.isCreative()) {
            return;
        }

        // Skip fire immune entities
        if (entity.fireImmune()) {
            return;
        }

        // Get the effect instance to check remaining duration
        MobEffectInstance effectInstance = entity.getEffect(this);
        if (effectInstance != null && effectInstance.getDuration() <= 1) {
            entity.setSecondsOnFire(0);
        } else {
            // Keep the fire visual but deal damage directly at 1/3 rate
            // (ticks every 60 ticks = once per 3 seconds, dealing 1 damage each time)
            entity.setRemainingFireTicks(2);
            entity.hurt(entity.damageSources().onFire(), 0.5f);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 60 == 0 || duration == 1;
    }

    @Override
    public boolean isInstantenous() {
        return false; // This is a duration-based effect
    }
}