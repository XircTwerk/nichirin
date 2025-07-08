package com.xirc.nichirin.common.attack.moves.thunder;

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
 * Sixth Form: Rumble and Flash
 * Summons lightning around enemies at long range
 */
public class RumbleFlashAttack extends ThunderBreathingAttackBase {

    private final Set<LivingEntity> struckTargets = new HashSet<>();

    public RumbleFlashAttack() {
        withTiming(70, 10, 30) // cooldown, windup, duration
                .withDamage(20.0f)
                .withRange(25.0f) // Very long range
                .withKnockback(0.5f)
                .withBreathCost(30.0f)
                .withHitStun(40); // 2 second stun
    }

    @Override
    protected void onStart() {
        struckTargets.clear();

        // Charge-up sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 0.5f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Strike multiple times during duration
        if (tickCount % 5 == 0) {
            performLightningBarrage();
        }
    }

    private void performLightningBarrage() {
        ServerLevel serverLevel = (ServerLevel) world;
        Vec3 userPos = user.position();

        // Find all targets in range
        AABB searchArea = new AABB(
                userPos.x - range, userPos.y - range, userPos.z - range,
                userPos.x + range, userPos.y + range, userPos.z + range
        );

        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, searchArea,
                entity -> entity != user && entity.isAlive() &&
                        entity.position().distanceTo(userPos) <= range &&
                        !struckTargets.contains(entity));

        // Strike up to 3 targets per barrage
        int strikeCount = Math.min(3, targets.size());
        for (int i = 0; i < strikeCount; i++) {
            if (i < targets.size()) {
                LivingEntity target = targets.get(i);

                // Create multiple lightning bolts around target
                for (int j = 0; j < 3; j++) {
                    double offsetX = (world.random.nextDouble() - 0.5) * 3;
                    double offsetZ = (world.random.nextDouble() - 0.5) * 3;
                    Vec3 strikePos = target.position().add(offsetX, 0, offsetZ);

                    LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
                    if (lightning != null) {
                        lightning.moveTo(strikePos);
                        lightning.setVisualOnly(true);
                        serverLevel.addFreshEntity(lightning);
                    }
                }

                // Apply damage with extended stun
                hitTarget(target);

                // Extra knockback for this form
                Vec3 knockbackDir = target.position().subtract(userPos).normalize();
                target.push(knockbackDir.x * knockback, 0.3, knockbackDir.z * knockback);

                // Massive particle effects
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        target.getX(), target.getY() + 1, target.getZ(),
                        50, 1.0, 1.0, 1.0, 0.3);

                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        target.getX(), target.getY() + target.getBbHeight(), target.getZ(),
                        30, 0.5, 0.5, 0.5, 0.2);

                // Thunder sounds
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 0.8f);

                struckTargets.add(target);
            }
        }

        // Environmental lightning for atmosphere
        for (int i = 0; i < 2; i++) {
            double angle = Math.random() * Math.PI * 2;
            double distance = 5 + Math.random() * (range - 5);
            Vec3 envStrike = userPos.add(
                    Math.cos(angle) * distance,
                    0,
                    Math.sin(angle) * distance
            );

            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(world);
            if (lightning != null) {
                lightning.moveTo(envStrike);
                lightning.setVisualOnly(true);
                serverLevel.addFreshEntity(lightning);
            }
        }
    }

    @Override
    protected void onStop() {
        struckTargets.clear();
    }
}