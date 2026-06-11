package com.xirc.nichirin.common.attack.moves.breathing.water;

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

/**
 * Ninth Form: Splashing Water Flow
 * Segmented dash ending in a quick slash
 * About 10 block range with particles appearing at user's feet every time they "jump"
 * Short repeated jumps in a zigzag pattern (like Centipede Attack)
 */
public class SplashingWaterFlowAttack extends WaterBreathingAttackBase {

    private static final int JUMP_COUNT = 8; // 5 jumps across 10 blocks
    private static final int JUMP_DURATION = 3; // Ticks per jump
    private static final int JUMP_INTERVAL = 4; // Ticks between jumps

    private int jumpsExecuted = 0;
    private int nextJumpTick = 0;
    private boolean finalSlashExecuted = false;
    private Vec3 baseDirection; // Forward direction when attack starts
    private List<Vec3> jumpPositions = new ArrayList<>();
    private final Set<LivingEntity> caughtEnemies = new HashSet<>();
    private final List<LivingEntity> draggedEnemies = new ArrayList<>();

    // Invulnerability tracking
    private boolean wasInvulnerable = false;

    public SplashingWaterFlowAttack() {
    }

    @Override
    protected void onStart() {
        jumpsExecuted = 0;
        nextJumpTick = 0;
        finalSlashExecuted = false;
        jumpPositions.clear();
        caughtEnemies.clear();
        draggedEnemies.clear();

        // Force horizontal direction only (ignore Y component)
        Vec3 rawDirection = user.getLookAngle();
        baseDirection = new Vec3(rawDirection.x, 0, rawDirection.z).normalize();


        // Store invulnerability state
        wasInvulnerable = user.isInvulnerable();
        user.setInvulnerable(true);
    }

    @Override
    protected void onActiveStart() {
        // Splashing water flow startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 1.0f, 1.3f);

        // Create initial water flow effect
        createFlowStartEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute zigzag jumps
        if (jumpsExecuted < JUMP_COUNT && tickCount >= windup + nextJumpTick) {
            executeZigzagJump();
            jumpsExecuted++;
            nextJumpTick += JUMP_DURATION + JUMP_INTERVAL;
        }

        // Drag caught enemies every tick
        if (jumpsExecuted > 0 && !finalSlashExecuted) {
            continueDragEffect();
        }

        // Execute final slash after all jumps
        if (!finalSlashExecuted && jumpsExecuted >= JUMP_COUNT &&
                tickCount >= windup + (JUMP_COUNT * (JUMP_DURATION + JUMP_INTERVAL)) + 5) {
            executeFinalSlash();
            finalSlashExecuted = true;
        }
    }

    private void executeZigzagJump() {
        Vec3 zigzagDirection;

        // Calculate zigzag direction: alternating left-right pattern
        if (jumpsExecuted % 2 == 0) {
            // Even jumps: slight left offset
            zigzagDirection = rotateDirection(baseDirection, -15);
        } else {
            // Odd jumps: slight right offset
            zigzagDirection = rotateDirection(baseDirection, 15);
        }

        // Calculate jump distance (total 10 blocks across 5 jumps = 2 blocks per jump)
        float jumpDistance = range / JUMP_COUNT;
        Vec3 jumpVelocity = zigzagDirection.scale(dashSpeed * 0.4).add(0, 0, 0); // Upward arc

        user.setDeltaMovement(jumpVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        // Record jump position for effects
        jumpPositions.add(user.position());

        // Hit enemies along jump path
        hitEnemiesAlongJump();

        // Jump effects
        createJumpEffect();

        // Jump sounds with increasing pitch
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS,
                0.8f, 1.2f + jumpsExecuted * 0.2f);
    }

    private Vec3 rotateDirection(Vec3 direction, double degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        // Rotate around Y axis (horizontal rotation)
        double newX = direction.x * cos - direction.z * sin;
        double newZ = direction.x * sin + direction.z * cos;

        return new Vec3(newX, direction.y, newZ).normalize();
    }

    private void hitEnemiesAlongJump() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Hit enemies in jump area
        List<LivingEntity> jumpTargets = getTargetsInCustomHitbox(
                userPos, 2.5, 3.0, 2.5);

