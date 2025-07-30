package com.xirc.nichirin.common.effect;

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
 * Stunned Status Effect - Completely immobilizes entities
 * Prevents all movement, attacks, and inputs for the duration
 */
public class StunnedStatusEffect extends MobEffect {

    // UUID for the movement speed modifier
    private static final UUID MOVEMENT_MODIFIER_UUID = UUID.fromString("9107DE5E-9CE8-5030-941E-514C1F160892");

    public StunnedStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0xFFD700); // Golden color for stun

        // Complete movement lockdown
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_MODIFIER_UUID.toString(),
                -1.0, // 100% movement speed reduction (complete immobilization)
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );

        // Prevent jumping for horses and other entities with jump strength
        this.addAttributeModifier(
                Attributes.JUMP_STRENGTH,
                "9207DE5E-9CE8-5030-941E-514C1F160893", // Different UUID
                -1.0, // 100% jump strength reduction
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        // Apply effect every tick for continuous immobilization
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Complete immobilization - force stop all movement
        entity.setDeltaMovement(Vec3.ZERO); // Stop all movement completely
        entity.hasImpulse = false; // Prevent any physics impulses

        // Prevent flying for players
        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
        }

        // Clear mob targeting and aggression
        if (entity instanceof Mob mob) {
            mob.setTarget(null);
            mob.setAggressive(false);
            mob.getNavigation().stop(); // Stop pathfinding
        }
    }
}