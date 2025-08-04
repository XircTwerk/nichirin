package com.xirc.nichirin.common.attack.moves.insect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * First Form: Dance of the Butterfly – Caprice
 * The user dashes forward with light-footed precision and stabs the opponent
 * with a venom-laced blade in one fluid motion.
 *
 * Mechanics:
 * - Instant dash (8-10 blocks)
 * - Single-target high-damage thrust
 * - No AoE — locks onto the enemy and teleports to them
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class ButterflyAttack extends InsectBreathingAttackBase {

    private boolean dashStarted = false;
    private boolean secondDashExecuted = false;
    private Vec3 dashDirection;
    private Vec3 startPosition;

    // Invulnerability during dash
    private boolean wasInvulnerable = false;

    public ButterflyAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        dashStarted = false;
        secondDashExecuted = false;
        startPosition = user.position();

        // DON'T set dash direction here - we'll capture it when the dash actually executes
        // This allows player to adjust aim during the leap phase
        System.out.println("DEBUG: Attack started, dash direction will be captured later");

        // Make user invulnerable during attack
        wasInvulnerable = user.isInvulnerable();
        user.setInvulnerable(true);

        // Butterfly startup sound
        playInsectSound();

        // Create initial butterfly flutter around user
        createButterflyFlutter(user.position().add(0, user.getBbHeight() / 2, 0));

        // Light startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.5f);

        // Create charging effect
        createDashChargeEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return; // Only run on server

        // Execute initial leap after windup
        if (!dashStarted && tickCount == windup + 1) {
            executeInitialLeap();
            dashStarted = true;
        }

        // Execute forward dash 1 second (20 ticks) after the leap
        if (dashStarted && !secondDashExecuted && tickCount == windup + 21) {
            executeForwardDash();
            secondDashExecuted = true;
        }

        // Hit enemies during the dash phase (after forward dash)
        if (secondDashExecuted && tickCount > windup + 21 && tickCount <= windup + 35) {
            List<LivingEntity> dashTargets = getTargetsInCustomHitbox(
                    user.position().add(0, user.getBbHeight() / 2, 0),
                    2.0, 2.5, 2.0);

            for (LivingEntity dashTarget : dashTargets) {
                executeThrust(dashTarget);
                break; // Only hit one target with the main strike
            }
        }
    }

    private void createDashChargeEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Use ServerLevel's random instead of world.random to avoid threading issues
        net.minecraft.util.RandomSource random = serverLevel.getRandom();

        // Create butterfly gathering effect
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * 2 * Math.PI;
            double radius = 2.0 + Math.sin(angle * 3) * 0.5; // Wavy circle

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + Math.sin(angle * 2) * 0.5;

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    x, y, z, 1, 0.05, 0.05, 0.05, 0.02);

            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }

        // FIXED: Remove direction indicator from charging since direction isn't set yet
        // Player can aim during the leap phase
    }

    private void executeInitialLeap() {
        System.out.println("DEBUG: executeInitialLeap() called");

        // For the leap, just go straight up with minimal forward movement
        // Don't commit to a direction yet - player can still aim during leap
        double upwardVelocity = 0.8; // Perfect height as tested
        double initialForwardVelocity = 0.1; // Very minimal forward momentum during leap

        // Use current look direction for minimal forward movement during leap
        Vec3 currentLookDirection = user.getLookAngle();
        Vec3 horizontalDirection = new Vec3(currentLookDirection.x, 0, currentLookDirection.z).normalize();

        Vec3 forwardComponent = horizontalDirection.scale(initialForwardVelocity);
        user.setDeltaMovement(forwardComponent.x, upwardVelocity, forwardComponent.z);
        System.out.println("DEBUG: Leap velocity set: " + user.getDeltaMovement());

        // Mark for client sync
        user.hurtMarked = true;
        user.hasImpulse = true;

        // Sync to client
        if (user instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(user));
        }

        // Leap sound
        playDashSound();
    }

    private void executeForwardDash() {
        System.out.println("DEBUG: executeForwardDash() called");

        // CAPTURE DASH DIRECTION NOW - when the dash actually executes
        // This allows player to aim during the leap phase
        Vec3 currentLookDirection = user.getLookAngle();
        dashDirection = new Vec3(currentLookDirection.x, 0, currentLookDirection.z).normalize();
        System.out.println("DEBUG: Dash direction captured at dash time: " + dashDirection);

        // Use teleportDistance as dash speed (like the builder config shows)
        float actualDashSpeed = (dashSpeed != null) ? dashSpeed : 3.0f;
        System.out.println("DEBUG: dashSpeed = " + dashSpeed + ", using actualDashSpeed = " + actualDashSpeed);

        Vec3 dashVelocity = dashDirection.scale(actualDashSpeed);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        System.out.println("DEBUG: Dash velocity set: " + user.getDeltaMovement());

        // Sync to client
        if (user instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(user));
        }

        // Dash effects
        createDashEffect();
        createInsectTrail(startPosition, user.position());

        // Additional dash sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.5f);
    }

    private void executeThrust(LivingEntity thrustTarget) {
        // Execute precision thrust with full damage and poison
        hitTarget(thrustTarget);

        // Strong knockback away from user
        Vec3 thrustDirection = thrustTarget.position().subtract(user.position()).normalize();
        thrustTarget.push(
                thrustDirection.x * knockback,
                0.3, // Slight upward component
                thrustDirection.z * knockback
        );

        // Create thrust impact effect
        createThrustImpactEffect(thrustTarget);

        // Precision strike sound
        world.playSound(null, thrustTarget.getX(), thrustTarget.getY(), thrustTarget.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.8f);

        // Poison application sound
        playPoisonSound(thrustTarget.position());
    }

    private void createDashEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Use ServerLevel's random instead of world.random to avoid threading issues
        net.minecraft.util.RandomSource random = serverLevel.getRandom();

        // Butterfly burst at arrival
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            double radius = 1.5;
            double speed = 0.3 + random.nextDouble() * 0.2;

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + random.nextDouble() * 2;

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    x, y, z, 1, speed, speed, speed, 0.1);

            if (i % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
            }
        }

        // Enchantment sparkles for magical dash
        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                userPos.x, userPos.y, userPos.z,
                25, 1.0, 1.0, 1.0, 0.2);
    }

    private void createThrustImpactEffect(LivingEntity thrustTarget) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = thrustTarget.position().add(0, thrustTarget.getBbHeight() / 2, 0);

        // Precision strike particles - concentrated burst
        serverLevel.sendParticles(ParticleTypes.CRIT,
                targetPos.x, targetPos.y, targetPos.z,
                15, 0.3, 0.3, 0.3, 0.2);

        // Poison burst
        createPoisonBurst(targetPos, 1.5f);

        // Magical sparkles for venom
        serverLevel.sendParticles(ParticleTypes.WITCH,
                targetPos.x, targetPos.y, targetPos.z,
                20, 0.5, 0.5, 0.5, 0.25);

        // Butterfly scatter effect
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * 2 * Math.PI;
            double distance = 2.0;

            double x = targetPos.x + Math.cos(angle) * distance;
            double z = targetPos.z + Math.sin(angle) * distance;
            double y = targetPos.y + 1;

            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    x, y, z, 2, 0.2, 0.2, 0.2, 0.1);
        }
    }

    @Override
    public boolean isPrecisionAttack() {
        return true; // This is a precision lock-on attack
    }

    @Override
    public boolean isDashAttack() {
        return true; // This is a dash attack
    }

    @Override
    protected void onStop() {
        // Restore original invulnerability state
        user.setInvulnerable(wasInvulnerable);

        // Final butterfly flutter effect
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
            createButterflyFlutter(userPos);

            // Final sparkle effect
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    userPos.x, userPos.y, userPos.z,
                    15, 0.8, 0.8, 0.8, 0.15);
        }

        // Graceful landing sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 2.0f);

        // Clear state
        dashStarted = false;
        secondDashExecuted = false;
    }
}