package com.xirc.nichirin.common.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Venom Status Effect - Stackable poison that bypasses immunity
 * - Always 33% speed reduction regardless of stack level
 * - Damage scales with stacks: 1 base damage + 10% more per tier
 * - Purple butterfly aura that gets more intense with stacks
 */
public class VenomStatusEffect extends MobEffect {

    private static final UUID MOVEMENT_MODIFIER_UUID = UUID.fromString("9107DE5E-9CE8-5030-941E-514C1F160892");
    private static final float BASE_DAMAGE = 1.0f;
    private static final float DAMAGE_MULTIPLIER_PER_TIER = 0.1f; // 10% more damage per tier

    public VenomStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0x9932CC); // Purple color

        // ALWAYS 33% speed reduction regardless of amplifier level
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_MODIFIER_UUID.toString(),
                -0.33,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Skip creative players
        if (entity instanceof Player player && player.isCreative()) {
            return;
        }

        // Calculate damage: Base damage * (1 + 10% per tier)
        // Tier 0 (amplifier 0): 1.0 * (1 + 0.0) = 1.0 damage
        // Tier 1 (amplifier 1): 1.0 * (1 + 0.1) = 1.1 damage
        // Tier 2 (amplifier 2): 1.0 * (1 + 0.2) = 1.2 damage
        // Tier 3 (amplifier 3): 1.0 * (1 + 0.3) = 1.3 damage
        // etc.
        float damageMultiplier = 1.0f + (amplifier * DAMAGE_MULTIPLIER_PER_TIER);
        float damage = BASE_DAMAGE * damageMultiplier;

        // Apply magic damage (bypasses armor like poison)
        entity.hurt(entity.damageSources().magic(), damage);

        // Add particles that scale with stack intensity
        if (entity.level() instanceof ServerLevel serverLevel) {
            createStackedVenomParticles(serverLevel, entity, amplifier + 1);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Damage every second (20 ticks) regardless of stack level
        return duration % 20 == 0;
    }

    private void createStackedVenomParticles(ServerLevel level, LivingEntity entity, int stacks) {
        Vec3 pos = entity.position().add(0, entity.getBbHeight() / 2, 0);

        // More particles with higher stacks
        int particleCount = Math.min(6 + stacks * 2, 20); // 6-20 particles based on stacks

        // Purple butterfly ring that gets denser with stacks
        for (int i = 0; i < particleCount; i++) {
            double angle = (entity.tickCount + i * (360.0 / particleCount)) * 0.1;
            double radius = 1.2;
            double x = pos.x + Math.cos(angle) * radius;
            double z = pos.z + Math.sin(angle) * radius;
            double y = pos.y + Math.sin(angle * 2) * 0.5;

            level.sendParticles(ParticleTypes.WITCH, x, y, z, 1, 0.1, 0.1, 0.1, 0.05);

            // More portal particles for higher stacks
            if (i % (Math.max(1, 4 - stacks / 3)) == 0) {
                level.sendParticles(ParticleTypes.PORTAL, x, y, z, 1, 0.05, 0.05, 0.05, 0.02);
            }
        }

        // Central venom cloud for high stacks
        if (stacks >= 5) {
            level.sendParticles(ParticleTypes.WITCH,
                    pos.x, pos.y, pos.z, stacks / 2, 0.3, 0.3, 0.3, 0.1);
        }
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        // ALWAYS return -0.33 (33% reduction) regardless of amplifier
        return -0.33;
    }
}