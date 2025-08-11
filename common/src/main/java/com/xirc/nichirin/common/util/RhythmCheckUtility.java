package com.xirc.nichirin.common.util;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

/**
 * Generic rhythm checking utility for all attacks during Musical Score
 * Keeps Musical Score logic separate from individual weapon classes
 */
public class RhythmCheckUtility {

    public enum TimingQuality {
        PERFECT("PERFECT!", 0xFF00FF00, 2.0f, true),
        GOOD("Good", 0xFFFFFF00, 1.5f, false),
        OFF_BEAT("Miss", 0xFFFF0000, 1.0f, false),
        NONE("", 0xFFFFFFFF, 1.0f, false);

        private final String displayName;
        private final int color;
        private final float damageMultiplier;
        private final boolean noCooldown;

        TimingQuality(String displayName, int color, float damageMultiplier, boolean noCooldown) {
            this.displayName = displayName;
            this.color = color;
            this.damageMultiplier = damageMultiplier;
            this.noCooldown = noCooldown;
        }

        public String getDisplayName() { return displayName; }
        public int getColor() { return color; }
        public float getDamageMultiplier() { return damageMultiplier; }
        public boolean allowsNoCooldown() { return noCooldown; }
    }

    public static class RhythmResult {
        public final boolean canAttack;
        public final float damageMultiplier;
        public final TimingQuality timing;

        public RhythmResult(boolean canAttack, float damageMultiplier, TimingQuality timing) {
            this.canAttack = canAttack;
            this.damageMultiplier = damageMultiplier;
            this.timing = timing;
        }
    }

    /**
     * Check rhythm timing for any player attack
     */
    public static RhythmResult checkRhythmTiming(Player player) {
        // Only check if Musical Score is active
        if (!player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get())) {
            return new RhythmResult(true, 1.0f, TimingQuality.NONE);
        }

        // Get timing quality from Musical Score effect
        TimingQuality timing;
        if (player.level().isClientSide) {
            // Use client-side rhythm meter if available
            try {
                var rhythmTiming = com.xirc.nichirin.client.gui.RhythmMeter.getCurrentTiming();
                timing = convertToUtilityTiming(rhythmTiming);
            } catch (Exception e) {
                timing = TimingQuality.OFF_BEAT;
            }
        } else {
            // Server-side fallback
            if (com.xirc.nichirin.common.effect.MusicalScoreEffect.allowsNoCooldown(player)) {
                timing = TimingQuality.PERFECT;
            } else {
                timing = TimingQuality.OFF_BEAT;
            }
        }

        boolean canAttack = timing != TimingQuality.OFF_BEAT;
        float damageMultiplier = timing.getDamageMultiplier();

        return new RhythmResult(canAttack, damageMultiplier, timing);
    }

    /**
     * Convert RhythmMeter timing to utility timing
     */
    private static TimingQuality convertToUtilityTiming(com.xirc.nichirin.client.gui.RhythmMeter.TimingQuality rhythmTiming) {
        switch (rhythmTiming) {
            case PERFECT: return TimingQuality.PERFECT;
            case GOOD: return TimingQuality.GOOD;
            case OFF_BEAT: return TimingQuality.OFF_BEAT;
            default: return TimingQuality.NONE;
        }
    }

    /**
     * Play rhythm feedback sounds and apply effects
     */
    public static void playRhythmFeedback(Player player, TimingQuality timing) {
        if (timing == TimingQuality.NONE) return;

        float pitch, volume;

        switch (timing) {
            case PERFECT:
                pitch = 2.0f; // High pitch for perfect
                volume = 1.0f;

                // Extend Musical Score duration
                if (!player.level().isClientSide) {
                    com.xirc.nichirin.common.effect.MusicalScoreEffect.extendForPerfectTiming(player);
                }

                // Trigger client-side success feedback
                if (player.level().isClientSide) {
                    try {
                        com.xirc.nichirin.client.gui.RhythmMeter.triggerSuccess();
                    } catch (Exception e) {
                        // Ignore if RhythmMeter not available
                    }
                }
                break;

            case GOOD:
                pitch = 1.5f; // Medium pitch for good
                volume = 0.8f;
                break;

            case OFF_BEAT:
                pitch = 0.5f; // Low pitch for miss
                volume = 0.6f;

                // Reduce Musical Score duration on miss
                if (!player.level().isClientSide) {
                    com.xirc.nichirin.common.effect.MusicalScoreEffect.reduceForMissedTiming(player);
                }
                break;

            default:
                return;
        }

        // Play note block sound with appropriate pitch/volume
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, volume, pitch);
    }

    /**
     * Show timing feedback to player
     */
    public static void showTimingFeedback(Player player, TimingQuality timing, float damageMultiplier) {
        if (timing == TimingQuality.NONE) return;

        if (timing == TimingQuality.OFF_BEAT) {
            player.displayClientMessage(
                    Component.literal("Missed the rhythm! Attack blocked!")
                            .withStyle(style -> style.withColor(0xFF3333)),
                    true
            );
        } else if (damageMultiplier != 1.0f) {
            player.displayClientMessage(
                    Component.literal(String.format("%.1fx Damage! (%s)",
                                    damageMultiplier, timing.getDisplayName()))
                            .withStyle(style -> style.withColor(timing.getColor())),
                    true
            );
        }
    }

    /**
     * Apply damage multiplier to any attack object that supports it
     */
    public static void applyDamageMultiplier(Object attack, float multiplier) {
        if (multiplier == 1.0f) return; // No change needed

        try {
            // Try to find and call setDamageMultiplier method
            var setMethod = attack.getClass().getMethod("setDamageMultiplier", float.class);
            setMethod.invoke(attack, multiplier);
        } catch (Exception e1) {
            try {
                // Try to find and modify damage field directly
                var damageField = attack.getClass().getDeclaredField("damage");
                damageField.setAccessible(true);
                float currentDamage = damageField.getFloat(attack);
                damageField.setFloat(attack, currentDamage * multiplier);
            } catch (Exception e2) {
                // If we can't apply multiplier, just log it
                System.out.println("Could not apply damage multiplier to " + attack.getClass().getSimpleName());
            }
        }
    }
}