package com.xirc.nichirin.common.attack.moves.thunder;

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
 */
public class RiceSpiritAttack extends ThunderBreathingAttackBase {

    private int slashCount = 0;
    private int slashTimer = 0;
    private LivingEntity lockedTarget = null;
    private final Random random = new Random();

    public RiceSpiritAttack() {
        withTiming(40, 5, 30) // cooldown, windup, duration
                .withDamage(8.0f) // Lower damage per slash
                .withRange(5.0f) // Range to find enemies
                .withKnockback(0.3f)
                .withBreathCost(15.0f)
                .withHitStun(20)
                .withHitboxSize(2.0f); // Hitbox around the target
    }

    @Override
    protected void onStart() {
        // Reset counters
        slashCount = 0;
        slashTimer = 0;
        lockedTarget = null;

        // Find closest enemy within range
        lockedTarget = findClosestEnemy();

        if (lockedTarget == null) {
            // No target in range - cancel the attack
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

        // Damage the locked target
        hitTarget(lockedTarget);

        // Visual feedback on the target
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    lockedTarget.getX(), lockedTarget.getY() + 1, lockedTarget.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1);
        }
    }

    /**
     * Find the closest enemy within range
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
        }

        // Sort by distance and return closest
        return entities.stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(user)))
                .orElse(null);
    }

    @Override
    protected void onStop() {
        lockedTarget = null;
    }
}