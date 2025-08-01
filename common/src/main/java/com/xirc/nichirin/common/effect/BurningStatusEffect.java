package com.xirc.nichirin.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
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

        // Keep the entity on fire by refreshing fire ticks
        // Set fire for 3 seconds (60 ticks) to ensure continuous burning
        entity.setSecondsOnFire(3);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Apply the effect every 20 ticks (1 second) to maintain fire
        // This ensures fire doesn't get extinguished while the effect is active
        return duration % 20 == 0;
    }

    @Override
    public boolean isInstantenous() {
        return false; // This is a duration-based effect
    }
}