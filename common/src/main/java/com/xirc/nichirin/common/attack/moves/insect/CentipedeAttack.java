package com.xirc.nichirin.common.attack.moves.insect;

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
 * Fourth Form: Dance of the Centipede – Hundred-Legged Zigzag
 * 22.5° -> -22.5° -> 22.5° -> 0° dash pattern with enemy dragging and massive finisher.
 */
public class CentipedeAttack extends InsectBreathingAttackBase {

    private static final int ZIGZAG_COUNT = 3;
    private static final int DASH_DURATION = 12;
    private static final int DASH_INTERVAL = 3;

    private int zigzagsExecuted = 0;
    private int nextZigzagTick = 0;
    private boolean finisherExecuted = false;
    private Vec3 baseDirection; // Forward direction when attack starts
    private final Set<LivingEntity> caughtEnemies = new HashSet<>();
    private final List<LivingEntity> draggedEnemies = new ArrayList<>();

    // Invulnerability tracking
    private boolean wasInvulnerable = false;

    public CentipedeAttack() {
        // Configuration comes from moveset
    }

    @Override
    protected void onStart() {
        zigzagsExecuted = 0;
        nextZigzagTick = 0;
        finisherExecuted = false;
        caughtEnemies.clear();
        draggedEnemies.clear();

        // Set base direction from user's facing when attack starts
        baseDirection = user.getLookAngle().normalize();

        // Store invulnerability state
        wasInvulnerable = user.isInvulnerable();

        // Centipede startup sounds
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.8f);

        // Create coiling effect
        createCentipedeCoilEffect();
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
        if (!finisherExecuted && zigzagsExecuted >= ZIGZAG_COUNT &&
                tickCount >= windup + (ZIGZAG_COUNT * (DASH_DURATION + DASH_INTERVAL)) + 5) {
            executeFinisher();
            finisherExecuted = true;
        }