        for (LivingEntity target : jumpTargets) {
            // Light damage per jump
            float originalDamage = damage;
            damage = damage * 0.3f; // 30% damage per jump
            hitTarget(target);
            damage = originalDamage;

            // Catch enemies for dragging
            if (!caughtEnemies.contains(target)) {
                caughtEnemies.add(target);
                draggedEnemies.add(target);
            }

            createJumpImpactEffect(target.position());
        }
    }

    private void continueDragEffect() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 4, 0);
        for (LivingEntity draggedEnemy : new ArrayList<>(draggedEnemies)) {
            if (draggedEnemy.isAlive()) {
                Vec3 toDrag = userPos.subtract(draggedEnemy.position());
                double dist = toDrag.length();
                if (dist > 0.5) {
                    // Scale velocity so enemies keep up with even fast dashes
                    draggedEnemy.setDeltaMovement(toDrag.normalize().scale(Math.min(dist, 3.5)));
                    draggedEnemy.hurtMarked = true;
                } else {
                    // Close enough — zero out so they don't oscillate
                    draggedEnemy.setDeltaMovement(
                            user.getDeltaMovement().x,
                            draggedEnemy.getDeltaMovement().y,
                            user.getDeltaMovement().z
                    );
                }
            } else {
                draggedEnemies.remove(draggedEnemy);
            }
        }
    }

    private void executeFinalSlash() {
        // Massive final slash in the flow direction
        Vec3 slashVelocity = baseDirection.scale(dashSpeed * 0.6);
        user.setDeltaMovement(slashVelocity);
        user.hurtMarked = true;

        // Hit all enemies in final slash area
        List<LivingEntity> slashTargets = getTargetsInCustomHitbox(
                user.position().add(0, user.getBbHeight() / 2, 0),
                hitboxSize, 3.0, hitboxSize);

        for (LivingEntity target : slashTargets) {
            // Full finisher damage
            hitTarget(target);

            // Strong knockback
            Vec3 slashKnockback = baseDirection.scale(knockback);
            target.push(slashKnockback.x, 0.4, slashKnockback.z);

            createFinalSlashImpactEffect(target.position());
        }

        // Create massive final water slash
        createFinalWaterSlash();

        // Final slash sounds
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.8f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.2f, 1.0f);
    }

    private void createFlowStartEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Initial water gathering for flow
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            double radius = 2.0;
            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + Math.sin(angle * 3) * 0.4;

            // Flow preparation particles
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    x, y, z, 2, 0.2, 0.2, 0.2, 0.1);

            if (i % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }

        // Direction preview
        for (int i = 1; i <= 3; i++) {
            Vec3 previewPos = userPos.add(baseDirection.scale(i * 1.5));
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    previewPos.x, previewPos.y, previewPos.z,
                    3, 0.2, 0.2, 0.2, 0.1);
        }
    }

    private void createJumpEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 jumpPos = user.position();

        // Splash at feet when jumping
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                jumpPos.x, jumpPos.y, jumpPos.z,
                12, 0.6, 0.2, 0.6, 0.2);

        // Water spray around jump
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * 2 * Math.PI;
            double radius = 1.5;

            double x = jumpPos.x + Math.cos(angle) * radius;
            double z = jumpPos.z + Math.sin(angle) * radius;
            double y = jumpPos.y + 0.5;

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    x, y, z, 2, 0.3, 0.3, 0.3, 0.15);
        }

        // Trailing water behind jump
        if (jumpsExecuted > 0 && !jumpPositions.isEmpty()) {
            Vec3 lastJumpPos = jumpPositions.get(jumpPositions.size() - 1);
            createWaterTrail(lastJumpPos, jumpPos);
        }

        // Zigzag trail effect
        createZigzagTrail();
    }

    private void createZigzagTrail() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create trail showing the zigzag pattern
        for (int i = 1; i <= 4; i++) {
            Vec3 trailPos = userPos.subtract(baseDirection.scale(i * 0.5));

            // Add slight zigzag offset to trail
            Vec3 rightDir = baseDirection.cross(new Vec3(0, 1, 0)).normalize();
            double zigzagOffset = Math.sin(i * 0.8) * 0.3;
            trailPos = trailPos.add(rightDir.scale(zigzagOffset));

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    trailPos.x, trailPos.y, trailPos.z,
                    3, 0.2, 0.2, 0.2, 0.1);

            // Dripping water for flow effect
            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                    trailPos.x, trailPos.y + 0.5, trailPos.z,
                    1, 0.1, 0, 0.1, 0.05);
        }
    }

    private void createJumpImpactEffect(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = impactPos.add(0, 1, 0);

        // Jump impact particles
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                targetPos.x, targetPos.y, targetPos.z,
                8, 0.4, 0.4, 0.4, 0.2);

        serverLevel.sendParticles(ParticleTypes.CRIT,
                targetPos.x, targetPos.y, targetPos.z,
                4, 0.2, 0.2, 0.2, 0.1);
    }

    private void createFinalWaterSlash() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Massive horizontal water slash
        for (int i = -8; i <= 8; i++) {
            double angle = i * 10; // 10-degree increments for wide slash
            double radians = Math.toRadians(angle);

            Vec3 slashDir = lookDir.yRot((float)radians);
            Vec3 slashPos = userPos.add(slashDir.scale(range * 0.9));

            // Final slash particles
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    slashPos.x, slashPos.y, slashPos.z,
                    6, 0.4, 0.4, 0.4, 0.2);

            serverLevel.sendParticles(ParticleTypes.CRIT,
                    slashPos.x, slashPos.y, slashPos.z,
                    3, 0.2, 0.2, 0.2, 0.1);
        }

        // Central explosion
        createWaterExplosion(userPos.add(lookDir.scale(range * 0.7)), 2.0f);
    }

    private void createFinalSlashImpactEffect(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = impactPos.add(0, 1, 0);

        // Final impact particles
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                targetPos.x, targetPos.y, targetPos.z,
                20, 0.8, 0.8, 0.8, 0.4);

        serverLevel.sendParticles(ParticleTypes.CRIT,
                targetPos.x, targetPos.y, targetPos.z,
                15, 0.6, 0.6, 0.6, 0.3);

        // Water burst around impact
        createWaterCircle(targetPos, 2.0f, 12);
    }

    @Override
    public boolean isDashAttack() {
        return true; // This is a dash attack with jumps
    }

    @Override
    protected void onStop() {
        // Restore invulnerability
        user.setInvulnerable(wasInvulnerable);
        user.setDeltaMovement(Vec3.ZERO);

        // Final flow dissolution
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            // Final water burst
            createWaterExplosion(userPos, 2.5f);

            // Trail dissolution effect
            for (Vec3 jumpPos : jumpPositions) {
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        jumpPos.x, jumpPos.y + 0.5, jumpPos.z,
                        8, 0.5, 0.5, 0.5, 0.2);
            }
        }

        // Final flow sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0f, 1.2f);

        // Clear state
        jumpsExecuted = 0;
        nextJumpTick = 0;
        finalSlashExecuted = false;
        jumpPositions.clear();
        caughtEnemies.clear();
        draggedEnemies.clear();
    }
}