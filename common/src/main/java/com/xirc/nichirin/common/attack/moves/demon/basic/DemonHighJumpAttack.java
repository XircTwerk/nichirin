package com.xirc.nichirin.common.attack.moves.demon.basic;

import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.component.IDemonAttacker;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Demon high jump attack - launches player 5 blocks up with powerful upward dash
 * Similar to dash strike but goes straight up instead of forward
 * Now launches all entities within 3 blocks up with the user
 * Crouch + Right-click when on ground
 */
public class DemonHighJumpAttack extends AbstractDemonAttack<DemonHighJumpAttack, IDemonAttacker> {

    private boolean jumpExecuted = false;
    private Vec3 startPosition;

    public DemonHighJumpAttack() {
        // Configuration comes from moveset
    }

    @Override
    protected void onStart() {
        jumpExecuted = false;

        if (user != null) {
            startPosition = user.position();
        }

        // Powerful launch sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.0f, 0.6f);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5f, 2.0f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide || user == null) return;

        // Execute jump immediately after windup
        if (!jumpExecuted && tickCount >= windup) {
            executeHighJump();
            jumpExecuted = true;
        }
    }

    private void executeHighJump() {
        if (user == null) return;

        // Find all entities within 3 block radius to launch with the user
        Vec3 userPos = user.position();
        List<LivingEntity> nearbyEntities = world.getEntitiesOfClass(LivingEntity.class,
                user.getBoundingBox().inflate(3.0), // 3 block radius
                entity -> entity != user && entity.isAlive() && !entity.isSpectator()
        );

        // Apply strong upward momentum for jump
        double jumpStrength = 1.2; // Strong upward velocity (5+ blocks)

        // Launch the user
        Vec3 currentVelocity = user.getDeltaMovement();
        user.setDeltaMovement(currentVelocity.x * 0.3, jumpStrength, currentVelocity.z * 0.3);
        user.hasImpulse = true;
        user.hurtMarked = true;

        // Launch all nearby entities with the same force
        for (LivingEntity entity : nearbyEntities) {
            Vec3 entityVelocity = entity.getDeltaMovement();
            entity.setDeltaMovement(entityVelocity.x * 0.3, jumpStrength, entityVelocity.z * 0.3);
            entity.hasImpulse = true;
            entity.hurtMarked = true;

            // Apply stunned effect to launched entities
            MobEffectInstance stunEffect = new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    20,
                    2,
                    false,
                    false,
                    true
            );
            entity.addEffect(stunEffect);
        }

        // Create launch effects
        createLaunchEffects();

        // Launch sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.5f);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.8f);
    }

    private void createLaunchEffects() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position();

        // Ground crater effect
        for (int i = 0; i < 360; i += 30) {
            double angle = Math.toRadians(i);
            double radius = 2.0;

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;

            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    x, userPos.y, z, 2, 0.1, 0.1, 0.1, 0.1);

            serverLevel.sendParticles(ParticleTypes.POOF,
                    x, userPos.y, z, 3, 0.2, 0.1, 0.2, 0.1);
        }

        // Central explosion
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                userPos.x, userPos.y, userPos.z, 1, 0.0, 0.0, 0.0, 0.0);

        // Upward particle stream
        for (int i = 0; i < 20; i++) {
            double offsetX = (serverLevel.random.nextDouble() - 0.5) * 1.0;
            double offsetZ = (serverLevel.random.nextDouble() - 0.5) * 1.0;
            double velocityY = 0.8 + serverLevel.random.nextDouble() * 0.4;

            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    userPos.x + offsetX, userPos.y, userPos.z + offsetZ,
                    1, 0.0, velocityY, 0.0, 0.1);
        }
    }

    private void createAscensionTrail() {
        // Removed complex trail - keeping it simple
    }

    @Override
    protected void onStop() {
        jumpExecuted = false;
        startPosition = null;

        // Final ascending particles
        if (world instanceof ServerLevel serverLevel && user != null) {
            Vec3 userPos = user.position();

            // Burst of upward particles at end of jump
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    userPos.x, userPos.y, userPos.z,
                    8, 0.4, 0.3, 0.4, 0.2);

            // Final explosion effect
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    userPos.x, userPos.y, userPos.z,
                    3, 0.2, 0.2, 0.2, 0.1);
        }
    }
}