package com.xirc.nichirin.common.attack.moves.breathing.water;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fifth Form: Blessed Rain After the Drought
 * Ultimate level single hit dash that drops ½ a healthbar on hit
 * Very small 1.0 block hitbox - precision attack that can be angled downwards
 * Similar to Butterfly Attack but with teleport instead of dash after leap
 */
public class BlessedRainAttack extends WaterBreathingAttackBase {

    private boolean leapStarted = false;
    private boolean teleportExecuted = false;
    private Vec3 leapDirection;
    private Vec3 teleportDirection;
    private Vec3 startPosition;

    // Invulnerability during attack
    private boolean wasInvulnerable = false;

    public BlessedRainAttack() {
    }

    @Override
    protected void onStart() {
        leapStarted = false;
        teleportExecuted = false;
        startPosition = user.position();

        // Capture leap and teleport directions at start
        leapDirection = user.getLookAngle().normalize();
        teleportDirection = leapDirection; // Same direction for both

        // Check if user is looking down for angled attack
        if (user.getXRot() > 10) { // Looking down
            teleportDirection = new Vec3(leapDirection.x, -0.5, leapDirection.z).normalize();
        }

        // Make user invulnerable during attack
        wasInvulnerable = user.isInvulnerable();
        user.setInvulnerable(true);

        // Blessed rain startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 0.8f, 1.5f);

        // Create rain gathering effect
        createRainGatheringEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute initial leap after windup (similar to Butterfly)
        if (!leapStarted && tickCount == windup + 1) {
            executeInitialLeap();
            leapStarted = true;
        }

        // Execute teleport strike 15 ticks after leap (0.75 seconds)
        if (leapStarted && !teleportExecuted && tickCount >= windup + 16) {
            executeTeleportStrike();
            teleportExecuted = true;
        }
    }

