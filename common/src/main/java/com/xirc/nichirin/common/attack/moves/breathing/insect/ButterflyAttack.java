package com.xirc.nichirin.common.attack.moves.breathing.insect;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
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
 * - Initial leap (8 blocks up) then forward dash (8-10 blocks) - NOW AIMABLE
 * - Single-target high-damage thrust
 * - No AoE — precision attack with leap-dash timing
 * - Total duration: windup + 35 ticks
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class ButterflyAttack extends InsectBreathingAttackBase {

    private boolean dashStarted = false;
    private boolean secondDashExecuted = false;
    private Vec3 leapDirection; // Direction for the initial leap
    private Vec3 dashDirection; // Direction for the forward dash (captured later)
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

        // Capture leap direction at start (for the initial upward leap)
        leapDirection = user.getLookAngle().normalize();

        // Don't capture dash direction yet - we'll do that right before the dash!
        dashDirection = null;

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

        // Create charging effect with leap direction preview
        createLeapChargeEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return; // Only run on server

        // Execute initial leap after windup (tick windup + 1)
        if (!dashStarted && tickCount == windup + 1) {
            executeInitialLeap();
            dashStarted = true;
        }

        // Execute forward dash 1 second (20 ticks) after the leap (tick windup + 21)
        if (dashStarted && !secondDashExecuted && tickCount == windup + 21) {
            // FIXED: Capture dash direction right before executing the dash!
            dashDirection = user.getLookAngle().normalize();
            executeForwardDash();
            secondDashExecuted = true;
        }

        // Hit enemies during the dash phase (windup + 21 to windup + 35 = 14 ticks)
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

    private void createLeapChargeEffect() {
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

            // Use butterfly particles like Bee Sting
            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                    x, y, z, 1, 0.05, 0.05, 0.05, 0.02);

            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }

        // Direction indicator showing where the leap will go (upward with slight forward)
        for (int i = 1; i <= 3; i++) {
            Vec3 leapPreview = userPos.add(leapDirection.scale(i * 0.3)).add(0, i * 0.4, 0);
            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                    leapPreview.x, leapPreview.y, leapPreview.z,
                    1, 0.05, 0.05, 0.05, 0.02);
        }
    }

    private void executeInitialLeap() {
        // For the leap, go straight up with minimal forward movement in the aimed direction
        double upwardVelocity = 0.8; // Perfect height as tested
        double initialForwardVelocity = 0.1; // Very minimal forward momentum during leap

        // Use the leap direction for consistent aiming
        Vec3 horizontalDirection = new Vec3(leapDirection.x, 0, leapDirection.z).normalize();

        Vec3 forwardComponent = horizontalDirection.scale(initialForwardVelocity);
        user.setDeltaMovement(forwardComponent.x, upwardVelocity, forwardComponent.z);

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
        // Use teleportDistance as dash speed (like the builder config shows)
        float actualDashSpeed = (dashSpeed != null) ? dashSpeed : 3.0f;

        // Use the freshly captured dash direction
        Vec3 dashVelocity = dashDirection.scale(actualDashSpeed);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        // Sync to client
        if (user instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(user));
        }

        // Dash effects - using butterfly particles like Bee Sting
        createDashEffect();
        createButterflyTrail(startPosition, user.position());

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

        // Butterfly burst at arrival - using butterfly particles like Bee Sting
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            double radius = 1.5;
            double speed = 0.3 + random.nextDouble() * 0.2;

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + random.nextDouble() * 2;

            // Primary butterfly particles
            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
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

    /**
     * Create butterfly trail between two points (like Bee Sting trail)
     */
    private void createButterflyTrail(Vec3 start, Vec3 end) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 direction = end.subtract(start);
        double distance = direction.length();
        Vec3 normalized = direction.normalize();

        // Create butterfly particles along the trail (similar to Bee Sting)
        for (double d = 0; d <= distance; d += 0.4) {
            Vec3 particlePos = start.add(normalized.scale(d));

            // Butterfly trail particles
            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.3, 0.3, 0.3, 0.1);

            if (d % 0.8 < 0.1) { // Every other particle position
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0.05, 0.05, 0.05, 0.03);
            }
        }
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

        // Butterfly particles for venom effect
        serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                targetPos.x, targetPos.y, targetPos.z,
                20, 0.5, 0.5, 0.5, 0.25);

        // Butterfly scatter effect
        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0) * 2 * Math.PI;
            double distance = 2.0;

            double x = targetPos.x + Math.cos(angle) * distance;
            double z = targetPos.z + Math.sin(angle) * distance;
            double y = targetPos.y + 1;

            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
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