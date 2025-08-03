package com.xirc.nichirin.common.attack.moves.insect;

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
 * Poison Dash - Crouch + Right-click attack for Insect Breathing
 * A short dash that leaves a trail of venom.
 *
 * Mechanics:
 * - Quick short dash
 * - Light damage + poison trail
 * - Good mobility option
 * - No cooldown for frequent use
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class PoisonDashAttack extends InsectBreathingAttackBase {

    private static final int DASH_DURATION = 10;

    private boolean dashStarted = false;
    private Vec3 dashDirection;
    private Set<LivingEntity> hitEntities = new HashSet<>();

    public PoisonDashAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        dashStarted = false;
        hitEntities.clear();

        dashDirection = user.getLookAngle().normalize();

        // Poison dash startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.8f, 1.5f);

        // Create initial poison aura
        createPoisonAura();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Start dash after short windup
        if (!dashStarted && tickCount == windup + 1) {
            startPoisonDash();
            dashStarted = true;
        }

        // Continue dash with poison trail
        if (dashStarted && tickCount > windup && tickCount <= windup + DASH_DURATION) {
            continuePoisonDash();
        }
    }

    private void createPoisonAura() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Poison gathering effect around user
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * 2 * Math.PI;
            double radius = 1.0 + Math.sin(angle * 3) * 0.2;

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + Math.sin(angle * 2) * 0.4;

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    x, y, z, 1, 0.05, 0.05, 0.05, 0.02);

            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }

        // Direction indicator with poison
        for (int i = 1; i <= 4; i++) {
            Vec3 dirPos = userPos.add(dashDirection.scale(i * 0.5));
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    dirPos.x, dirPos.y, dirPos.z,
                    2, 0.1, 0.1, 0.1, 0.05);
        }
    }

    private void startPoisonDash() {
        // Set dash velocity
        Vec3 dashVelocity = dashDirection.scale(dashSpeed);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        // Dash sound
        playDashSound();

        // Additional poison sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LINGERING_POTION_THROW, SoundSource.PLAYERS, 0.8f, 1.3f);

        // Create initial dash burst with poison
        createPoisonDashBurst();
    }

    private void continuePoisonDash() {
        // Maintain dash velocity
        Vec3 dashVelocity = dashDirection.scale(dashSpeed);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;

        // Create continuous poison trail
        createPoisonTrail();

        // Hit enemies along the dash path (light damage)
        List<LivingEntity> dashTargets = getTargetsInCustomHitbox(
                user.position().add(0, user.getBbHeight() / 2, 0),
                hitboxSize, 2.0, hitboxSize);

        for (LivingEntity target : dashTargets) {
            if (!hitEntities.contains(target)) {
                // Light dash damage with poison
                hitTarget(target);
                hitEntities.add(target);

                // Very light knockback
                Vec3 dashKnockback = dashDirection.scale(knockback * 0.3);
                target.push(dashKnockback.x, 0.05, dashKnockback.z);

                // Create poison impact
                createPoisonImpactEffect(target.position());

                // Light hit sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.SPIDER_HURT, SoundSource.PLAYERS, 0.5f, 1.8f);
            }
        }
    }

    private void createPoisonDashBurst() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Poison explosion at dash start
        serverLevel.sendParticles(ParticleTypes.WITCH,
                userPos.x, userPos.y, userPos.z,
                15, 0.8, 0.8, 0.8, 0.2);

        serverLevel.sendParticles(ParticleTypes.PORTAL,
                userPos.x, userPos.y, userPos.z,
                10, 0.6, 0.6, 0.6, 0.15);

        // Lingering poison cloud
        serverLevel.sendParticles(ParticleTypes.WITCH,
                userPos.x, userPos.y, userPos.z,
                8, 0.4, 0.4, 0.4, 0.1);
    }

    private void createPoisonTrail() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Dense poison trail behind user
        for (int i = 1; i <= 6; i++) {
            Vec3 trailPos = userPos.subtract(dashDirection.scale(i * 0.4));

            // Main poison particles
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    trailPos.x, trailPos.y, trailPos.z,
                    3, 0.2, 0.2, 0.2, 0.08);

            // Additional venom effect
            if (i <= 3) {
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        trailPos.x, trailPos.y, trailPos.z,
                        2, 0.15, 0.15, 0.15, 0.06);
            }
        }

        // Ground poison puddles
        Vec3 groundPos = user.position();
        serverLevel.sendParticles(ParticleTypes.WITCH,
                groundPos.x, groundPos.y + 0.1, groundPos.z,
                4, 0.5, 0.1, 0.5, 0.05);

        // Side poison vapors
        Vec3 rightDir = dashDirection.cross(new Vec3(0, 1, 0)).normalize();
        for (int side = -1; side <= 1; side += 2) {
            Vec3 sidePos = userPos.add(rightDir.scale(side * 0.6));
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    sidePos.x, sidePos.y, sidePos.z,
                    2, 0.1, 0.1, 0.1, 0.04);
        }
    }

    private void createPoisonImpactEffect(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = impactPos.add(0, 1, 0);

        // Light poison impact
        serverLevel.sendParticles(ParticleTypes.WITCH,
                targetPos.x, targetPos.y, targetPos.z,
                12, 0.3, 0.3, 0.3, 0.15);

        serverLevel.sendParticles(ParticleTypes.PORTAL,
                targetPos.x, targetPos.y, targetPos.z,
                8, 0.2, 0.2, 0.2, 0.1);

        // Small poison burst
        serverLevel.sendParticles(ParticleTypes.WITCH,
                targetPos.x, targetPos.y, targetPos.z,
                6, 0.4, 0.4, 0.4, 0.12);
    }

    @Override
    public boolean isDashAttack() {
        return true; // This is a dash attack
    }

    @Override
    protected void onStop() {
        // Stop user movement
        user.setDeltaMovement(Vec3.ZERO);

        // Final poison cloud effect
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            // Lingering poison cloud
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    userPos.x, userPos.y, userPos.z,
                    20, 1.0, 1.0, 1.0, 0.15);

            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    userPos.x, userPos.y, userPos.z,
                    12, 0.8, 0.8, 0.8, 0.1);

            // Ground poison residue
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    userPos.x, userPos.y, userPos.z,
                    15, 1.5, 0.2, 1.5, 0.08);
        }

        // Final poison sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.5f, 1.0f);

        // Clear state
        dashStarted = false;
        hitEntities.clear();
    }
}