    private void createRainGatheringEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create rain clouds gathering above user
        for (int i = 0; i < 20; i++) {
            double angle = (i / 20.0) * 2 * Math.PI;
            double radius = 2.5;
            double height = 3 + Math.sin(angle * 2) * 0.5;

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + height;

            // Rain cloud particles
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    x, y, z, 1, 0.1, 0.1, 0.1, 0.02);

            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        x, y, z, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        // Direction preview for leap
        for (int i = 1; i <= 3; i++) {
            Vec3 previewPos = userPos.add(leapDirection.scale(i * 0.4)).add(0, i * 0.3, 0);
            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                    previewPos.x, previewPos.y, previewPos.z,
                    2, 0.1, 0.1, 0.1, 0.05);
        }
    }

    private void executeInitialLeap() {
        // Leap upward with slight forward movement (like Butterfly)
        double upwardVelocity = 0.9;
        double forwardVelocity = 0.15;

        Vec3 horizontalDirection = new Vec3(leapDirection.x, 0, leapDirection.z).normalize();
        Vec3 leapVelocity = horizontalDirection.scale(forwardVelocity).add(0, upwardVelocity, 0);

        user.setDeltaMovement(leapVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        // Sync to client
        if (user instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(user));
        }

        // Leap sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.PLAYERS, 1.0f, 1.8f);

        // Create leap effect
        createLeapEffect();
    }

    private void executeTeleportStrike() {
        // Calculate teleport destination
        Vec3 teleportDestination = user.position().add(teleportDirection.scale(range * 0.8));

        // Instant teleport, stopping at block collision instead of failing or clipping.
        teleportSafe(teleportDestination);
        Vec3 strikePosition = user.position();

        // Create massive rain strike effect
        createRainStrikeEffect(strikePosition);

        // Hit enemies in very small precise hitbox
        List<LivingEntity> targets = getTargetsInCustomHitbox(
                strikePosition.add(0, user.getBbHeight() / 2, 0),
                hitboxSize, // Very small 1.0 block hitbox
                hitboxSize + 0.5,
                hitboxSize
        );

        for (LivingEntity target : targets) {

            hitTarget(target);

            // Strong knockback
            Vec3 strikeKnockback = teleportDirection.scale(knockback);
            target.push(strikeKnockback.x, 0.5, strikeKnockback.z);

            // Create massive impact effect
            createRainImpactEffect(target.position());

            // Critical hit sound
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5f, 0.8f);
        }

        // Teleport strike sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.5f);
    }

    private void createLeapEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Rain droplets falling around leap
        for (int i = 0; i < 15; i++) {
            double x = userPos.x + (Math.random() - 0.5) * 3;
            double z = userPos.z + (Math.random() - 0.5) * 3;
            double y = userPos.y + 2 + Math.random() * 2;

            serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                    x, y, z, 1, 0, -0.3, 0, 0.1);
        }

        // Water splash at takeoff
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                startPosition.x, startPosition.y, startPosition.z,
                12, 0.5, 0.2, 0.5, 0.15);
    }

    private void createRainStrikeEffect(Vec3 strikePos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Massive rain column from sky
        for (int height = 0; height < 10; height++) {
            Vec3 columnPos = strikePos.add(0, height, 0);

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    columnPos.x, columnPos.y, columnPos.z,
                    8, 0.3, 0.3, 0.3, 0.2);

            // Rain droplets
            for (int drops = 0; drops < 5; drops++) {
                double x = columnPos.x + (Math.random() - 0.5) * 1.5;
                double z = columnPos.z + (Math.random() - 0.5) * 1.5;
                double y = columnPos.y + 1;

                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        x, y, z, 1, 0, -0.5, 0, 0.1);
            }
        }

        // Ground impact explosion
        createWaterExplosion(strikePos, 2.5f);

        // Rain splash around impact
        for (int i = 0; i < 24; i++) {
            double angle = (2 * Math.PI * i) / 24;
            double radius = 2.0;

            double x = strikePos.x + Math.cos(angle) * radius;
            double z = strikePos.z + Math.sin(angle) * radius;
            double y = strikePos.y;

            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    x, y, z, 4, 0.4, 0.4, 0.4, 0.2);
        }
    }

    private void createRainImpactEffect(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = impactPos.add(0, 1, 0);

        // Precision strike particles
        serverLevel.sendParticles(ParticleTypes.CRIT,
                targetPos.x, targetPos.y, targetPos.z,
                20, 0.5, 0.5, 0.5, 0.3);

        // Rain impact burst
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                targetPos.x, targetPos.y, targetPos.z,
                30, 0.8, 0.8, 0.8, 0.4);

        // Blessed rain healing effect (visual only)
        serverLevel.sendParticles(ParticleTypes.HEART,
                targetPos.x, targetPos.y, targetPos.z,
                5, 0.3, 0.3, 0.3, 0.1);
    }

    @Override
    public boolean isDashAttack() {
        return true; // This is a teleport dash attack
    }

    @Override
    protected void onStop() {
        // Restore original invulnerability state
        user.setInvulnerable(wasInvulnerable);

        // Reset velocity
        user.setDeltaMovement(Vec3.ZERO);

        // Final blessed rain effect
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            // Final rain shower
            for (int i = 0; i < 50; i++) {
                double x = userPos.x + (Math.random() - 0.5) * 6;
                double z = userPos.z + (Math.random() - 0.5) * 6;
                double y = userPos.y + 4 + Math.random() * 3;

                serverLevel.sendParticles(ParticleTypes.DRIPPING_WATER,
                        x, y, z, 1, 0, -0.4, 0, 0.1);
            }

            // Peaceful water splash
            serverLevel.sendParticles(ParticleTypes.SPLASH,
                    userPos.x, userPos.y, userPos.z,
                    20, 0.6, 0.6, 0.6, 0.2);
        }

        // Peaceful rain ending sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 0.8f, 1.2f);

        // Clear state
        leapStarted = false;
        teleportExecuted = false;
    }
}