package com.xirc.nichirin.common.attack.moves.breathing.mist;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Form 4: Low-stance windup into a long dash. Slashes through the path and finishes with a wide sweep.
public class ShiftingFlowSlashAttack extends MistBreathingAttackBase {

    private boolean dashStarted = false;
    private boolean finisherExecuted = false;
    private Vec3 dashDirection;
    private Vec3 dashStartPos;
    private int dashTick = 0;
    private Vec3 lastDashPos = null;
    private final Set<LivingEntity> hitDuringDash = new HashSet<>();

    @Override
    protected void onStart() {
        dashStarted = false;
        finisherExecuted = false;
        hitDuringDash.clear();
        dashDirection = angledDashDirection();
        dashStartPos = null;
        dashTick = 0;

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

        if (!finisherExecuted && tickCount >= windup + duration - 2) {
            executeFinisher();
            finisherExecuted = true;
        }
    }

    private void launchDash() {
        dashStartPos = user.position();
        dashTick = 0;

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9f, 1.6f);
        playMistSound();
    }

    private void sustainDash() {
        if (dashStartPos != null && dashSpeed != null) {
            dashTick++;
            float progress = (float) dashTick / Math.max(duration, 1);
            Vec3 targetPos = dashStartPos.add(dashDirection.scale(range * progress));
            lastDashPos = user.position();
            teleportSafe(targetPos);
        }
        if (world instanceof ServerLevel serverLevel) {
            Vec3 pos = user.position();
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    pos.x, pos.y + 0.3, pos.z, 5, 0.4, 0.1, 0.4, 0.02);
        }
    }

    private void slashEnemiesInPath() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Sweep hitbox from last position to current to fill gaps caused by teleport
        List<LivingEntity> targets;
        if (lastDashPos != null) {
            targets = getTargetsAlongPath(lastDashPos.add(0, user.getBbHeight() / 2, 0), userPos, hitboxSize);
        } else {
            targets = getTargetsInCustomHitbox(userPos, hitboxSize, hitboxSize * 1.5, hitboxSize);
        }

        for (LivingEntity target : targets) {
            if (!hitDuringDash.contains(target)) {
                hitTarget(target);
                hitDuringDash.add(target);
                dashTick = duration; // stop dash on hit
                user.setDeltaMovement(Vec3.ZERO); // cancel user movement on hit

                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.3f);
            }
        }
    }

    private List<LivingEntity> getTargetsAlongPath(Vec3 from, Vec3 to, float radius) {
        Vec3 dir = to.subtract(from);
        double dist = dir.length();
        if (dist < 0.01) return getTargetsInCustomHitbox(to, radius, radius * 1.5, radius);

        Set<LivingEntity> found = new HashSet<>();
        int steps = Math.max(1, (int) Math.ceil(dist / Math.max(radius * 0.25, 0.1)));
        for (int i = 0; i <= steps; i++) {
            Vec3 sample = from.add(dir.scale((double) i / steps));
            found.addAll(getTargetsInCustomHitbox(sample, radius, radius * 1.5, radius));
        }
        return new ArrayList<>(found);
    }

    private void executeFinisher() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

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
        dashStartPos = null;
        lastDashPos = null;
        dashTick = 0;
    }

    private Vec3 angledDashDirection() {
        Vec3 look = user.getLookAngle();
        return new Vec3(look.x, Math.max(-0.25, Math.min(0.25, look.y)), look.z).normalize();
    }
}
