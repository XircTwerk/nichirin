package com.xirc.nichirin.common.attack.moves.breathing.mist;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Form 5: 5-hop zigzag charge with large hitboxes. Drags enemies into a straight finisher.
public class SeaOfCloudsAndHazeAttack extends MistBreathingAttackBase {

    private static final int   ZIGZAG_COUNT    = 7;
    private static final int   DASH_DURATION   = 6;
    private static final int   DASH_INTERVAL   = 5;  // ticks between hops (was 2, now more watchable)
    private static final float DASH_DIST_FACTOR = 0.55f; // dashSpeed * this = blocks per hop

    private int     zigzagsExecuted = 0;
    private int     nextZigzagTick  = 0;
    private boolean finisherExecuted = false;
    private Vec3    baseDirection;

    // Smooth lerp state for current hop
    private Vec3 dashStartPos = null;
    private Vec3 dashEndPos   = null;
    private int  dashStartTick = 0;

    private final Set<LivingEntity>  caughtEnemies  = new HashSet<>();
    private final List<LivingEntity> draggedEnemies = new ArrayList<>();
    private boolean wasInvulnerable = false;
    private final List<UUID> spawnedClones = new ArrayList<>();

    @Override
    protected void onStart() {
        zigzagsExecuted  = 0;
        nextZigzagTick   = 0;
        finisherExecuted = false;
        dashStartPos     = null;
        dashEndPos       = null;
        caughtEnemies.clear();
        draggedEnemies.clear();
        spawnedClones.clear();

        Vec3 look = user.getLookAngle();
        baseDirection = new Vec3(look.x, 0, look.z).normalize();
        wasInvulnerable = user.isInvulnerable();

        createMistParticles();
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.9f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // ── 1. Trigger next zigzag hop ──────────────────────────────────────
        if (zigzagsExecuted < ZIGZAG_COUNT && tickCount >= windup + nextZigzagTick) {
            setupZigzagDash();
            zigzagsExecuted++;
            nextZigzagTick += DASH_DURATION + DASH_INTERVAL;
        }

        // ── 2. Lerp position across DASH_DURATION ticks (smooth, grounded) ──
        if (dashStartPos != null && dashEndPos != null) {
            int tickInDash = tickCount - dashStartTick;
            if (tickInDash >= 0 && tickInDash <= DASH_DURATION) {
                double t = (double) tickInDash / DASH_DURATION;
                double s = t * t * (3.0 - 2.0 * t); // smoothstep
                double lerpX = dashStartPos.x + (dashEndPos.x - dashStartPos.x) * s;
                double lerpZ = dashStartPos.z + (dashEndPos.z - dashStartPos.z) * s;
                // Always use current Y — never lift the player off the ground
                double y = user.getY();
                if (user instanceof ServerPlayer sp) {
                    sp.teleportTo(lerpX, y, lerpZ);
                } else {
                    user.absMoveTo(lerpX, y, lerpZ, user.getYRot(), user.getXRot());
                }
                user.setDeltaMovement(Vec3.ZERO);
            }
        }

        // ── 3. Finisher ─────────────────────────────────────────────────────
        int finisherStartTick = windup + (ZIGZAG_COUNT * (DASH_DURATION + DASH_INTERVAL)) + 5;
        if (!finisherExecuted && zigzagsExecuted >= ZIGZAG_COUNT && tickCount >= finisherStartTick) {
            executeFinisher();
            finisherExecuted = true;
        }

        // ── 4. Drag between hops ─────────────────────────────────────────────
        if (zigzagsExecuted > 0 && !finisherExecuted) {
            continueDragEffect();
        }
    }

    /** Sets up a single zigzag hop: stores lerp start/end, snap camera, queue hits. */
    private void setupZigzagDash() {
        double[] angles = {45, -45, 35, -35, 20, -20, 0};
        double angle = zigzagsExecuted < angles.length ? angles[zigzagsExecuted] : 0;
        Vec3 dir = rotateDirection(baseDirection, angle);

        float dist = dashSpeed != null ? dashSpeed * DASH_DIST_FACTOR : 5.0f;
        dashStartPos  = user.position();
        dashEndPos    = new Vec3(dashStartPos.x + dir.x * dist, dashStartPos.y, dashStartPos.z + dir.z * dist);
        dashStartTick = tickCount;

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
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 4, 0);

        for (LivingEntity draggedEnemy : new ArrayList<>(draggedEnemies)) {
            if (draggedEnemy.isAlive()) {
                Vec3 toDrag = userPos.subtract(draggedEnemy.position());
                double dist = toDrag.length();
                if (dist > 0.5) {
                    draggedEnemy.setDeltaMovement(toDrag.normalize().scale(Math.min(dist, 3.5)));
                    draggedEnemy.hurtMarked = true;
                } else {
                    draggedEnemy.setDeltaMovement(
                            user.getDeltaMovement().x,
                            draggedEnemy.getDeltaMovement().y,
                            user.getDeltaMovement().z
                    );
                }
                createWaterTrailParticles(draggedEnemy.position());
            } else {
                draggedEnemies.remove(draggedEnemy);
            }
        }
    }

    private void hitEnemiesAlongPath() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        List<LivingEntity> targets = getTargetsInCustomHitbox(userPos, hitboxSize * 1.5, hitboxSize, hitboxSize * 1.5);

        for (LivingEntity target : targets) {
            float originalDamage = damage;
            damage = damage * 0.45f;
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
        // Short forward burst to sell the final slash
        if (dashSpeed != null) {
            user.setDeltaMovement(baseDirection.scale(dashSpeed * 0.4));
            user.hurtMarked = true;
            user.hasImpulse = true;
        }

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
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

        float circleRadius = range * 0.4f;
        Vec3 circleCenter  = userPos;
        createMistCircle(circleCenter, circleRadius, 28);

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

        createMistCircle(user.position().add(0, 1, 0), range * 0.5f, 32);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 1.1f);

        if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (UUID id : spawnedClones) {
                net.minecraft.world.entity.Entity e = serverLevel.getEntity(id);
                if (e != null) e.discard();
            }
        }

        zigzagsExecuted  = 0;
        nextZigzagTick   = 0;
        finisherExecuted = false;
        dashStartPos     = null;
        dashEndPos       = null;
        caughtEnemies.clear();
        draggedEnemies.clear();
        spawnedClones.clear();
    }
}
