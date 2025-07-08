package com.xirc.nichirin.common.attack.moves.thunder;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Second Form: Rice Spirit
 * 5 slashes with random directions around the player
 */
public class RiceSpiritAttack extends ThunderBreathingAttackBase {

    private int slashCount = 0;
    private int slashTimer = 0;
    private final Set<LivingEntity> hitEntities = new HashSet<>();
    private final Set<LivingEntity> lockedTargets = new HashSet<>();
    private final Random random = new Random();

    public RiceSpiritAttack() {
        withTiming(40, 5, 30) // cooldown, windup, duration
                .withDamage(8.0f) // Lower damage per slash
                .withRange(2.0f) // Distance from player for slashes
                .withKnockback(0.3f)
                .withBreathCost(15.0f)
                .withHitStun(20);
    }

    @Override
    protected void onStart() {
        // Reset counters
        slashCount = 0;
        slashTimer = 0;
        hitEntities.clear();
        lockedTargets.clear();

        // Thunder sound on start
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.3f, 2.0f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute slashes with 0.2 second intervals (4 ticks)
        slashTimer++;

        if (slashTimer % 4 == 0 && slashCount < 5) {
            performSlash();
            slashCount++;
        }
    }

    private void performSlash() {
        // Random angle for this slash
        float angle = random.nextFloat() * 360f;
        float radian = (float) Math.toRadians(angle);

        // Calculate slash position (1-2 blocks away)
        float distance = 1.0f + random.nextFloat();
        Vec3 slashPos = user.position().add(
                Math.cos(radian) * distance,
                1.0, // Height offset
                Math.sin(radian) * distance
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
                    10, 0.3, 0.3, 0.3, 0.1);
        }

        // Play slash sound
        world.playSound(null, slashPos.x, slashPos.y, slashPos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                0.8f, 1.5f + random.nextFloat() * 0.2f);

        // Get targets in hitbox
        List<LivingEntity> targets = getTargetsInHitbox(slashPos);

        for (LivingEntity target : targets) {
            // First hit locks the target for guaranteed combo
            if (slashCount == 0 && !hitEntities.contains(target)) {
                lockedTargets.add(target);
            }

            // If target is locked or this is the first hit
            if (lockedTargets.contains(target) || !hitEntities.contains(target)) {
                // Use base hit method
                hitTarget(target);
                hitEntities.add(target);
            }
        }
    }

    @Override
    protected void onStop() {
        hitEntities.clear();
        lockedTargets.clear();
    }
}