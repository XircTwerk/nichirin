package com.xirc.nichirin.common.attack.moves.water;

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
 * Second Form: Water Wheel (Wheel Version)
 * The user jumps forward and creates a vertical wheel around them
 * Multi-hit lingering attack that lunges forward slightly
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class WaterWheelAttack extends WaterBreathingAttackBase {

    private boolean wheelStarted = false;
    private Set<LivingEntity> hitEntities = new HashSet<>();
    private int wheelTicks = 0;

    public WaterWheelAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        wheelStarted = false;
        hitEntities.clear();
        wheelTicks = 0;

        // Water wheel startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.8f);

        // Initial water particles
        createWaterParticles();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Start wheel after windup
        if (!wheelStarted && tickCount == windup + 1) {
            startWheel();
            wheelStarted = true;
        }

        // Continue wheel rotation during duration
        if (wheelStarted && tickCount > windup && tickCount < windup + duration) {
            wheelTicks++;
            performWheel();
        }
    }

    private void startWheel() {
        // Small forward lunge using dash speed
        if (dashSpeed != null && dashSpeed > 0) {
            Vec3 lungeDirection = user.getLookAngle().normalize();
            Vec3 lungeVelocity = lungeDirection.scale(dashSpeed * 0.5); // Half dash speed for lunge
            user.setDeltaMovement(lungeVelocity.x, 0.3, lungeVelocity.z); // Small upward component
            user.hurtMarked = true;
            user.hasImpulse = true;
        }

        // Wheel start sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.9f);

        // Create initial wheel effect
        createWheelStartEffect();
    }

    private void performWheel() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create continuous vertical wheel effect
        createVerticalWheelEffect(userPos);

        // Hit enemies in wheel radius - allow multiple hits but with timing
        if (wheelTicks % 6 == 0) { // Hit every 6 ticks (0.3 seconds)
            List<LivingEntity> targets = getTargetsInCustomHitbox(
                    userPos,
                    hitboxSize, // width
                    hitboxSize + 1, // height (taller for vertical wheel)
                    hitboxSize  // depth
            );

            for (LivingEntity target : targets) {
                // Use no immunity for multi-hit but track recently hit entities
                if (!hasRecentlyHit(target)) {
                    hitTargetNoImmunity(target);
                    trackRecentHit(target);

                    // Light upward and outward knockback to keep enemies in wheel
                    Vec3 wheelKnockback = target.position().subtract(userPos).normalize();
                    target.push(wheelKnockback.x * knockback * 0.3, 0.1, wheelKnockback.z * knockback * 0.3);

                    // Individual hit sound
                    world.playSound(null, target.getX(), target.getY(), target.getZ(),
                            SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.6f, 1.2f + wheelTicks * 0.05f);
                }
            }
        }

        // Wheel rotation sound every few ticks
        if (wheelTicks % 10 == 0) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.0f + wheelTicks * 0.02f);
        }
    }

    private boolean hasRecentlyHit(LivingEntity target) {
        // Allow hitting the same target again after 12 ticks (0.6 seconds)
        return hitEntities.contains(target) && wheelTicks % 12 > 6;
    }

    private void trackRecentHit(LivingEntity target) {
        hitEntities.add(target);
        // Clear old hits periodically to allow re-hitting
        if (wheelTicks % 24 == 0) {
            hitEntities.clear();
        }
    }

    private void createWheelStartEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Initial wheel formation - vertical circle
        for (int i = 0; i < 16; i++) {
            double angle = (i / 16.0) * 2 * Math.PI;
            double radius = hitboxSize * 0.8;

            // Vertical wheel - rotate around Z axis (creates vertical wheel in XY plane)
            double x = userPos.x + Math.cos(angle) * radius;
            double y = userPos.y + Math.sin(angle) * radius;
            double z = userPos.z;

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    x, y, z, 3, 0.2, 0.2, 0.2, 0.1);

            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                    x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
        }

        // Water burst at center
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                userPos.x, userPos.y, userPos.z,
                20, 0.5, 0.5, 0.5, 0.2);
    }

    private void createVerticalWheelEffect(Vec3 userPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Create rotating vertical wheel effect
        double wheelSpeed = wheelTicks * 0.4; // Rotation speed
        int particlesPerFrame = 12;

        for (int i = 0; i < particlesPerFrame; i++) {
            double angle = (i / (double)particlesPerFrame) * 2 * Math.PI + wheelSpeed;
            double radius = hitboxSize * 0.7;

            // Vertical wheel - particles rotate in XY plane
            double x = userPos.x + Math.cos(angle) * radius;
            double y = userPos.y + Math.sin(angle) * radius;
            double z = userPos.z;

            // Main wheel particles
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    x, y, z, 2, 0.1, 0.1, 0.1, 0.08);

            // Add some depth with Z offset
            double zOffset = Math.sin(angle * 2) * 0.3; // Creates some depth variation
            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                    x, y, z + zOffset, 1, 0.05, 0.05, 0.05, 0.03);
        }

        // Central water spout
        if (wheelTicks % 5 == 0) {
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    userPos.x, userPos.y, userPos.z,
                    6, 0.2, 0.8, 0.2, 0.1);
        }

        // Outer wheel rim effect
        if (wheelTicks % 8 == 0) {
            createWaterCircle(userPos, hitboxSize, 16);
        }
    }

    @Override
    protected void onStop() {
        // Reset user velocity
        user.setDeltaMovement(Vec3.ZERO);

        // Clear hit tracking
        hitEntities.clear();
        wheelTicks = 0;
        wheelStarted = false;

        // Final wheel dissolution effect
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            // Final water explosion
            createWaterExplosion(userPos, 1.5f);

            // Wheel dissolution particles
            for (int i = 0; i < 20; i++) {
                double angle = (i / 20.0) * 2 * Math.PI;
                double radius = hitboxSize;

                double x = userPos.x + Math.cos(angle) * radius;
                double y = userPos.y + Math.sin(angle) * radius;
                double z = userPos.z;

                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        x, y, z, 3, 0.3, 0.3, 0.3, 0.15);
            }
        }

        // Final water sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0f, 0.8f);
    }
}