        // During dashes, maintain drag effect
        if (zigzagsExecuted > 0 && !finisherExecuted) {
            continueDragEffect();
        }
    }

    private void executeZigzagDash() {
        Vec3 zigzagDirection;

        // Calculate zigzag direction: 22.5° -> -22.5° -> 22.5° -> 0° (finisher)
        if (zigzagsExecuted == 0) {
            // First dash: +22.5° from forward
            zigzagDirection = rotateDirection(baseDirection, 22.5);
        } else if (zigzagsExecuted == 1) {
            // Second dash: -22.5° from forward
            zigzagDirection = rotateDirection(baseDirection, -22.5);
        } else {
            // Third dash: +22.5° from forward
            zigzagDirection = rotateDirection(baseDirection, 22.5);
        }

        // Fast dash with invincibility
        Vec3 dashVelocity = zigzagDirection.scale(dashSpeed * 2.0); // Very fast
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        // Grant invincibility during dash
        user.setInvulnerable(true);

        // Catch and drag enemies (like Flame Tiger)
        catchAndDragEnemies();

        // Hit enemies along path
        hitEnemiesAlongPath();

        // Zigzag effects
        createZigzagTrail(zigzagsExecuted);

        // Sounds
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.5f + zigzagsExecuted * 0.3f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.SPIDER_STEP, SoundSource.PLAYERS, 1.0f, 1.2f);
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

    private void catchAndDragEnemies() {
        Vec3 userPos = user.position();

        // Large hitbox to catch enemies (like Flame Tiger)
        List<LivingEntity> pathEnemies = getTargetsInCustomHitbox(
                userPos, 6.0, 4.0, 6.0); // Big hitbox

        for (LivingEntity enemy : pathEnemies) {
            if (!caughtEnemies.contains(enemy)) {
                caughtEnemies.add(enemy);
                draggedEnemies.add(enemy);

                // Catch sound and effect
                world.playSound(null, enemy.getX(), enemy.getY(), enemy.getZ(),
                        SoundEvents.SPIDER_HURT, SoundSource.PLAYERS, 0.8f, 1.5f);
                createCatchEffect(enemy.position());
            }
        }
    }

    private void continueDragEffect() {
        Vec3 userPos = user.position();

        // Drag caught enemies (like Flame Tiger)
        for (LivingEntity draggedEnemy : new ArrayList<>(draggedEnemies)) {
            if (draggedEnemy.isAlive()) {
                // Pull enemy toward user
                Vec3 dragPosition = userPos.subtract(baseDirection.scale(2.0));
                Vec3 dragVelocity = dragPosition.subtract(draggedEnemy.position()).scale(0.9);

                draggedEnemy.setDeltaMovement(dragVelocity);
                draggedEnemy.hurtMarked = true;
                draggedEnemy.hasImpulse = true;

                // Drag trail effect
                createDragTrailEffect(draggedEnemy.position());
            } else {
                draggedEnemies.remove(draggedEnemy);
            }
        }
    }

    private void hitEnemiesAlongPath() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Hit enemies in large area during zigzag
        List<LivingEntity> targets = getTargetsInCustomHitbox(userPos, 5.0, 3.0, 5.0);

        for (LivingEntity target : targets) {
            // Reduced damage per zigzag hit (like Flame Tiger)
            float originalDamage = damage;
            damage = damage * 0.4f; // 40% damage per zigzag
            hitTarget(target);
            damage = originalDamage;

            // Light knockback to keep enemies close
            Vec3 lightKnockback = target.position().subtract(userPos).normalize().scale(knockback * 0.3);
            target.push(lightKnockback.x, 0.1, lightKnockback.z);

            createZigzagImpactEffect(target.position());
        }

        // Also hit dragged enemies
        for (LivingEntity draggedEnemy : draggedEnemies) {
            if (draggedEnemy.isAlive()) {
                float originalDamage = damage;
                damage = damage * 0.4f;
                hitTarget(draggedEnemy);
                damage = originalDamage;
                createZigzagImpactEffect(draggedEnemy.position());
            }
        }
    }

    private void executeFinisher() {
        // Massive forward dash for finisher (0° - straight forward)
        Vec3 finisherVelocity = baseDirection.scale(dashSpeed * 2.5);
        user.setDeltaMovement(finisherVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        // Hit all enemies in huge finisher area
        List<LivingEntity> finisherTargets = getTargetsInCustomHitbox(
                user.position().add(0, user.getBbHeight() / 2, 0),
                hitboxSize * 2.5, 4.0, hitboxSize * 2.5); // Massive hitbox

        for (LivingEntity target : finisherTargets) {
            // Full finisher damage
            hitTarget(target);

            // Strong knockback
            Vec3 finisherKnockback = target.position().subtract(user.position()).normalize();
            target.push(finisherKnockback.x * knockback * 1.5, 0.8, finisherKnockback.z * knockback * 1.5);

            createFinisherImpactEffect(target.position());
        }

        // Release dragged enemies with massive damage
        for (LivingEntity draggedEnemy : draggedEnemies) {
            if (draggedEnemy.isAlive()) {
                // Extra finisher damage for dragged enemies
                float originalDamage = damage;
                damage = damage * 1.5f; // 150% damage
                hitTarget(draggedEnemy);
                damage = originalDamage;

                // Massive knockback
                Vec3 finalKnockback = baseDirection.scale(knockback * 3.0);
                draggedEnemy.push(finalKnockback.x, 1.0, finalKnockback.z);

                createFinisherImpactEffect(draggedEnemy.position());
            }
        }

        // Massive venom burst
        createVenomBurst();

        // Finisher sounds
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.5f, 0.8f);
        playPoisonSound(user.position());
    }

    // Visual effects
    private void createCentipedeCoilEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        for (int i = 0; i < 25; i++) {
            double progress = i / 25.0;
            double angle = progress * 6 * Math.PI; // Multiple rotations
            double radius = 2.0 * (1.0 - progress); // Spiral inward
            double height = progress * 2.5;

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + height;

            serverLevel.sendParticles(ParticleTypes.WITCH, x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.PORTAL, x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }
    }

    private void createZigzagTrail(int zigzagNumber) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        for (int i = 1; i <= 6; i++) {
            Vec3 trailPos = userPos.subtract(baseDirection.scale(i * 0.4));

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    trailPos.x, trailPos.y, trailPos.z, 3, 0.2, 0.2, 0.2, 0.1);
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    trailPos.x, trailPos.y, trailPos.z, 2, 0.15, 0.15, 0.15, 0.08);
        }

        // Invulnerability sparkles
        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                userPos.x, userPos.y, userPos.z, 8, 0.5, 0.5, 0.5, 0.1);
    }

    private void createCatchEffect(Vec3 enemyPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        serverLevel.sendParticles(ParticleTypes.WITCH,
                enemyPos.x, enemyPos.y + 1, enemyPos.z, 10, 0.5, 0.5, 0.5, 0.2);
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                enemyPos.x, enemyPos.y + 1, enemyPos.z, 8, 0.4, 0.4, 0.4, 0.15);
    }

    private void createDragTrailEffect(Vec3 enemyPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        serverLevel.sendParticles(ParticleTypes.WITCH,
                enemyPos.x, enemyPos.y + 0.5, enemyPos.z, 2, 0.3, 0.3, 0.3, 0.1);
        if (tickCount % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    enemyPos.x, enemyPos.y, enemyPos.z, 1, 0.2, 0.2, 0.2, 0.05);
        }
    }

    private void createZigzagImpactEffect(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = impactPos.add(0, 1, 0);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                targetPos.x, targetPos.y, targetPos.z, 5, 0.2, 0.2, 0.2, 0.1);
        serverLevel.sendParticles(ParticleTypes.WITCH,
                targetPos.x, targetPos.y, targetPos.z, 8, 0.3, 0.3, 0.3, 0.12);
    }

    private void createFinisherImpactEffect(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = impactPos.add(0, 1, 0);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                targetPos.x, targetPos.y, targetPos.z, 20, 0.6, 0.6, 0.6, 0.3);
        createPoisonBurst(targetPos, 2.0f);
        serverLevel.sendParticles(ParticleTypes.WITCH,
                targetPos.x, targetPos.y, targetPos.z, 25, 0.8, 0.8, 0.8, 0.4);
    }

    private void createVenomBurst() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        serverLevel.sendParticles(ParticleTypes.WITCH,
                userPos.x, userPos.y, userPos.z, 60, 2.5, 2.5, 2.5, 0.5);
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                userPos.x, userPos.y, userPos.z, 40, 2.0, 2.0, 2.0, 0.4);

        // Centipede dissipation effect
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * 2 * Math.PI;
            double radius = 5.0;
            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y;

            serverLevel.sendParticles(ParticleTypes.WITCH, x, y, z, 8, 0.6, 1.2, 0.6, 0.3);
        }
    }

    @Override
    public boolean hasInvincibilityFrames() {
        return true;
    }

    @Override
    public boolean isDashAttack() {
        return true;
    }

    @Override
    protected void onStop() {
        // Restore invulnerability
        user.setInvulnerable(wasInvulnerable);
        user.setDeltaMovement(Vec3.ZERO);

        // Final centipede dissolution
        createVenomBurst();

        // Final sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.SPIDER_DEATH, SoundSource.PLAYERS, 1.0f, 1.2f);

        // Clear state
        zigzagsExecuted = 0;
        nextZigzagTick = 0;
        finisherExecuted = false;
        caughtEnemies.clear();
        draggedEnemies.clear();
    }
}