package com.xirc.nichirin.common.effect;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Shocked Status Effect - Applied by Thunder Breathing techniques
 * Immobilizes enemies with a 75% movement speed reduction and prevents most actions
 */
public class ShockedStatusEffect extends MobEffect {

    // UUID for the movement speed modifier
    private static final UUID MOVEMENT_MODIFIER_UUID = UUID.fromString("7107DE5E-7CE8-4030-940E-514C1F160890");

    public ShockedStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFFF00); // Yellow color for thunder

        // Add attribute modifiers
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_MODIFIER_UUID.toString(),
                -0.75, // 75% reduction
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Apply effect every tick for continuous particles and movement restriction
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Restrict movement but not rotation
        Vec3 motion = entity.getDeltaMovement();

        // Heavily reduce horizontal movement (on top of the attribute modifier)
        double horizontalMultiplier = 0.1; // Additional 90% reduction for near-immobilization
        entity.setDeltaMovement(
                motion.x * horizontalMultiplier,
                Math.min(motion.y, 0.0), // Prevent jumping but allow falling
                motion.z * horizontalMultiplier
        );

        // Prevent flying for players
        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
        }

        // Clear mob targeting and aggression
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.setAggressive(false);
        }

        // Spawn thunder particles around the entity
        if (!entity.level().isClientSide() && entity.level() instanceof ServerLevel serverLevel) {
            spawnThunderParticles(serverLevel, entity);
        }
    }

    private void spawnThunderParticles(ServerLevel level, LivingEntity entity) {
        // Spawn particles every 2 ticks to avoid overwhelming
        if (entity.tickCount % 2 == 0) {
            double entityHeight = entity.getBbHeight();
            double entityWidth = entity.getBbWidth();

            // Spawn 3-5 particles at random positions around the entity
            int particleCount = 1 + level.random.nextInt(3);

            for (int i = 0; i < particleCount; i++) {
                // Random position around the entity
                double offsetX = (level.random.nextDouble() - 0.5) * entityWidth * 1.5;
                double offsetY = level.random.nextDouble() * entityHeight;
                double offsetZ = (level.random.nextDouble() - 0.5) * entityWidth * 1.5;

                level.sendParticles(
                        NichirinParticleRegistry.THUNDER.get(),
                        entity.getX() + offsetX,
                        entity.getY() + offsetY,
                        entity.getZ() + offsetZ,
                        1, // count
                        0.0, // xSpeed
                        0.0, // ySpeed
                        0.0, // zSpeed
                        0.0  // speed
                );
            }

            // Occasionally spawn a larger burst
            if (entity.tickCount % 10 == 0) {
                level.sendParticles(
                        NichirinParticleRegistry.THUNDER.get(),
                        entity.getX(),
                        entity.getY() + entityHeight / 2,
                        entity.getZ(),
                        1, // count
                        entityWidth * 0.5, // xOffset
                        entityHeight * 0.5, // yOffset
                        entityWidth * 0.5, // zOffset
                        0.001 // speed
                );
            }
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        super.removeAttributeModifiers(entity, attributeMap, amplifier);

        // Spawn a final burst of particles when effect ends
        if (!entity.level().isClientSide() && entity.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    NichirinParticleRegistry.THUNDER.get(),
                    entity.getX(),
                    entity.getY() + entity.getBbHeight() / 2,
                    entity.getZ(),
                    5,
                    0.3,
                    0.3,
                    0.3,
                    0.01
            );
        }
    }
}