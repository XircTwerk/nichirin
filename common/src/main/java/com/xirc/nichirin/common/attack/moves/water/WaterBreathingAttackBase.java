package com.xirc.nichirin.common.attack.moves.water;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Base class for Thunder Breathing attacks
 * Now extends AbstractBreathingAttack for unified interface
 *
 * Thunder-specific functionality includes special hit effects and electrical visual effects
 *
 * IMPORTANT: All visual effects (particles, sounds) and audio should be handled
 * in the individual attack classes, NOT in the builder configuration.
 * The builder only handles combat stats, timing, and resources.
 */
public abstract class WaterBreathingAttackBase extends AbstractBreathingAttack<WaterBreathingAttackBase, IBreathingAttacker<?, ?>> {

    /**
     * Apply Thunder Breathing specific damage and effects to a target
     * Overrides the base hitTarget to add Thunder-specific effects
     */
    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;

        // Apply base damage and knockback
        super.hitTarget(target);

        // Apply Thunder Breathing specific "shocked" effect
        if (hitStun > 0) {
            target.addEffect(new MobEffectInstance(
                    NichirinEffectRegistry.SHOCKED.get(),
                    hitStun,
                    0, // Amplifier 0 (effect only has 1 level)
                    false, // Ambient
                    true   // Show particles
            ));
        }
    }

    /**
     * Special Thunder Breathing hit method that removes immunity frames
     * Used for attacks like Rice Spirit that should always hit with rapid strikes
     */
    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;

        // Apply base no-immunity hit
        super.hitTargetNoImmunity(target);



    }

    /**
     * Check if this attack uses dash mechanics
     * Thunder Breathing features high-speed dash attacks
     */
    public boolean isDashAttack() {
        return hasDash();
    }

    /**
     * Get the effective speed multiplier for this Thunder attack
     * Based on whether it's a teleport, dash, or normal attack
     */
    public float getSpeedMultiplier() {
        if (hasTeleport()) {
            return Float.MAX_VALUE; // Instant movement
        } else if (hasDash() && dashSpeed != null) {
            return dashSpeed;
        } else {
            return 1.0f; // Normal speed
        }
    }

    /**
     * Get the effective windup time, accounting for teleport-specific timing
     */
    public int getEffectiveWindup() {
        if (hasTeleportWindup() && teleportWindup != null) {
            return teleportWindup;
        }
        return windup;
    }




    @Override
    protected abstract void onStart();

    /**
     * Called every tick during the attack (after windup period)
     * Implement the main Thunder attack logic here (movement, damage, effects)
     */
    @Override
    protected abstract void perform();

    /**
     * Called when the Thunder attack ends (optional override)
     * Implement Thunder-specific cleanup logic here
     */
    @Override
    protected void onStop() {
        super.onStop();
        // Additional Thunder-specific cleanup can be added here
    }
}