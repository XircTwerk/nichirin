package com.xirc.nichirin.common.attack.moves.thunder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fourth Form: Distant Thunder
 * Summons lightning on all enemies in a radius over 8 seconds (4 strikes)
 */
public class DistantThunderAttack extends ThunderBreathingAttackBase {

    private int strikeCount = 0;
    private int strikeTimer = 0;
    private final Set<LivingEntity> trackedTargets = new HashSet<>();

    public DistantThunderAttack() {
        withTiming(80, 15, 160) // cooldown, windup, 8 seconds duration
                .withDamage(12.0f)
                .withRange(20.0f) // Large AOE radius
                .withKnockback(0.1f)
                .withBreathCost(30.0f)
                .withHitStun(20); // 1 second stun per strike
    }

    @Override
    protected void onStart() {
        // Reset counters
        strikeCount = 0;
        strikeTimer = 0;
        trackedTargets.clear();

        // Find all targets in range at start
        Vec3 center = user.position();
        AABB searchArea = new AABB(
                center.x - range, center.y - range, center.z - range,
                center.x + range, center.y + range, center.z + range
        );

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != user && entity.isAlive() &&
                        entity.position().distanceTo(center) <= range);

        trackedTargets.addAll(targets);

        // Ominous thunder sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.5f);

        // Visual indicator - dark clouds effect
        if (world instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 50; i++) {
                double offsetX = (world.random.nextDouble() - 0.5) * range * 2;
                double offsetZ = (world.random.nextDouble() - 0.5) * range * 2;
                serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                        center.x + offsetX, center.y + 15, center.z + offsetZ,
                        1, 0, -0.1, 0, 0.05);
            }
        }
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Strike every 2 seconds (40 ticks)
        strikeTimer++;

        if (strikeTimer >= 40 && strikeCount < 4) {
            performLightningStrike();
            strikeTimer = 0;
            strikeCount++;
        }

        // Continuous storm effects
        if (tickCount % 10 == 0 && world instanceof ServerLevel serverLevel) {
            Vec3 center = user.position();
            for (int i = 0; i < 20; i++) {
                double offsetX = (world.random.nextDouble() - 0.5) * range * 2;
                double offsetZ = (world.random.nextDouble() - 0.5) * range * 2;
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        center.x + offsetX, center.y + 10 + world.random.nextDouble() * 5, center.z + offsetZ,
                        1, 0, -0.2, 0, 0.1);
            }
        }
    }

    private void performLightningStrike() {
        ServerLevel serverLevel = (ServerLevel) world;

        // Strike all tracked targets
        for (LivingEntity target : trackedTargets) {
            if (target.isAlive() && target.position().distanceTo(user.position()) <= range) {
                // Create visual lightning bolt (doesn't do vanilla damage)
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
                if (lightning != null) {
                    lightning.moveTo(target.position());
                    lightning.setVisualOnly(true);
                    serverLevel.addFreshEntity(lightning);
                }

                // Apply our custom damage and effects
                hitTarget(target);

                // Extra effects at strike location
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), target.getY(), target.getZ(),
                        50, 1.0, 2.0, 1.0, 0.5);

                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        target.getX(), target.getY() + target.getBbHeight(), target.getZ(),
                        20, 0.3, 0.3, 0.3, 0.1);

                // Thunder sound at target
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.0f);
            }
        }

        // Also strike random positions for atmosphere
        for (int i = 0; i < 3; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * range * 1.5;
            double offsetZ = (world.random.nextDouble() - 0.5) * range * 1.5;
            Vec3 strikePos = user.position().add(offsetX, 0, offsetZ);

            // Find ground level
            BlockPos groundPos = world.getHeightmapPos(
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                    new BlockPos((int)strikePos.x, (int)strikePos.y, (int)strikePos.z)
            );

            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
            if (lightning != null) {
                lightning.moveTo(groundPos.getX(), groundPos.getY(), groundPos.getZ());
                lightning.setVisualOnly(true);
                serverLevel.addFreshEntity(lightning);
            }
        }
    }

    @Override
    protected void onStop() {
        trackedTargets.clear();
    }
}