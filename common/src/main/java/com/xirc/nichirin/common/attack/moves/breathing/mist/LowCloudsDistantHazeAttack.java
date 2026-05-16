package com.xirc.nichirin.common.attack.moves.breathing.mist;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Form 1: Thrusting skewer dash. Pierces all enemies in a straight line.
public class LowCloudsDistantHazeAttack extends MistBreathingAttackBase {

    private boolean dashStarted = false;
    private Vec3 dashDirection;
    private Vec3 dashStartPos;
    private int dashTick = 0;

    @Override
    protected void onStart() {
        dashStarted = false;
        dashStartPos = null;
        dashTick = 0;
        // Flatten to horizontal so pitch doesn't cause diagonal drift (#3)
        Vec3 look = user.getLookAngle();
        dashDirection = new Vec3(look.x, 0, look.z).normalize();

        // mist coils at feet during crouch windup
        if (world instanceof ServerLevel serverLevel) {
            Vec3 feetPos = user.position();
            for (int i = 0; i < 16; i++) {
                double angle = (2 * Math.PI * i) / 16;
                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        feetPos.x + Math.cos(angle), feetPos.y + 0.15, feetPos.z + Math.sin(angle),
                        1, 0.05, 0.0, 0.05, 0.01);
            }
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 0.8f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (!dashStarted && tickCount == windup + 1) {
            launchDash();
            dashStarted = true;
        }

        if (!dashStarted) return;

        // Teleport-based movement for precision (velocity is overridden by client prediction)
        if (dashStartPos != null && dashSpeed != null) {
            dashTick++;
            double totalDistance = dashSpeed * 8.0; // matches original velocity * duration
            float progress = (float) dashTick / Math.max(duration, 1);
            Vec3 targetPos = dashStartPos.add(dashDirection.scale(totalDistance * progress));
            teleportSafe(targetPos);
        }

        createMistTrail(user.position(), user.position().subtract(dashDirection.scale(2)));

        List<LivingEntity> targets = getTargetsInRangeLine(1.2f);
        for (LivingEntity target : targets) {
            hitTarget(target);
        }
    }

    private void launchDash() {
        dashStartPos = user.position();
        dashTick = 0;

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.8f);
        playMistSound();
    }

    @Override
    protected void onStop() {
        user.setDeltaMovement(Vec3.ZERO);

        if (world instanceof ServerLevel serverLevel) {
            Vec3 pos = user.position().add(0, 1, 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z,
                    20, 0.8, 0.5, 0.8, 0.04);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH, pos.x, pos.y, pos.z,
                    12, 0.6, 0.4, 0.6, 0.03);
        }

        dashStarted = false;
        dashStartPos = null;
        dashTick = 0;
    }
}
