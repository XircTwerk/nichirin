package com.xirc.nichirin.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stunned Status Effect - Prevents movement and actions but allows knockback
 * Prevents player input and mob AI but preserves physics-based movement
 */
public class StunnedStatusEffect extends MobEffect {

    // UUID for the movement speed modifier
    private static final UUID MOVEMENT_MODIFIER_UUID = UUID.fromString("9107DE5E-9CE8-5030-941E-514C1F160892");

    // Track entities that were recently knocked back to allow their movement
    private static final Map<UUID, Long> recentKnockback = new HashMap<>();
    private static final int KNOCKBACK_GRACE_TICKS = 15; // Allow movement for 15 ticks after knockback

    public StunnedStatusEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFD700); // Golden color for stun

        // Reduce movement speed significantly but don't eliminate it completely
        this.addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_MODIFIER_UUID.toString(),
                -0.95, // 95% reduction instead of 100%
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
    }

    /**
     * Mark an entity as recently knocked back to allow their movement temporarily
     */
    public static void markRecentKnockback(LivingEntity entity) {
        recentKnockback.put(entity.getUUID(), entity.level().getGameTime() + KNOCKBACK_GRACE_TICKS);
    }

    /**
     * Clean up expired knockback grace periods
     */
    public static void cleanupExpiredGracePeriods(LivingEntity entity) {
        Long graceEnd = recentKnockback.get(entity.getUUID());
        if (graceEnd != null && entity.level().getGameTime() >= graceEnd) {
            recentKnockback.remove(entity.getUUID());
        }
    }

    /**
     * Remove knockback grace period for an entity
     */
    public static void removeKnockbackGrace(LivingEntity entity) {
        recentKnockback.remove(entity.getUUID());
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true; // Apply every tick
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Check if entity is in knockback grace period
        Long graceEnd = recentKnockback.get(entity.getUUID());
        boolean inGracePeriod = graceEnd != null && entity.level().getGameTime() < graceEnd;

        // Clean up expired grace periods
        if (graceEnd != null && entity.level().getGameTime() >= graceEnd) {
            recentKnockback.remove(entity.getUUID());
            inGracePeriod = false;
        }

        // If not in grace period, restrict movement
        if (!inGracePeriod) {
            Vec3 currentMovement = entity.getDeltaMovement();

            // Only restrict horizontal movement that's likely from input/AI
            // Allow significant movement (likely from knockback) and preserve Y movement always
            double threshold = 0.3; // Movement below this is considered input-based

            double newX = Math.abs(currentMovement.x) > threshold ? currentMovement.x : 0;
            double newZ = Math.abs(currentMovement.z) > threshold ? currentMovement.z : 0;

            entity.setDeltaMovement(newX, currentMovement.y, newZ);
        }

        // Player-specific restrictions
        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            // Prevent flying
            player.getAbilities().flying = false;
        }

        // Mob-specific restrictions
        if (entity instanceof Mob mob) {
            // Disable AI but don't interfere with physics
            mob.setTarget(null);
            mob.setAggressive(false);
            mob.getNavigation().stop();
        }
    }
}