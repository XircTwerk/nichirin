package com.xirc.nichirin.common.attack.moves.breathing.water;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.xirc.nichirin.common.vfx.VfxIds;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Water Surface Slash Attack with 3-hit combo system
 * Stage 1: Left to right slash (no knockback)
 * Stage 2: Right to left slash (no knockback)
 * Stage 3: Downward slam (high damage, knockback, stun)
 *
 * This class handles behavior and visual/audio effects for all 3 stages.
 * Uses hitTargetNoImmunity to bypass immunity frames for rapid combo hits.
 */
public class WaterSurfaceSlashAttack extends WaterBreathingAttackBase {

    private boolean slashExecuted = false;
    private int comboStage = 1; // 1 = left-to-right, 2 = right-to-left, 3 = downward slam
    private int comboTimer = 0;
    private boolean isComboActive = false;

    public WaterSurfaceSlashAttack() {
    }

    /**
     * Set the combo stage for this attack
     * @param stage 1 = left-to-right, 2 = right-to-left, 3 = downward slam
     */
    public void setComboStage(int stage) {
        this.comboStage = Math.max(1, Math.min(3, stage)); // Clamp between 1-3
    }

    @Override
    protected void onStart() {
        slashExecuted = false;
        comboTimer = 0;
        isComboActive = true;
    }

    @Override
    protected void onActiveStart() {
        // Quick water slash sound
        playWaterSlashSound();

        SoundEvent startupSound = comboStage == 3 ? SoundEvents.WATER_AMBIENT : SoundEvents.WATER_AMBIENT;
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                startupSound, SoundSource.PLAYERS, 0.6f, 1.0f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        comboTimer++;

        // Execute slash immediately (no windup)
        if (!slashExecuted && comboTimer == windup + 1) {
            executeSlash();
            slashExecuted = true;
        }

        // For stages 1 and 2, check for combo continuation after a brief window
        if (isComboActive && (comboStage == 1 || comboStage == 2) && comboTimer >= 15) {
            // Keep the staged attack ready for a followup for 15 ticks (0.75 seconds).
            isComboActive = false;
        }
    }

    private void executeSlash() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 effectOrigin = user.position().add(lookDir.scale(1.25)).add(0, 0.08, 0);
        if (comboStage == 3) {
            playWaterVfxAt(VfxIds.WATERFALL_BASIN, effectOrigin, lookDir, 0.8f);
        } else {
            playWaterVfx(comboStage == 2 ? VfxIds.WATER_SURFACE_SLASH_REVERSE : VfxIds.WATER_SURFACE_SLASH,
                    effectOrigin, lookDir,
                    comboStage == 2 ? 1.12f : 1.0f);
        }

        // Hit enemies in front - use hitTargetNoImmunity for combo attacks
        List<LivingEntity> targets = getTargetsAtRange();

        for (LivingEntity target : targets) {
            // Use hitTargetNoImmunity to bypass immunity frames for rapid combo
            hitTargetNoImmunity(target);

            // Apply knockback only for slam (stage 3)
            if (comboStage == 3) {
                Vec3 knockbackDir = target.position().subtract(userPos).normalize();
                target.push(knockbackDir.x * knockback, 0.3, knockbackDir.z * knockback);
            }

            // Individual hit sound - different for slam
            SoundEvent hitSound = comboStage == 3 ? SoundEvents.PLAYER_SPLASH_HIGH_SPEED : SoundEvents.PLAYER_SPLASH;
            float hitPitch = comboStage == 3 ? 0.8f : 1.4f;
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    hitSound, SoundSource.PLAYERS, 0.8f, hitPitch);
        }

        // Main slash sound - different for slam
        SoundEvent attackSound = comboStage == 3 ? SoundEvents.ANVIL_LAND : SoundEvents.PLAYER_ATTACK_SWEEP;
        float attackPitch = comboStage == 3 ? 1.5f : 1.2f;
        float attackVolume = comboStage == 3 ? 1.5f : 1.0f;

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                attackSound, SoundSource.PLAYERS, attackVolume, attackPitch);
    }


    @Override
    protected void onStop() {

        // Reset state
        slashExecuted = false;
        isComboActive = false;

    }

    /**
     * Get the current combo stage
     */
    public int getComboStage() {
        return comboStage;
    }

    /**
     * Check if this is the final hit in the combo
     */
    public boolean isFinalHit() {
        return comboStage == 3;
    }

    /**
     * Check if combo is still active for chaining
     */
    public boolean isComboActive() {
        return isComboActive && (comboStage == 1 || comboStage == 2);
    }

    /**
     * Trigger the next combo stage manually (for manual combo system)
     */
    public void triggerNextStage() {
    }
}
