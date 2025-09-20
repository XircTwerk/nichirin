package com.xirc.nichirin.common.attack.moves.breathing.water;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Water Surface Slash Attack with 3-hit combo system
 * Stage 1: Left to right slash (no knockback)
 * Stage 2: Right to left slash (no knockback)
 * Stage 3: Downward slam (high damage, knockback, stun)
 *
 * All configuration comes from the moveset builder.
 * This class handles behavior and visual/audio effects for all 3 stages.
 * Uses hitTargetNoImmunity to bypass immunity frames for rapid combo hits.
 */
public class WaterSurfaceSlashAttack extends WaterBreathingAttackBase {

    private boolean slashExecuted = false;
    private int comboStage = 1; // 1 = left-to-right, 2 = right-to-left, 3 = downward slam
    private int comboTimer = 0;
    private boolean isComboActive = false;

    public WaterSurfaceSlashAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
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
            // Allow combo window for 15 ticks (0.75 seconds)
            isComboActive = false;
        }
    }

    private void executeSlash() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Different slash effects based on combo stage
        switch (comboStage) {
            case 1 -> {
                createLeftToRightSlash(userPos, lookDir);
            }
            case 2 -> {
                createRightToLeftSlash(userPos, lookDir);
            }
            case 3 -> {
                createDownwardSlam(userPos, lookDir);
            }
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

    private void createLeftToRightSlash(Vec3 userPos, Vec3 lookDir) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Calculate perpendicular vector for horizontal slashing
        Vec3 rightVector = lookDir.cross(new Vec3(0, 1, 0)).normalize();

        // Move particle spawn point away from player (in front of them)
        Vec3 slashCenter = userPos.add(lookDir.scale(range * 0.5)).add(0, 0.2, 0);

        // Create timed slash from LEFT TO RIGHT with arc effect
        int totalParticles = 12;
        float arcHeight = 0.8f; // Height of the arc

        for (int i = 0; i < totalParticles; i++) {
            int finalI = i;

            // Schedule each particle with a small delay to create motion effect
            CompletableFuture.delayedExecutor(i * 20L, TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        if (world instanceof ServerLevel level) {
                            float progress = (float) finalI / (totalParticles - 1); // 0.0 to 1.0

                            // Create arc motion: start left, peak in middle, end right
                            float horizontalOffset = (progress - 0.5f) * range * 1.4f; // -range*0.7 to +range*0.7
                            float forwardOffset = arcHeight * (1.0f - 4.0f * (progress - 0.5f) * (progress - 0.5f)); // Parabolic arc forward

                            Vec3 particlePos = slashCenter
                                    .add(rightVector.scale(horizontalOffset))
                                    .add(lookDir.scale(forwardOffset)); // Arc forward instead of up

                            // More particles at the peak of the arc for emphasis
                            int particleCount = (progress > 0.3f && progress < 0.7f) ? 6 : 4;

                            level.sendParticles(ParticleTypes.SPLASH,
                                    particlePos.x, particlePos.y, particlePos.z,
                                    particleCount, 0.15, 0.1, 0.15, 0.12);

                            level.sendParticles(ParticleTypes.DRIPPING_WATER,
                                    particlePos.x, particlePos.y, particlePos.z,
                                    2, 0.08, 0.05, 0.08, 0.08);
                        }
                    });
        }

        // Add immediate initial splash at start position (left side)
        Vec3 startPos = slashCenter.add(rightVector.scale(-range * 0.7));
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                startPos.x, startPos.y, startPos.z,
                4, 0.1, 0.1, 0.1, 0.1);
    }

    private void createRightToLeftSlash(Vec3 userPos, Vec3 lookDir) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Calculate perpendicular vector for horizontal slashing
        Vec3 rightVector = lookDir.cross(new Vec3(0, 1, 0)).normalize();

        // Move particle spawn point away from player
        Vec3 slashCenter = userPos.add(lookDir.scale(range * 0.5)).add(0, 0.3, 0);

        // Create timed slash from RIGHT TO LEFT with deeper arc
        int totalParticles = 14;
        float arcHeight = 1.0f; // Slightly higher arc for second hit

        for (int i = 0; i < totalParticles; i++) {
            int finalI = i;

            // Slightly faster timing for second hit
            CompletableFuture.delayedExecutor(i * 15L, TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        if (world instanceof ServerLevel level) {
                            float progress = (float) finalI / (totalParticles - 1); // 0.0 to 1.0

                            // Create arc motion: start right, peak in middle, end left
                            float horizontalOffset = (0.5f - progress) * range * 1.4f; // +range*0.7 to -range*0.7
                            float forwardOffset = arcHeight * (1.0f - 4.0f * (progress - 0.5f) * (progress - 0.5f)); // Parabolic arc forward

                            Vec3 particlePos = slashCenter
                                    .add(rightVector.scale(horizontalOffset))
                                    .add(lookDir.scale(forwardOffset)); // Arc forward instead of up

                            // Enhanced particles for second hit
                            int particleCount = (progress > 0.25f && progress < 0.75f) ? 7 : 5;

                            level.sendParticles(ParticleTypes.SPLASH,
                                    particlePos.x, particlePos.y, particlePos.z,
                                    particleCount, 0.18, 0.12, 0.18, 0.15);

                            level.sendParticles(ParticleTypes.DRIPPING_WATER,
                                    particlePos.x, particlePos.y, particlePos.z,
                                    3, 0.1, 0.08, 0.1, 0.1);
                        }
                    });
        }

        // Add immediate initial splash at start position (right side)
        Vec3 startPos = slashCenter.add(rightVector.scale(range * 0.7));
        serverLevel.sendParticles(ParticleTypes.SPLASH,
                startPos.x, startPos.y, startPos.z,
                5, 0.12, 0.1, 0.12, 0.12);
    }

    private void createDownwardSlam(Vec3 userPos, Vec3 lookDir) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        // Move slam effect in front of player
        Vec3 slamCenter = userPos.add(lookDir.scale(range * 0.6));

        // Create timed vertical slash from UP TO DOWN
        int totalParticles = 16;
        float maxHeight = 2.5f;

        for (int i = 0; i < totalParticles; i++) {
            int finalI = i;

            // Faster timing for slam effect
            CompletableFuture.delayedExecutor(i * 25L, TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        if (world instanceof ServerLevel level) {
                            float progress = (float) finalI / (totalParticles - 1); // 0.0 to 1.0

                            // Vertical motion from top to bottom
                            float verticalOffset = maxHeight * (1.0f - progress); // Start high, end low

                            Vec3 particlePos = slamCenter.add(0, verticalOffset, 0);

                            // More particles as we get closer to impact
                            int particleCount = (int)(4 + progress * 8); // 4-12 particles

                            if (progress < 0.8f) {
                                // Falling water particles for most of the descent
                                level.sendParticles(ParticleTypes.FALLING_WATER,
                                        particlePos.x, particlePos.y, particlePos.z,
                                        particleCount, 0.2, 0.1, 0.2, 0.15);
                            } else {
                                // Heavy splash particles near impact
                                level.sendParticles(ParticleTypes.SPLASH,
                                        particlePos.x, particlePos.y, particlePos.z,
                                        particleCount, 0.4, 0.2, 0.4, 0.25);
                            }
                        }
                    });
        }

        // Delayed massive ground impact effect
        CompletableFuture.delayedExecutor(totalParticles * 25L + 100L, TimeUnit.MILLISECONDS)
                .execute(() -> {
                    if (world instanceof ServerLevel level) {
                        Vec3 impactPos = slamCenter.add(0, -0.5, 0);

                        // Massive ground splash
                        level.sendParticles(ParticleTypes.SPLASH,
                                impactPos.x, impactPos.y, impactPos.z,
                                30, 1.5, 0.3, 1.5, 0.4);

                        // Radial splash pattern
                        for (int angle = 0; angle < 360; angle += 20) {
                            double radians = Math.toRadians(angle);
                            double offsetX = Math.cos(radians) * 2.0;
                            double offsetZ = Math.sin(radians) * 2.0;

                            level.sendParticles(ParticleTypes.SPLASH,
                                    impactPos.x + offsetX, impactPos.y, impactPos.z + offsetZ,
                                    4, 0.2, 0.15, 0.2, 0.15);
                        }

                        // Upward water columns
                        for (int column = 0; column < 8; column++) {
                            float height = 0.3f + column * 0.25f;
                            level.sendParticles(ParticleTypes.SPLASH,
                                    impactPos.x, impactPos.y + height, impactPos.z,
                                    3, 0.15, 0.1, 0.15, 0.1);
                        }
                    }
                });

        // Immediate startup particles at the top
        Vec3 startPos = slamCenter.add(0, maxHeight, 0);
        serverLevel.sendParticles(ParticleTypes.FALLING_WATER,
                startPos.x, startPos.y, startPos.z,
                6, 0.1, 0.1, 0.1, 0.1);
    }

    @Override
    protected void onStop() {

        // Reset state
        slashExecuted = false;
        isComboActive = false;

        // Final effect based on combo stage
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            if (comboStage == 3) {
                // Larger final splash for slam finisher
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        userPos.x, userPos.y, userPos.z,
                        15, 0.5, 0.5, 0.5, 0.2);
            } else {
                // Standard final splash
                serverLevel.sendParticles(ParticleTypes.SPLASH,
                        userPos.x, userPos.y, userPos.z,
                        8, 0.3, 0.3, 0.3, 0.1);
            }
        }
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
        if (isComboActive() && comboStage < 3) {
        }
    }
}