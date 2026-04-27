package com.xirc.nichirin.common.attack.moves.breathing.mist;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fourth Form: Shifting Flow Slash
 * 10-tick low-stance windup, then an 18-block dash completed in ~10 ticks.
 * Slashes all enemies in the dash path, then delivers a powerful final slash at the end.
 * Good stun on both path hits and finisher.
 */
public class ShiftingFlowSlashAttack extends MistBreathingAttackBase {

    private boolean dashStarted = false;
    private boolean finisherExecuted = false;
    private Vec3 dashDirection;
    private Vec3 startPosition;
    private final Set<LivingEntity> hitDuringDash = new HashSet<>();

    @Override
    protected void onStart() {
        dashStarted = false;
        finisherExecuted = false;
        hitDuringDash.clear();
        dashDirection = user.getLookAngle().normalize();
        startPosition = user.position();

        // Low stance startup: mist pooling at feet
        if (world instanceof ServerLevel serverLevel) {
            Vec3 pos = user.position();
            for (int i = 0; i < 20; i++) {
                double angle = (2 * Math.PI * i) / 20;
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        pos.x + Math.cos(angle) * 1.5, pos.y + 0.1, pos.z + Math.sin(angle) * 1.5,
                        1, 0.1, 0.0, 0.1, 0.01);
            }
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 0.7f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (!dashStarted && tickCount == windup + 1) {
            launchDash();
            dashStarted = true;
        }

        if (!dashStarted) return;

        sustainDash();
        slashEnemiesInPath();

        // Final slash triggers in the last 2 ticks of the dash
        if (!finisherExecuted && tickCount >= windup + duration - 2) {
            executeFinisher();
            finisherExecuted = true;
        }
    }

    private void launchDash() {
        if (dashSpeed != null) {
            user.setDeltaMovement(dashDirection.scale(dashSpeed / Math.max(duration, 1) * 12.0));
            user.hurtMarked = true;
            user.hasImpulse = true;
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9f, 1.6f);
        playMistSound();
    }

    private void sustainDash() {
        if (dashSpeed != null) {
            user.setDeltaMovement(dashDirection.scale(dashSpeed / Math.max(duration, 1) * 9.0));
            user.hurtMarked = true;
        }

        // Low mist trail hugging the ground during dash
        if (world instanceof ServerLevel serverLevel) {
            Vec3 pos = user.position();
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    pos.x, pos.y + 0.3, pos.z, 5, 0.4, 0.1, 0.4, 0.02);
        }
    }

    private void slashEnemiesInPath() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Hit enemies the dash passes through
        List<LivingEntity> targets = getTargetsInLine(
                startPosition,
                userPos.add(dashDirection.scale(2.5)),
                2.0
        );

        for (LivingEntity target : targets) {
            if (!hitDuringDash.contains(target)) {
                hitTarget(target);
                hitDuringDash.add(target);

                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.3f);
            }
        }
    }

    private void executeFinisher() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Wide sweeping final slash at the end of the dash
        List<LivingEntity> sweepTargets = getTargetsInSweep(100f, range * 0.35f, 6);

        for (LivingEntity target : sweepTargets) {
            hitTarget(target);
            createMistHitParticles(target.position());
        }

        // Final flash
        createMistCircle(userPos, 3.5f, 18);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.1f, 1.2f);
    }

    @Override
    protected void onStop() {
        user.setDeltaMovement(Vec3.ZERO);
        hitDuringDash.clear();
        dashStarted = false;
        finisherExecuted = false;
    }
}
