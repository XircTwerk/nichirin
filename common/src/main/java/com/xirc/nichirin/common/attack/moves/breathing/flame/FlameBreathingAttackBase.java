package com.xirc.nichirin.common.attack.moves.breathing.flame;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.common.vfx.VfxManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;

// Base for Flame Breathing attacks. All hits apply fire and burning effects.
@SuppressWarnings("rawtypes")
public abstract class FlameBreathingAttackBase extends AbstractBreathingAttack<FlameBreathingAttackBase, IBreathingAttacker> {

    // Flame-specific properties
    private static final int DEFAULT_FIRE_DURATION = 3; // 3 seconds of fire

    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;
        super.hitTarget(target);
        applyFireEffect(target);
        playFlameHitSound(target.position());
    }

    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;
        super.hitTargetNoImmunity(target);
        applyFireEffect(target);
        playFlameHitSound(target.position());
    }

    /**
     * Apply fire effect to target
     * Uses both direct fire application AND Burning status effect
     */
    protected void applyFireEffect(LivingEntity target) {
        // Skip fire immune entities (Blazes, Ghasts, etc.)
        if (target.fireImmune()) {
            return;
        }

        // Skip creative mode players - they shouldn't be affected by fire
        if (target instanceof Player player && player.isCreative()) {
            return;
        }

        // Calculate fire duration based on damage - but keep it reasonable
        // Max 8 seconds even for high damage attacks
        int fireSeconds = Math.min(8, Math.max(DEFAULT_FIRE_DURATION, (int)(damage / 4.0f)));

        // Apply direct fire (we know this works from debug)
        target.igniteForSeconds(fireSeconds);
        target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), fireSeconds * 20));

        int burningDurationTicks = fireSeconds * 20;
        target.addEffect(new MobEffectInstance(
                NichirinEffectRegistry.burning(),
                burningDurationTicks,
                0, false, true
        ));
    }

    protected void playFlameVfx(ResourceLocation effectId, Vec3 origin, Vec3 direction, float scale) {
        if (world instanceof ServerLevel serverLevel) {
            VfxManager.playAttached(serverLevel, user, effectId, origin, direction, scale);
        }
    }

    /** Anchored variant: the effect stays where it was cast instead of tracking the player. */
    protected void playFlameVfxAt(ResourceLocation effectId, Vec3 origin, Vec3 direction, float scale) {
        if (world instanceof ServerLevel serverLevel) {
            VfxManager.playOwned(serverLevel, user, effectId, origin, direction, scale);
        }
    }

    /**
     * Play flame-related sound effects
     */
    protected void playFlameSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 1.0f);
        }
    }

    /**
     * Play flame hit sound
     */
    protected void playFlameHitSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6f, 1.5f);
        }
    }

    /**
     * Play flame explosion sound
     */
    protected void playFlameExplosionSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    // Flame Breathing specific utility methods

    /**
     * Check if this attack creates an explosion
     */
    public boolean isExplosiveAttack() {
        // Override in specific attacks that create explosions
        return false;
    }

    /**
     * Check if this attack is a dash/mobility attack
     */
    public boolean isDashAttack() {
        return hasDash() || hasTeleport();
    }

    /**
     * Check if this attack hits in a 360-degree area
     */
    public boolean isOmnidirectional() {
        // Override in attacks like Blooming Flame Undulation
        return false;
    }

    /**
     * Get the fire duration this attack applies (in seconds)
     */
    public int getFireDuration() {
        return Math.max(DEFAULT_FIRE_DURATION, (int)(damage / 2.0f));
    }

    // Abstract methods that Flame attacks must implement
    // These are inherited from AbstractBreathingAttack but documented here for clarity

    /**
     * Called when the Flame attack starts (after breath consumption)
     * Implement Flame-specific startup effects here (particles, sounds, positioning)
     */
    @Override
    protected abstract void onStart();

    /**
     * Called every tick during the attack (after windup period)
     * Implement the main Flame attack logic here (movement, damage, effects)
     */
    @Override
    protected abstract void perform();

    /**
     * Called when the Flame attack ends (optional override)
     * Implement Flame-specific cleanup logic here
     */
    @Override
    protected void onStop() {
        super.onStop();
        // Additional Flame-specific cleanup can be added here
    }
}
