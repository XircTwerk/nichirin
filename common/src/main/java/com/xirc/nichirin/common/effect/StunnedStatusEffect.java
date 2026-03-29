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
 * Level 0 (amplifier 0): No movement reduction
 * Level 1+ (amplifier 1+): 95% movement reduction
 */
public class StunnedStatusEffect extends MobEffect {

    // UUID for the movement speed modifier
    private static final UUID MOVEMENT_MODIFIER_UUID = UUID.fromString("9107DE5E-9CE8-5030-941E-514C1F160892");

    // Track entities that were recently knocked back to allow their movement
    private static final Map<UUID, Long> recentKnockback = new HashMap<>();
    private static final int KNOCKBACK_GRACE_TICKS = 15; // Allow movement for 15 ticks after knockback

    public StunnedStatusEffect() {
        super(MobEffectCategory.NEUTRAL, 0xFFD700); // Golden color for stun

        // Note: We don't add attribute modifiers in constructor anymore
        // since we need to handle them dynamically based on amplifier level
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
    public void addAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        // Only apply movement reduction for amplifier 1+ (Level 2+)
        if (amplifier >= 1) {
            var attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attribute != null) {
                AttributeModifier modifier = new AttributeModifier(
                        MOVEMENT_MODIFIER_UUID,
                        "Stunned movement reduction",
                        -0.95, // 95% reduction
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                );
                attribute.addTransientModifier(modifier);
            }
        }
    }

    @Override
    public void removeAttributeModifiers(LivingEntity entity, net.minecraft.world.entity.ai.attributes.AttributeMap attributeMap, int amplifier) {
        var attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(MOVEMENT_MODIFIER_UUID);
        }
        // Restore building/flight abilities that were disabled during stun
        if (entity instanceof Player player) {
            player.getAbilities().mayBuild = true;
            if (player.isCreative() || player.isSpectator()) {
                player.getAbilities().mayfly = true;
            }
            player.onUpdateAbilities();
        }
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

        // Only restrict movement for amplifier 1+ (Level 2+) and when not in grace period
        if (amplifier >= 1 && !inGracePeriod) {
            Vec3 currentMovement = entity.getDeltaMovement();

            // Only restrict horizontal movement that's likely from input/AI
            // Allow significant movement (likely from knockback) and preserve Y movement always
            double threshold = 0.3; // Movement below this is considered input-based

            double newX = Math.abs(currentMovement.x) > threshold ? currentMovement.x : 0;
            double newZ = Math.abs(currentMovement.z) > threshold ? currentMovement.z : 0;

            entity.setDeltaMovement(newX, currentMovement.y, newZ);
        }

        // Player-specific restrictions (apply regardless of amplifier level)
        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            // Prevent flying only (not building - setting mayBuild false permanently breaks interaction)
            player.getAbilities().mayfly = false;
        }

        // Mob-specific restrictions (apply regardless of amplifier level)
        if (entity instanceof Mob mob) {
            // Disable AI but don't interfere with physics
            mob.setTarget(null);
            mob.setAggressive(false);
            mob.getNavigation().stop();
        }
    }
}