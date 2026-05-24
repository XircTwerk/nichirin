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

/**
 * Demon high jump attack - launches player 5 blocks up with powerful upward dash
 * Similar to dash strike but goes straight up instead of forward
 * Now launches all entities within 3 blocks up with the user
 * Crouch + Right-click when on ground
 */
public class DemonHighJumpAttack extends AbstractDemonAttack<DemonHighJumpAttack, IDemonAttacker> {

    private boolean jumpExecuted = false;
    private boolean preventFallDamage = false;
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

        // Apply strong upward momentum for jump (#86 — buffed)
        double jumpStrength = 1.5;

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
        if (!(world instanceof ServerLevel sl) || user == null) return;

        Vec3 ground = user.position();

        // Radial ring of EXPLOSION + POOF on the ground (12 directions, every 30°)
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30.0);
            double rx = Math.cos(angle) * 2.0;
            double rz = Math.sin(angle) * 2.0;
            sl.sendParticles(ParticleTypes.EXPLOSION,
                    ground.x + rx, ground.y, ground.z + rz, 2, 0.1, 0.05, 0.1, 0.02);
            sl.sendParticles(ParticleTypes.POOF,
                    ground.x + rx, ground.y, ground.z + rz, 3, 0.15, 0.05, 0.15, 0.03);
        }

        // Large central explosion
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                ground.x, ground.y, ground.z, 1, 0, 0, 0, 0);

        // Upward cloud stream
        for (int i = 0; i < 20; i++) {
            double height = i * 0.3;
            sl.sendParticles(ParticleTypes.CLOUD,
                    ground.x + (world.random.nextDouble() - 0.5) * 0.6,
                    ground.y + height,
                    ground.z + (world.random.nextDouble() - 0.5) * 0.6,
                    1, 0.1, 0.05, 0.1, 0.02 + height * 0.01);
        }
    }

    private void createAscensionTrail() {
        // Removed complex trail - keeping it simple
    }

    @Override
    protected void onStop() {
        if (world instanceof ServerLevel sl && user != null) {
            Vec3 landing = user.position();
            sl.sendParticles(ParticleTypes.CLOUD,
                    landing.x, landing.y + user.getBbHeight() * 0.5, landing.z,
                    8, 0.4, 0.2, 0.4, 0.1);
            sl.sendParticles(ParticleTypes.EXPLOSION,
                    landing.x, landing.y, landing.z, 3, 0.3, 0.1, 0.3, 0.05);
        }
        jumpExecuted = false;
        preventFallDamage = true;
        startPosition = null;
    }

    @Override
    public void tick() {
        super.tick();
        if (preventFallDamage && user != null && user.onGround()) {
            user.resetFallDistance();
            preventFallDamage = false;
        }
    }
}