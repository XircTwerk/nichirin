package com.xirc.nichirin.common.attack.moves.demon.basic;

import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.component.IDemonAttacker;
import com.xirc.nichirin.registry.NichirinSoundRegistry;
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
                target.setDeltaMovement(currentVelocity.x, -1.5 * statMultiplier, currentVelocity.z); // Strong downward force
                target.hasImpulse = true;
                target.hurtMarked = true;


                // Bury effect - teleport target 1 block down
                BlockPos targetPos = target.blockPosition();
                BlockPos buriedPos = targetPos.below();

                // Check if the position below is safe (not in void or lava)
                if (buriedPos.getY() > world.getMinBuildHeight() &&
                        !world.getBlockState(buriedPos).isAir()) {

                    target.teleportTo(target.getX(), buriedPos.getY() + 1, target.getZ());

                    }
            }

            // Impact sound per target
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    NichirinSoundRegistry.STOMP_LAND.get(), SoundSource.PLAYERS, 1.0f, 0.8f);
        }

        // Create massive ground impact (1 block below user position)
        createStompImpact(userPos.add(0, -1, 0));

        // Force player to ground with downward velocity
        user.setDeltaMovement(user.getDeltaMovement().x, -2.0 * statMultiplier, user.getDeltaMovement().z);

        // Main impact sounds
        world.playSound(null, userPos.x, userPos.y, userPos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 0.6f);

        world.playSound(null, userPos.x, userPos.y, userPos.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    private void createStompImpact(Vec3 impactPos) {
        if (!(world instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.EXPLOSION,
                impactPos.x, impactPos.y + 1, impactPos.z, 1, 0.0, 0.0, 0.0, 0.0);
        sl.sendParticles(ParticleTypes.POOF,
                impactPos.x, impactPos.y + 0.5, impactPos.z, 25, 1.2, 0.3, 1.2, 0.05);
        sl.sendParticles(ParticleTypes.CLOUD,
                impactPos.x, impactPos.y + 1, impactPos.z, 8, 0.8, 0.3, 0.8, 0.02);
    }

    @Override
    protected void onStop() {
        stompExecuted = false;
    }
}
