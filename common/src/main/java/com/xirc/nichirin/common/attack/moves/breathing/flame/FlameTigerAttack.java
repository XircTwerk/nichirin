package com.xirc.nichirin.common.attack.moves.breathing.flame;

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
 * Fifth Form: Flame Tiger
 * The user dashes forth, bringing their blade into a high guard before performing
 * a series of sword slashes which take the form of a flaming tiger enveloping the user.
 *
 * Mechanics:
 * - Lots of strikes during 8 block dash
 * - User goes in a straight line
 * - Drags enemies with you
 * - High DPS multi-hit attack - NOW RESPECTS IMMUNITY FRAMES
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class FlameTigerAttack extends FlameBreathingAttackBase {

    private boolean dashStarted = false;
    private Vec3 dashDirection;
    private Vec3 startPosition;
    private final Set<LivingEntity> caughtEnemies = new HashSet<>();
    private final List<LivingEntity> draggedEnemies = new ArrayList<>();
    private int hitCounter = 0;

    public FlameTigerAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        dashStarted = false;
        caughtEnemies.clear();
        draggedEnemies.clear();
        hitCounter = 0;

        dashDirection = user.getLookAngle().normalize();
        startPosition = user.position();

        // Tiger roar sound at start
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 1.5f);

        // Initial flame tiger formation
        createTigerFormationEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Start dash after windup
        if (!dashStarted && tickCount == windup + 1) {
            startDash();
            dashStarted = true;
        }

        // Continue dash and attacks during duration
        if (dashStarted && tickCount > windup && tickCount < windup + duration) {
            continueDash();
            performMultipleSlashes();
        }
    }

    private void startDash() {
        // Set user velocity for dash - much more reasonable calculation
        float actualDashSpeed = dashSpeed != null ? dashSpeed : range;
        Vec3 dashVelocity = dashDirection.scale(actualDashSpeed / duration * 10);

        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        // Dash start sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.0f);
    }

    private void continueDash() {
        // Maintain dash velocity - same calculation as startDash
        if (dashSpeed != null) {
            float actualDashSpeed = dashSpeed;
            Vec3 dashVelocity = dashDirection.scale(actualDashSpeed / duration * 6);
            user.setDeltaMovement(dashVelocity);
            user.hurtMarked = true;
        }

        // Create continuous tiger effect during dash
        createTigerTrailEffect();

        // Catch new enemies in path and drag existing ones
        catchAndDragEnemies();
    }

    private void performMultipleSlashes() {
        // Perform slashes every few ticks for high DPS
        if (tickCount % 4 == 0) { // Slash every 4 ticks = 5 slashes per second
            executeSlash();
            hitCounter++;

            // Slash sound
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.2f + (hitCounter * 0.1f));
        }
    }

    private void executeSlash() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Hit enemies in front of the user along the dash direction (strip Y to keep hitbox level)
        Vec3 horizDir = dashDirection != null
                ? new Vec3(dashDirection.x, 0, dashDirection.z).normalize()
                : new Vec3(user.getLookAngle().x, 0, user.getLookAngle().z).normalize();
        Vec3 hitboxCenter = userPos.add(horizDir.scale(2.0));
        List<LivingEntity> nearbyTargets = getTargetsInCustomHitbox(
                hitboxCenter, 4.0, 3.0, 4.0);

        for (LivingEntity target : nearbyTargets) {
            // Hit each enemy - NOW RESPECTS IMMUNITY FRAMES
            hitTarget(target); // Changed from hitTargetNoImmunity to hitTarget

            // Create claw marks effect
            createClawMarksEffect(target.position());

            // Light knockback to keep enemies close but not push them away
            Vec3 lightKnockback = target.position().subtract(userPos).normalize().scale(knockback * 0.3);
            target.push(lightKnockback.x, 0.05, lightKnockback.z);
        }

        // Also hit dragged enemies
        for (LivingEntity draggedEnemy : draggedEnemies) {
            if (draggedEnemy.isAlive()) {
                hitTarget(draggedEnemy); // Changed from hitTargetNoImmunity to hitTarget
                createClawMarksEffect(draggedEnemy.position());
            }
        }
    }

    private void catchAndDragEnemies() {
        Vec3 userPos = user.position();

        // Find enemies in dash path
        List<LivingEntity> pathEnemies = getTargetsInLine(
                startPosition,
                userPos.add(dashDirection.scale(2)), // Look ahead
                2.0 // Thickness of dash path
        );

        for (LivingEntity enemy : pathEnemies) {
            if (!caughtEnemies.contains(enemy)) {
                // Catch new enemy
                caughtEnemies.add(enemy);
                draggedEnemies.add(enemy);

                // Tiger catch sound
                world.playSound(null, enemy.getX(), enemy.getY(), enemy.getZ(),
                        SoundEvents.RAVAGER_ATTACK, SoundSource.PLAYERS, 0.8f, 1.3f);

                // Visual effect when caught
                createCatchEffect(enemy.position());
            }
        }

        // Drag caught enemies along
        for (LivingEntity draggedEnemy : new ArrayList<>(draggedEnemies)) {
            if (draggedEnemy.isAlive()) {
                // Calculate drag position (slightly behind user)
                Vec3 dragPosition = userPos.subtract(dashDirection.scale(1.5));

                // Set enemy velocity to follow the user
                Vec3 dragVelocity = dragPosition.subtract(draggedEnemy.position()).scale(0.8);
                draggedEnemy.setDeltaMovement(dragVelocity);
                draggedEnemy.hurtMarked = true;
                draggedEnemy.hasImpulse = true;

                // Create drag trail effect
                createDragTrailEffect(draggedEnemy.position());
            } else {
                // Remove dead enemies from drag list
                draggedEnemies.remove(draggedEnemy);
            }
        }
    }

    private void createTigerFormationEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create tiger shape with soul fire flames
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * 2 * Math.PI;
            double tigerRadius = 2.0 + Math.sin(angle * 3) * 0.5; // Tiger body shape
            double height = 1.0 + Math.sin(angle * 2) * 0.3;

            Vec3 tigerPos = userPos.add(
                    Math.cos(angle) * tigerRadius,
                    height,
                    Math.sin(angle) * tigerRadius
            );

            serverLevel.sendParticles(ParticleTypes.FLAME,
                    tigerPos.x, tigerPos.y, tigerPos.z,
                    2, 0.1, 0.1, 0.1, 0.05);
        }

        // Tiger eyes effect (two bright points in front)
        Vec3 eyePos1 = userPos.add(dashDirection.scale(2.5)).add(0.3, 1, 0);
        Vec3 eyePos2 = userPos.add(dashDirection.scale(2.5)).add(-0.3, 1, 0);

        serverLevel.sendParticles(ParticleTypes.FLAME,
                eyePos1.x, eyePos1.y, eyePos1.z, 5, 0.05, 0.05, 0.05, 0.02);
        serverLevel.sendParticles(ParticleTypes.FLAME,
                eyePos2.x, eyePos2.y, eyePos2.z, 5, 0.05, 0.05, 0.05, 0.02);
    }

    private void createTigerTrailEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Tiger body trail following the user
        for (int i = 0; i < 8; i++) {
            Vec3 trailPos = userPos.subtract(dashDirection.scale(i * 0.5));

            // Main tiger body flames
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    trailPos.x, trailPos.y, trailPos.z,
                    3, 0.5, 0.3, 0.5, 0.1);

            // Tiger stripes (alternating soul fire)
            if (i % 2 == 0) {
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        trailPos.x + 0.3, trailPos.y, trailPos.z,
                        1, 0.1, 0.1, 0.1, 0.02);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        trailPos.x - 0.3, trailPos.y, trailPos.z,
                        1, 0.1, 0.1, 0.1, 0.02);
            }
        }

        // Tiger paws hitting the ground
        if (tickCount % 6 == 0) { // Every 6 ticks = paw steps
            Vec3 pawPos = userPos.add(0, -0.5, 0);
            serverLevel.sendParticles(ParticleTypes.LAVA,
                    pawPos.x, pawPos.y, pawPos.z,
                    3, 0.3, 0.1, 0.3, 0.05);
        }
    }

    private void createClawMarksEffect(Vec3 targetPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Create claw slash marks
        Vec3 clawDirection = user.getLookAngle().cross(new Vec3(0, 1, 0)).normalize();

        for (int claw = 0; claw < 3; claw++) { // 3 claw marks
            Vec3 clawStart = targetPos.add(clawDirection.scale((claw - 1) * 0.4));

            for (int i = 0; i < 5; i++) {
                Vec3 clawPos = clawStart.add(user.getLookAngle().scale(i * 0.2));

                serverLevel.sendParticles(ParticleTypes.FLAME,
                        clawPos.x, clawPos.y + 0.5, clawPos.z,
                        2, 0.1, 0.1, 0.1, 0.05);
            }
        }
    }

    private void createCatchEffect(Vec3 enemyPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Tiger jaw snapping effect
        serverLevel.sendParticles(ParticleTypes.FLAME,
                enemyPos.x, enemyPos.y + 1, enemyPos.z,
                10, 0.5, 0.5, 0.5, 0.2);

        // Flame burst when enemy is caught
        serverLevel.sendParticles(ParticleTypes.FLAME,
                enemyPos.x, enemyPos.y + 1, enemyPos.z,
                15, 0.8, 0.8, 0.8, 0.3);
    }

    private void createDragTrailEffect(Vec3 enemyPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Flame trail behind dragged enemies
        serverLevel.sendParticles(ParticleTypes.FLAME,
                enemyPos.x, enemyPos.y + 0.5, enemyPos.z,
                2, 0.3, 0.3, 0.3, 0.1);

        // Smoke from being dragged
        if (tickCount % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    enemyPos.x, enemyPos.y, enemyPos.z,
                    1, 0.2, 0.2, 0.2, 0.05);
        }
    }

    @Override
    public boolean isDashAttack() {
        return true; // This is a dash attack
    }

    @Override
    protected void onStop() {
        // Reset user velocity
        user.setDeltaMovement(Vec3.ZERO);

        // Release all dragged enemies with final tiger swipe
        for (LivingEntity draggedEnemy : draggedEnemies) {
            if (draggedEnemy.isAlive()) {

                // Strong knockback for finale
                Vec3 finalKnockback = dashDirection.scale(knockback * 2.0);
                draggedEnemy.push(finalKnockback.x, 0.5, finalKnockback.z);

                // Extended fire duration
                draggedEnemy.setSecondsOnFire(getFireDuration() + 5);

                // Final claw marks
                createClawMarksEffect(draggedEnemy.position());
            }
        }

        // Final tiger roar
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.5f, 1.2f);

        // Tiger dissipation effect
        createTigerDissipationEffect();

        // Clear all state
        dashStarted = false;
        caughtEnemies.clear();
        draggedEnemies.clear();
        hitCounter = 0;
    }

    private void createTigerDissipationEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Tiger dissolving into flames and smoke
        for (int i = 0; i < 50; i++) {
            double angle = (i / 50.0) * 2 * Math.PI;
            double radius = 3.0;
            double height = Math.random() * 3;

            Vec3 dissipatePos = userPos.add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius
            );

            // Flames dissipating upward
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    dissipatePos.x, dissipatePos.y, dissipatePos.z,
                    1, 0.2, 0.8, 0.2, 0.1);

            // Smoke clouds
            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        dissipatePos.x, dissipatePos.y + 1, dissipatePos.z,
                        2, 0.5, 0.5, 0.5, 0.05);
            }
        }
    }
}