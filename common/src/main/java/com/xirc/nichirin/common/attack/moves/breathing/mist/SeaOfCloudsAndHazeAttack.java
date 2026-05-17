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

// Form 5: 5-hop zigzag charge with large hitboxes. Drags enemies into a straight finisher.
public class SeaOfCloudsAndHazeAttack extends MistBreathingAttackBase {

    private static final int ZIGZAG_COUNT = 7;
    private static final int DASH_DURATION = 6;
    private static final int DASH_INTERVAL = 2;

    private int zigzagsExecuted = 0;
    private int nextZigzagTick = 0;
    private boolean finisherExecuted = false;
    private Vec3 baseDirection;
    private final Set<LivingEntity> caughtEnemies = new HashSet<>();
    private final List<LivingEntity> draggedEnemies = new ArrayList<>();
    private boolean wasInvulnerable = false;

    @Override
    protected void onStart() {
        zigzagsExecuted = 0;
        nextZigzagTick = 0;
        finisherExecuted = false;
        caughtEnemies.clear();
        draggedEnemies.clear();
        Vec3 look = user.getLookAngle();
        baseDirection = new Vec3(look.x, 0, look.z).normalize();
        wasInvulnerable = user.isInvulnerable();

        // Mist coil startup
        createMistParticles();
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.9f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute zigzag dashes
        if (zigzagsExecuted < ZIGZAG_COUNT && tickCount >= windup + nextZigzagTick) {
            executeZigzagDash();
            zigzagsExecuted++;
            nextZigzagTick += DASH_DURATION + DASH_INTERVAL;
        }

        // Execute finisher after all zigzags
        int finisherStartTick = windup + (ZIGZAG_COUNT * (DASH_DURATION + DASH_INTERVAL)) + 5;
        if (!finisherExecuted && zigzagsExecuted >= ZIGZAG_COUNT && tickCount >= finisherStartTick) {
            executeFinisher();
            finisherExecuted = true;
        }

        // Maintain drag effect between dashes
        if (zigzagsExecuted > 0 && !finisherExecuted) {
            continueDragEffect();
        }
    }

    private void executeZigzagDash() {
        // 7-hop pattern: alternating ±45° closing in to 0° for the final approach
        double[] angles = {45, -45, 35, -35, 20, -20, 0};
        double angle = zigzagsExecuted < angles.length ? angles[zigzagsExecuted] : 0;
        Vec3 zigzagDirection = rotateDirection(baseDirection, angle);

        // Larger dash speed than Centipede
        if (dashSpeed != null) {
            user.setDeltaMovement(zigzagDirection.scale(dashSpeed * 0.25));
            user.hurtMarked = true;
            user.hasImpulse = true;
        }

        user.setInvulnerable(true);

        catchAndDragEnemies();
        hitEnemiesAlongPath();
        createHopTrailEffect();

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.3f + zigzagsExecuted * 0.1f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private void catchAndDragEnemies() {
        Vec3 userPos = user.position();
        // Bigger hitbox than Centipede
        List<LivingEntity> pathEnemies = getTargetsInCustomHitbox(userPos, hitboxSize * 1.75, hitboxSize, hitboxSize * 1.75);

        for (LivingEntity enemy : pathEnemies) {
            if (!caughtEnemies.contains(enemy)) {
                caughtEnemies.add(enemy);
                draggedEnemies.add(enemy);

                world.playSound(null, enemy.getX(), enemy.getY(), enemy.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.6f, 1.4f);
                createMistHitParticles(enemy.position());
            }
        }
    }

    private void continueDragEffect() {
        Vec3 userPos = user.position();

        for (LivingEntity draggedEnemy : new ArrayList<>(draggedEnemies)) {
            if (draggedEnemy.isAlive()) {
                Vec3 dragTarget = userPos.subtract(baseDirection.scale(2.0));
                Vec3 toDrag = dragTarget.subtract(draggedEnemy.position());
                double dist = toDrag.length();
                if (dist > 0.3) {
                    draggedEnemy.setDeltaMovement(toDrag.normalize().scale(Math.min(dist * 0.8, 2.5)));
                    draggedEnemy.hurtMarked = true;
                }
                createWaterTrailParticles(draggedEnemy.position());
            } else {
                draggedEnemies.remove(draggedEnemy);
            }
        }
    }

    private void hitEnemiesAlongPath() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        // Bigger hitbox than Centipede
        List<LivingEntity> targets = getTargetsInCustomHitbox(userPos, hitboxSize * 1.5, hitboxSize, hitboxSize * 1.5);

        for (LivingEntity target : targets) {
            float originalDamage = damage;
            damage = damage * 0.45f; // 45% per zigzag
            hitTarget(target);
            damage = originalDamage;

            Vec3 lightKnockback = target.position().subtract(userPos).normalize().scale(knockback * 0.12f);
            target.push(lightKnockback.x, 0.04, lightKnockback.z);
        }

        for (LivingEntity draggedEnemy : draggedEnemies) {
            if (draggedEnemy.isAlive()) {
                float originalDamage = damage;
                damage = damage * 0.45f;
                hitTarget(draggedEnemy);
                damage = originalDamage;
            }
        }
    }

    private void executeFinisher() {
        // Straight final dash
        if (dashSpeed != null) {
            user.setDeltaMovement(baseDirection.scale(dashSpeed));
            user.hurtMarked = true;
            user.hasImpulse = true;
        }

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        // Large finisher hitbox
        List<LivingEntity> finisherTargets = getTargetsInCustomHitbox(userPos, hitboxSize * 1.5f, 2.5, hitboxSize * 1.5f);

        for (LivingEntity target : finisherTargets) {
            hitTarget(target);
            Vec3 finisherKnockback = target.position().subtract(user.position()).normalize();
            target.push(finisherKnockback.x * knockback * 0.7f, 0.35, finisherKnockback.z * knockback * 0.7f);
            createMistHitParticles(target.position());
        }

        for (LivingEntity draggedEnemy : draggedEnemies) {
            if (draggedEnemy.isAlive()) {
                float originalDamage = damage;
                damage = damage * 1.5f;
                hitTarget(draggedEnemy);
                damage = originalDamage;

                Vec3 finalKnockback = baseDirection.scale(knockback * 1.2f);
                draggedEnemy.push(finalKnockback.x, 0.4, finalKnockback.z);
                createMistHitParticles(draggedEnemy.position());
            }
        }

        createMistCircle(userPos, range * 0.4f, 28);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.4f, 0.9f);
    }

    private void createHopTrailEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        for (int i = 1; i <= 6; i++) {
            Vec3 trailPos = userPos.subtract(baseDirection.scale(i * 0.5));
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    trailPos.x, trailPos.y, trailPos.z, 3, 0.3, 0.3, 0.3, 0.03);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                    trailPos.x, trailPos.y, trailPos.z, 2, 0.2, 0.2, 0.2, 0.02);
        }

        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                userPos.x, userPos.y, userPos.z, 6, 0.5, 0.5, 0.5, 0.1);
    }

    @Override
    protected void onStop() {
        user.setInvulnerable(wasInvulnerable);
        user.setDeltaMovement(Vec3.ZERO);

        // Final mist burst
        createMistCircle(user.position().add(0, 1, 0), range * 0.5f, 32);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 1.1f);

        zigzagsExecuted = 0;
        nextZigzagTick = 0;
        finisherExecuted = false;
        caughtEnemies.clear();
        draggedEnemies.clear();
    }
}
