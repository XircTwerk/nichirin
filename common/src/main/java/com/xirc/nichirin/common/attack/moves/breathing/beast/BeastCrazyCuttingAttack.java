package com.xirc.nichirin.common.attack.moves.breathing.beast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Fifth Fang: Crazy Cutting - Omnidirectional slicing while mid-air.
 * Levitates the user during the attack, hits all directions.
 */
public class BeastCrazyCuttingAttack extends BeastBreathingAttackBase {

    private boolean levitationApplied = false;

    @Override
    protected void onStart() {
        levitationApplied = false;

        // Apply levitation to float during attack
        user.addEffect(new MobEffectInstance(MobEffects.LEVITATION, windup + duration + 5, 1, false, false, false));
        levitationApplied = true;

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.7f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        Vec3 center = user.position().add(0, user.getBbHeight() / 2, 0);

        // Every 3 ticks, sweep in all directions
        if (tickCount % 3 == 0) {
            List<LivingEntity> targets = getTargetsInCircle(range, 12);
            for (LivingEntity target : targets) {
                hitTarget(target);
            }
            createOmniSlashEffect(center);
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.0f + (tickCount * 0.02f));
        }
    }

    private void createOmniSlashEffect(Vec3 center) {
        if (!(world instanceof ServerLevel sl)) return;

        double sweepRadius = range;
        int steps = 16;
        for (int i = 0; i < steps; i++) {
            double angle = (i / (double) steps) * 2 * Math.PI;
            double x = center.x + Math.cos(angle) * sweepRadius;
            double z = center.z + Math.sin(angle) * sweepRadius;

            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, x, center.y, z, 1, 0.05, 0.1, 0.05, 0);
            sl.sendParticles(ParticleTypes.CRIT, x, center.y, z, 1, 0.1, 0.1, 0.1, 0.05);

            if (i % 3 == 0) {
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, x, center.y + 0.5, z, 2, 0.2, 0.2, 0.2, 0.1);
            }
        }

        // Central burst
        sl.sendParticles(ParticleTypes.ENCHANTED_HIT, center.x, center.y, center.z,
                10, 0.5, 0.5, 0.5, 0.3);
    }

    @Override
    protected void onStop() {
        // Remove levitation early if attack ended before natural expiry
        if (levitationApplied && user.hasEffect(MobEffects.LEVITATION)) {
            user.removeEffect(MobEffects.LEVITATION);
        }
        levitationApplied = false;

        // Slow fall after attack
        user.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false, false));
    }
}
