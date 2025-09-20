package com.xirc.nichirin.common.attack.moves.breathing.thunder;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Second Form: Rice Spirit
 * 5 slashes focused on a single target - locks onto closest enemy
 *
 * All configuration now comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class RiceSpiritAttack extends ThunderBreathingAttackBase {

    private int slashCount = 0;
    private int slashTimer = 0;
    private LivingEntity lockedTarget = null;
    private final Random random = new Random();

    public RiceSpiritAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        // Reset counters
        slashCount = 0;
        slashTimer = 0;
        lockedTarget = null;

        // Find closest enemy within range (using configured range)
        lockedTarget = findClosestEnemy();

        if (lockedTarget == null) {
            // FIXED: No target in range - stop the attack BEFORE it becomes active
            // This prevents breath consumption and cooldown application

            // Stop the attack immediately - this will prevent breath consumption
            // since the attack never becomes fully active
            stop();
            return;
        }


        // Thunder sound on start
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.3f, 2.0f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Check if we still have a valid target
        if (lockedTarget == null || !lockedTarget.isAlive() || lockedTarget.isRemoved()) {
            stop();
            return;
        }

        // Execute slashes with 0.2 second intervals (4 ticks)
        slashTimer++;

        if (slashTimer % 4 == 0 && slashCount < 5) {
            performSlash();
            slashCount++;

        }

        // Stop after all 5 slashes are complete
        if (slashCount >= 5) {
            stop();
        }
    }

    private void performSlash() {
        if (lockedTarget == null) return;

        // Get target's current position
        Vec3 targetPos = lockedTarget.position();

        // Add some variation to slash positions around the target
        float angleOffset = (slashCount * 72f) + random.nextFloat() * 30f; // Distribute around target
        float radian = (float) Math.toRadians(angleOffset);
        float offsetDistance = 0.5f + random.nextFloat() * 0.5f;

        Vec3 slashPos = targetPos.add(
                Math.cos(radian) * offsetDistance,
                1.0 + random.nextFloat() * 0.5f, // Vary height slightly
                Math.sin(radian) * offsetDistance
        );

        // Create slash visual
        if (world instanceof ServerLevel serverLevel) {
            // Lightning slash effect
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    slashPos.x, slashPos.y, slashPos.z,
                    1, 0, 0, 0, 0);

            // Electric particles
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    slashPos.x, slashPos.y, slashPos.z,
                    15, 0.3, 0.3, 0.3, 0.1);

            // Trail from player to target
            Vec3 playerPos = user.position().add(0, 1, 0);
            int particleCount = 10;
            for (int i = 0; i < particleCount; i++) {
                double t = i / (double) particleCount;
                Vec3 particlePos = playerPos.lerp(slashPos, t);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0, 0, 0, 0);
            }
        }

        // Play slash sound
        world.playSound(null, slashPos.x, slashPos.y, slashPos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                0.8f, 1.5f + random.nextFloat() * 0.2f);

        // FIXED: Use hitTargetNoImmunity to remove immunity frames for rapid slashes
        hitTargetNoImmunity(lockedTarget);

        // Visual feedback on the target
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    lockedTarget.getX(), lockedTarget.getY() + 1, lockedTarget.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1);
        }
    }

    /**
     * Find the closest enemy within range (using configured range)
     */
    private LivingEntity findClosestEnemy() {
        AABB searchBox = new AABB(
                user.getX() - range, user.getY() - range, user.getZ() - range,
                user.getX() + range, user.getY() + range, user.getZ() + range
        );

        List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> entity != user && entity.isAlive() && !entity.isSpectator());


        if (entities.isEmpty()) {
            return null;
        } else {
            // Play init ding sound
            world.playSound(null, user,
                    SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS,
                    0.8f,1.5f);
        }

        // Sort by distance and return closest
        LivingEntity closest = entities.stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(user)))
                .orElse(null);

        if (closest != null) {
            double distance = Math.sqrt(closest.distanceToSqr(user));
        }

        return closest;
    }

    @Override
    protected void onStop() {
        lockedTarget = null;
    }
}