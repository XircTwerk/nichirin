package com.xirc.nichirin.common.attack.moves.demon.basic;

import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.component.IDemonAttacker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Demon stomp attack - high damage area attack that buries enemies
 * Crouch + Right-click when airborne
 */
public class DemonStompAttack extends AbstractDemonAttack<DemonStompAttack, IDemonAttacker> {

    private boolean stompExecuted = false;

    public DemonStompAttack() {
        // Configuration comes from moveset
    }

    @Override
    protected void onStart() {
        stompExecuted = false;

        // Whoosh sound as demon prepares to slam down
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8f, 0.5f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute stomp immediately (no windup)
        if (!stompExecuted && tickCount >= windup) {
            executeStomp();
            stompExecuted = true;
        }
    }

    private void executeStomp() {
        if (user == null) return;

        // Can only stomp when airborne
        if (user.onGround()) {
            return;
        }

        Vec3 userPos = user.position();

        // Hit all enemies in range around the impact point
        List<LivingEntity> targets = getTargetsInCircle((float)range, 8);

        for (LivingEntity target : targets) {
            // High damage stomp
            hitTarget(target);

            // Increase gravity effect - make them fall faster/harder
            if (target.isAlive()) {
                // Apply strong downward velocity
                Vec3 currentVelocity = target.getDeltaMovement();
                target.setDeltaMovement(currentVelocity.x, -1.5, currentVelocity.z); // Strong downward force
                target.hasImpulse = true;
                target.hurtMarked = true;


                // Bury effect - teleport target 1 block down
                BlockPos targetPos = target.blockPosition();
                BlockPos buriedPos = targetPos.below();

                // Check if the position below is safe (not in void or lava)
                if (buriedPos.getY() > world.getMinBuildHeight() &&
                        !world.getBlockState(buriedPos).isAir()) {

                    target.teleportTo(target.getX(), buriedPos.getY() + 1, target.getZ());

                    // Buried in ground effect
                    if (world instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.POOF,
                                target.getX(), target.getY(), target.getZ(),
                                10, 0.5, 0.3, 0.5, 0.1);
                    }
                }
            }

            // Impact sound per target
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 0.8f);
        }

        // Create massive ground impact
        createStompImpact(userPos);

        // Force player to ground with downward velocity
        user.setDeltaMovement(user.getDeltaMovement().x, -2.0, user.getDeltaMovement().z);

        // Main impact sounds
        world.playSound(null, userPos.x, userPos.y, userPos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 0.6f);

        world.playSound(null, userPos.x, userPos.y, userPos.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    private void createStompImpact(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Central massive explosion
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                impactPos.x, impactPos.y, impactPos.z, 3, 0.5, 0.1, 0.5, 0.0);

        // Shockwave rings
        for (int ring = 1; ring <= 3; ring++) {
            double radius = ring * 2.0;
            int particlesInRing = ring * 12;

            for (int i = 0; i < particlesInRing; i++) {
                double angle = (i / (double)particlesInRing) * 2 * Math.PI;
                double x = impactPos.x + Math.cos(angle) * radius;
                double z = impactPos.z + Math.sin(angle) * radius;

                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        x, impactPos.y, z, 2, 0.1, 0.1, 0.1, 0.05);

                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        x, impactPos.y + 0.5, z, 1, 0.1, 0.2, 0.1, 0.05);
            }
        }

        // Ground debris
        for (int i = 0; i < 50; i++) {
            double offsetX = (serverLevel.random.nextDouble() - 0.5) * range * 2;
            double offsetZ = (serverLevel.random.nextDouble() - 0.5) * range * 2;
            double velocityY = 0.5 + serverLevel.random.nextDouble() * 0.8;

            serverLevel.sendParticles(ParticleTypes.ASH,
                    impactPos.x + offsetX, impactPos.y, impactPos.z + offsetZ,
                    3, 0.3, velocityY, 0.3, 0.2);
        }

        // Crater dust cloud
        serverLevel.sendParticles(ParticleTypes.POOF,
                impactPos.x, impactPos.y + 1, impactPos.z,
                30, 2.0, 1.0, 2.0, 0.3);

        // Upward impact burst
        for (int i = 0; i < 20; i++) {
            double velocityX = (serverLevel.random.nextDouble() - 0.5) * 1.0;
            double velocityY = 1.0 + serverLevel.random.nextDouble() * 1.5;
            double velocityZ = (serverLevel.random.nextDouble() - 0.5) * 1.0;

            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    impactPos.x, impactPos.y, impactPos.z,
                    1, velocityX, velocityY, velocityZ, 0.2);
        }
    }

    @Override
    protected void onStop() {
        stompExecuted = false;
    }
}