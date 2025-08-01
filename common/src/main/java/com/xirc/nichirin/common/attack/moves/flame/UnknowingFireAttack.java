package com.xirc.nichirin.common.attack.moves.flame;

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
 * First Form: Unknowing Fire
 * The user dashes towards the target at tremendous speeds,
 * before unleashing a singular horizontal slash directed at the target's neck for a decapitation.
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class UnknowingFireAttack extends FlameBreathingAttackBase {

    private static final float DASH_DISTANCE = 4.0f; // Longer dash for this attack
    private static final int DASH_DURATION = 6; // Faster dash (6 ticks)
    private static final float SLASH_WIDTH = 8.0f; // Very wide slash
    private static final float SLASH_DEPTH = 3.0f; // Deep slash

    private boolean dashStarted = false;
    private boolean slashExecuted = false;
    private Vec3 dashDirection;
    private Vec3 startPosition;
    private Vec3 targetPosition;
    private Set<LivingEntity> hitEntities = new HashSet<>();

    // Invulnerability and fall damage protection
    private boolean wasInvulnerable = false;
    private boolean shouldPreventFallDamage = false;

    public UnknowingFireAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        dashStarted = false;
        slashExecuted = false;
        hitEntities.clear();

        dashDirection = user.getLookAngle().normalize();
        startPosition = user.position();
        targetPosition = startPosition.add(dashDirection.scale(DASH_DISTANCE));

        // Make user invulnerable during attack
        wasInvulnerable = user.isInvulnerable();
        user.setInvulnerable(true);

        // Flame startup sound
        playFlameSound();

        // Create initial flame particles around user
        createFlameParticles();

        // Fire charge sound for dash preparation
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.2f);

        // Create charging effect
        createDashChargeEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Start dash after windup
        if (!dashStarted && tickCount == windup + 1) {
            startDash();
            dashStarted = true;
        }

        // Continue dash
        if (dashStarted && tickCount > windup && tickCount <= windup + DASH_DURATION) {
            continueDash();
        }

        // Execute massive slash at the end of dash
        if (!slashExecuted && tickCount == windup + DASH_DURATION + 1) {
            executeSlash();
            slashExecuted = true;
        }
    }

    private void createDashChargeEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create intense flame buildup around user
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            double radius = 1.0 + Math.sin(angle * 3) * 0.3; // Wavy circle

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + Math.random() * 2.0;

            serverLevel.sendParticles(ParticleTypes.FLAME,
                    x, y, z, 2, 0.1, 0.1, 0.1, 0.05);
        }

        // Upward flame spiral
        for (int i = 0; i < 15; i++) {
            double height = i * 0.2;
            double angle = i * 0.5;
            double radius = 0.8;

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + height;

            serverLevel.sendParticles(ParticleTypes.FLAME,
                    x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
        }
    }

    private void startDash() {
        // Set user velocity for dash - using FlameTiger's method
        Vec3 dashVelocity = dashDirection.scale(DASH_DISTANCE / DASH_DURATION * 6);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        // Dash start sound - more intense
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2f, 1.8f);

        // Create initial dash burst
        createDashBurst();
    }

    private void continueDash() {
        // Maintain dash velocity - using FlameTiger's method
        Vec3 dashVelocity = dashDirection.scale(DASH_DISTANCE / DASH_DURATION * 6);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;

        // Create continuous intense trail
        createIntenseDashTrail();

        // Hit enemies during dash with light damage
        List<LivingEntity> dashTargets = getTargetsInCustomHitbox(
                user.position().add(0, user.getBbHeight() / 2, 0),
                2.0, 2.5, 2.0);

        for (LivingEntity target : dashTargets) {
            if (!hitEntities.contains(target)) {
                // Very light dash damage (20% of full damage)
                float originalDamage = damage;
                damage = damage * 0.2f;
                hitTarget(target);
                damage = originalDamage;

                hitEntities.add(target);

                // Light knockback during dash
                Vec3 dashKnockback = dashDirection.scale(knockback * 0.3);
                target.push(dashKnockback.x, 0.05, dashKnockback.z);
            }
        }
    }

    private void executeSlash() {
        // Create MASSIVE horizontal slash effect
        createMassiveSlashEffect();

        // Hit all enemies in the massive slash area
        Vec3 userPos = user.position().add(0, user.getBbHeight() * 0.7, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();

        // Create multiple hitboxes for the wide slash
        for (int i = -4; i <= 4; i++) {
            double offset = i * (SLASH_WIDTH / 8.0);
            Vec3 slashCenter = userPos.add(lookDir.scale(SLASH_DEPTH / 2)).add(rightDir.scale(offset));

            List<LivingEntity> targets = getTargetsInCustomHitbox(slashCenter, 2.0, 3.0, 2.0);

            for (LivingEntity target : targets) {
                // Full power slash damage
                hitTargetNoImmunity(target);

                // Strong sideways knockback based on position
                Vec3 slashKnockback = rightDir.scale(knockback * (i > 0 ? 1 : -1) * 1.5);
                slashKnockback = slashKnockback.add(lookDir.scale(knockback * 0.5));
                target.push(slashKnockback.x, 0.4, slashKnockback.z);

                // Create impact particles
                createSlashImpactParticles(target.position());

                // Individual hit sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 1.4f);
            }
        }

        // Main slash sound - very dramatic
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.8f);
    }

    private void createDashBurst() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Explosion of flames at dash start
        serverLevel.sendParticles(ParticleTypes.FLAME,
                userPos.x, userPos.y, userPos.z,
                25, 1.0, 1.0, 1.0, 0.3);

        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                userPos.x, userPos.y, userPos.z,
                15, 0.8, 0.8, 0.8, 0.2);
    }

    private void createIntenseDashTrail() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Dense flame trail behind user
        for (int i = 1; i <= 6; i++) {
            Vec3 trailPos = userPos.subtract(dashDirection.scale(i * 0.4));

            serverLevel.sendParticles(ParticleTypes.FLAME,
                    trailPos.x, trailPos.y, trailPos.z,
                    4, 0.3, 0.3, 0.3, 0.1);

            serverLevel.sendParticles(ParticleTypes.FLAME,
                    trailPos.x, trailPos.y, trailPos.z,
                    6, 0.4, 0.4, 0.4, 0.15);
        }

        // Side flames during dash
        Vec3 rightDir = dashDirection.cross(new Vec3(0, 1, 0)).normalize();
        for (int side = -1; side <= 1; side += 2) {
            Vec3 sidePos = userPos.add(rightDir.scale(side * 1.0));
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    sidePos.x, sidePos.y, sidePos.z,
                    3, 0.2, 0.2, 0.2, 0.1);
        }
    }

    private void createMassiveSlashEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() * 0.7, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();

        // Create MASSIVE horizontal arc of flame particles
        for (int i = -16; i <= 16; i++) { // Much wider slash
            double offset = i * (SLASH_WIDTH / 32.0);

            for (int depth = 0; depth < 6; depth++) { // Multiple depth layers
                Vec3 slashPos = userPos
                        .add(lookDir.scale(1.0 + depth * 0.5))
                        .add(rightDir.scale(offset));

                // Main flame particles - more intense in center
                int particleCount = Math.max(1, 6 - Math.abs(i) / 3);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        slashPos.x, slashPos.y, slashPos.z,
                        particleCount, 0.2, 0.2, 0.2, 0.1);

                if (Math.abs(i) <= 8) {
                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            slashPos.x, slashPos.y, slashPos.z,
                            3, 0.1, 0.1, 0.1, 0.08);
                }

                // Crit particles for cutting effect
                if (Math.abs(i) <= 4 && depth < 3) {
                    serverLevel.sendParticles(ParticleTypes.CRIT,
                            slashPos.x, slashPos.y, slashPos.z,
                            2, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }

        // Create vertical flame walls at the edges
        for (int side = -1; side <= 1; side += 2) {
            Vec3 edgePos = userPos.add(rightDir.scale(side * SLASH_WIDTH / 2));

            for (int height = 0; height < 5; height++) {
                Vec3 wallPos = edgePos.add(0, height * 0.5, 0);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        wallPos.x, wallPos.y, wallPos.z,
                        8, 0.3, 0.3, 0.3, 0.2);
            }
        }

        // Massive flame explosion at slash center
        Vec3 slashCenter = userPos.add(lookDir.scale(SLASH_DEPTH / 2));
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                slashCenter.x, slashCenter.y, slashCenter.z,
                3, 0.5, 0.5, 0.5, 0);

        serverLevel.sendParticles(ParticleTypes.FLAME,
                slashCenter.x, slashCenter.y, slashCenter.z,
                40, 2.0, 1.0, 2.0, 0.4);

        // Smoke cloud from the massive slash
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                slashCenter.x, slashCenter.y + 1, slashCenter.z,
                25, 3.0, 2.0, 3.0, 0.3);
    }

    private void createSlashImpactParticles(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Intense flame burst at impact
        serverLevel.sendParticles(ParticleTypes.FLAME,
                impactPos.x, impactPos.y + 1, impactPos.z,
                15, 0.5, 0.5, 0.5, 0.3);

        serverLevel.sendParticles(ParticleTypes.CRIT,
                impactPos.x, impactPos.y + 1, impactPos.z,
                10, 0.4, 0.4, 0.4, 0.2);

        serverLevel.sendParticles(ParticleTypes.FLAME,
                impactPos.x, impactPos.y + 1, impactPos.z,
                8, 0.3, 0.3, 0.3, 0.15);
    }

    @Override
    public boolean isDashAttack() {
        return true; // This is a dash attack
    }

    @Override
    protected void onStop() {
        // Restore original invulnerability state
        user.setInvulnerable(wasInvulnerable);

        // Set flag to prevent fall damage on next landing
        shouldPreventFallDamage = true;

        // If user is already on ground, reset fall distance to prevent immediate fall damage
        if (user.onGround()) {
            user.resetFallDistance();
        }

        // Reset user velocity
        user.setDeltaMovement(Vec3.ZERO);

        // Final dramatic flame explosion
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            // Massive finale explosion
            createFlameExplosion(userPos, 2.0f);

            // Extra dramatic effects
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    userPos.x, userPos.y, userPos.z,
                    50, 2.5, 2.5, 2.5, 0.5);

            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    userPos.x, userPos.y + 2, userPos.z,
                    30, 2.0, 1.5, 2.0, 0.2);
        }

        // Final dramatic sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.7f);

        // Clear state
        dashStarted = false;
        slashExecuted = false;
        hitEntities.clear();
    }

    @Override
    public void tick() {
        super.tick();

        // Check if we should prevent fall damage and user just landed
        if (shouldPreventFallDamage && user.onGround() && user.fallDistance > 0) {
            user.resetFallDistance(); // Prevent fall damage
            shouldPreventFallDamage = false; // Reset flag after use
        }
    }
}