package com.xirc.nichirin.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * "Blurry" status effect applied by Mist Breathing attacks.
 *
 * Client-side: triggers a blurry mist-colored screen shader (handled by MistBlurShaderHandler).
 * Server-side: confuses mob AI — redirects mobs to wander to the wrong location each tick.
 */
public class BlurryStatusEffect extends MobEffect {

    public BlurryStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0xB0D8E8); // pale mist-blue
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Apply every 10 ticks (0.5s) to avoid flooding navigation
        return duration % 10 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide) return;
        if (!(entity instanceof Mob mob)) return;

        // Send the mob to a random position far from their actual target
        Vec3 pos = entity.position();
        double angle = entity.level().getRandom().nextDouble() * 2 * Math.PI;
        double radius = 8.0 + entity.level().getRandom().nextDouble() * 8.0;
        double targetX = pos.x + Math.cos(angle) * radius;
        double targetZ = pos.z + Math.sin(angle) * radius;

        mob.getNavigation().moveTo(targetX, pos.y, targetZ, 1.0);
        // Also temporarily forget the current attack target so they don't immediately snap back
        mob.setTarget(null);
    }
}
