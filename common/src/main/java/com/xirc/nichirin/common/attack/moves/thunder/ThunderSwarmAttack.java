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
 * Third Form: Thunder Swarm
 * 6 large-scale slashes in random positions around a wide area
 */
public class ThunderSwarmAttack extends ThunderBreathingAttackBase {

    private int slashCount = 0;
    private int slashTimer = 0;
    private final Set<LivingEntity> hitEntities = new HashSet<>();
    private final Random random = new Random();

    public ThunderSwarmAttack() {
        withTiming(50, 10, 40) // cooldown, windup, duration
                .withDamage(10.0f)
                .withRange(8.0f) // Large area around player
                .withKnockback(0.2f)
                .withBreathCost(25.0f)
                .withHitStun(20)
                .withHitboxSize(3.0f); // Size 3 hitbox as specified
    }

    @Override
    protected void onStart() {
        // Reset counters
        slashCount = 0;
        slashTimer = 0;
        hitEntities.clear();

        // Thunder sounds
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.7f, 1.5f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Player can move during this attack
        // Execute slashes over time
        slashTimer++;

        if (slashTimer % 6 == 0 && slashCount < 6) {
            performLargeSlash();
            slashCount++;
        }
    }

    private void performLargeSlash() {
        // Random position within range
        float angle = random.nextFloat() * 360f;
        float distance = 2.0f + random.nextFloat() * (range - 2.0f);
        float radian = (float) Math.toRadians(angle);

        Vec3 slashCenter = user.position().add(
                Math.cos(radian) * distance,
                0.5 + random.nextFloat() * 2.0f, // Varying heights
                Math.sin(radian) * distance
        );

        // Create large visual effect
        if (world instanceof ServerLevel serverLevel) {
            // Multiple sweep attacks for larger visual
            for (int i = 0; i < 3; i++) {
                Vec3 offset = new Vec3(
                        (random.nextFloat() - 0.5) * 2,
                        (random.nextFloat() - 0.5) * 2,
                        (random.nextFloat() - 0.5) * 2
                );
                Vec3 particlePos = slashCenter.add(offset);

                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        particlePos.x, particlePos.y, particlePos.z,
                        1, 0, 0, 0, 0);
            }

            // Lots of electric particles for swarm effect
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    slashCenter.x, slashCenter.y, slashCenter.z,
                    30, 1.0, 1.0, 1.0, 0.2);

            // Lightning bolt particle effect
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    slashCenter.x, slashCenter.y + 3, slashCenter.z,
                    5, 0.1, 2.0, 0.1, 0.1);
        }

        // Thunder sound for each slash
        world.playSound(null, slashCenter.x, slashCenter.y, slashCenter.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS,
                0.6f, 1.8f + random.nextFloat() * 0.4f);

        // Damage entities in large hitbox
        List<LivingEntity> targets = getTargetsInHitbox(slashCenter);

        for (LivingEntity target : targets) {
            if (!hitEntities.contains(target)) {
                hitTarget(target);
                hitEntities.add(target);

                // Extra particles on hit
                if (world instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            target.getX(), target.getY() + 1, target.getZ(),
                            15, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }
    }

    @Override
    protected void onStop() {
        hitEntities.clear();
    }
}