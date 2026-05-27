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

// Form 1: Low Clouds, Distant Haze — velocity-based lunge that pierces enemies along the path.
public class LowCloudsDistantHazeAttack extends MistBreathingAttackBase {

    private boolean dashStarted = false;
    private Vec3 dashDirection;
    private Vec3 dashStartPos;
    private int dashTick = 0;
    private int dashDuration = 0;
    private final Set<LivingEntity> hitEntities = new HashSet<>();

    @Override
    protected void onStart() {
        dashStarted = false;
        dashTick = 0;
        dashDuration = 0;
        hitEntities.clear();
        dashDirection = null;
        dashStartPos = null;

        // Mist coils at feet during windup
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
            dashDirection = angledDashDirection();
            dashStartPos = user.position();
            dashDuration = Math.max(1, duration);
            dashStarted = true;

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.8f);
            playMistSound();
        }

        if (!dashStarted || dashDirection == null) return;

        if (dashTick < dashDuration) {
            dashTick++;
            float progress = (float) dashTick / dashDuration;
            Vec3 targetPos = dashStartPos.add(dashDirection.scale(range * 10 * progress));
            teleportSafe(targetPos);
        }

        createMistTrail(user.position(), user.position().subtract(dashDirection.scale(1.5)));

        List<LivingEntity> targets = getTargetsInRangeLine(1.2f);
        for (LivingEntity target : targets) {
            if (!hitEntities.contains(target)) {
                hitTarget(target);
                hitEntities.add(target);
                dashTick = dashDuration; // stop dash on hit
            }
        }
    }

    @Override
    protected void onStop() {
        user.setDeltaMovement(Vec3.ZERO);
        user.hurtMarked = true;

        if (world instanceof ServerLevel serverLevel) {
            Vec3 pos = user.position().add(0, 1, 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z,
                    20, 0.8, 0.5, 0.8, 0.04);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH, pos.x, pos.y, pos.z,
                    12, 0.6, 0.4, 0.6, 0.03);
        }

        dashStarted = false;
        dashTick = 0;
        dashDuration = 0;
        dashStartPos = null;
        hitEntities.clear();
    }

    private Vec3 angledDashDirection() {
        Vec3 look = user.getLookAngle();
        return new Vec3(look.x, Math.max(-0.25, Math.min(0.25, look.y)), look.z).normalize();
    }
}
