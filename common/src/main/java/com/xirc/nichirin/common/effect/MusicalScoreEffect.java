package com.xirc.nichirin.common.effect;

import com.xirc.nichirin.common.data.PlayerDataProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

/**
 * Musical Score - Tengen's ultimate ability
 * Activated when user has Sound Breathing and is at low health (1 heart)
 * Provides massive stat boosts and rhythm-based damage multipliers
 * Can be used multiple times but death save is once per life
 */
public class MusicalScoreEffect extends MobEffect {

    // Players who have used their death save (once per life)
    private static final Set<UUID> playersUsedDeathSave = new HashSet<>();

    // Effect parameters
    private static final int EFFECT_DURATION = 200; // 10 seconds (200 ticks)
    private static final int MAX_DURATION = 320; // 16 seconds maximum
    private static final float BASE_DAMAGE_MULTIPLIER = 3.0f; // 3x base damage

    public MusicalScoreEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700); // Gold color

        // Massive stat boosts
        addAttributeModifier(Attributes.ATTACK_DAMAGE, "7107DE5E-7CE8-4030-940E-514C1F160890",
                2.0, AttributeModifier.Operation.MULTIPLY_TOTAL); // +200% attack damage
        addAttributeModifier(Attributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160891",
                1.0, AttributeModifier.Operation.MULTIPLY_TOTAL); // +100% movement speed
        addAttributeModifier(Attributes.ATTACK_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160892",
                1.5, AttributeModifier.Operation.MULTIPLY_TOTAL); // +150% attack speed
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            // Auto-end if player doesn't have Sound Breathing
            if (!hasSoundBreathing(player)) {
                player.removeEffect(this);
                if (!player.level().isClientSide) {
                    com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Removing Musical Score from {} - no Sound Breathing", player.getName().getString());
                }
                return;
            }

            // Note: Breath restoration would go here if BreathData class exists
            // For now, Musical Score just provides the stat boosts and rhythm bonuses
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true; // Apply every tick
    }

    /**
     * Check if player can activate Musical Score (no cooldown check)
     */
    public static boolean canActivate(Player player) {
        // No cooldown - only check if they have Sound Breathing
        return hasSoundBreathing(player);
    }

    /**
     * Check if player can use death save (once per life)
     */
    public static boolean canActivateForDeathSave(Player player) {
        return hasSoundBreathing(player) && !hasUsedDeathSave(player);
    }

    /**
     * Activate Musical Score for a player
     */
    public static boolean activate(Player player) {
        if (!canActivate(player)) {
            return false;
        }

        if (!player.level().isClientSide) {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score activating for {} (death save: false, no cooldown)", player.getName().getString());
        }

        // Apply the effect
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get(),
                EFFECT_DURATION,
                0,
                false,
                true,
                true
        ));

        if (!player.level().isClientSide) {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score activated successfully for {} (no cooldown)", player.getName().getString());
        }

        return true;
    }

    /**
     * Special activation for death save (once per life)
     */
    public static boolean activateForDeathSave(Player player) {
        if (!canActivateForDeathSave(player)) {
            if (!player.level().isClientSide) {
                if (hasUsedDeathSave(player)) {
                    com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score death save cannot activate for {} - already used this life", player.getName().getString());
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Death save can only be used once per life"));
                } else {
                    com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score death save cannot activate for {} - no Sound Breathing", player.getName().getString());
                }
            }
            return false;
        }

        if (!player.level().isClientSide) {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score activating for {} (death save: true, no cooldown)", player.getName().getString());
        }

        // Mark death save as used
        playersUsedDeathSave.add(player.getUUID());

        if (!player.level().isClientSide) {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score death save used by {}. Cannot be used again this life.", player.getName().getString());
        }

        // Apply the effect
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get(),
                EFFECT_DURATION,
                0,
                false,
                true,
                true
        ));

        if (!player.level().isClientSide) {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score activated successfully for {} (no cooldown)", player.getName().getString());
            com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score death save activated for {}! Cannot be used again this life.", player.getName().getString());
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Death save can only be used once per life"));
        }

        return true;
    }

    /**
     * Extend Musical Score duration for perfect timing (+2 seconds, max 16 seconds)
     */
    public static void extendForPerfectTiming(Player player) {
        var effect = player.getEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get());
        if (effect != null) {
            int currentDuration = effect.getDuration();
            int newDuration = Math.min(currentDuration + 40, MAX_DURATION); // +2 seconds, max 16 seconds

            if (currentDuration < MAX_DURATION) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get(),
                        newDuration,
                        0,
                        false,
                        true,
                        true
                ));

                if (!player.level().isClientSide) {
                    com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score extended by 2 seconds for perfect timing: {}", player.getName().getString());
                }

                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§a+2 seconds! Perfect Rhythm! §7(" + (newDuration/20) + "s total)"),
                        true
                );
            } else {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§eMusical Score at maximum duration! (16s)"),
                        true
                );
            }
        }
    }

    /**
     * Reduce Musical Score duration for missed timing (-2 seconds, min 1 second)
     */
    public static void reduceForMissedTiming(Player player) {
        var effect = player.getEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get());
        if (effect != null) {
            int currentDuration = effect.getDuration();
            int newDuration = Math.max(currentDuration - 40, 20); // -2 seconds, min 1 second

            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get(),
                    newDuration,
                    0,
                    false,
                    true,
                    true
            ));

            if (!player.level().isClientSide) {
                com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score reduced by 2 seconds for missed timing: {}", player.getName().getString());
            }

            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("§c-2 seconds! Missed rhythm! §7(" + (newDuration/20) + "s remaining)"),
                    true
            );
        }
    }

    /**
     * Get the breathing damage multiplier for Musical Score users
     */
    public static float getBreathingDamageMultiplier(Player player) {
        if (player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get())) {
            return BASE_DAMAGE_MULTIPLIER; // 3x damage
        }
        return 1.0f;
    }

    /**
     * Check if Musical Score allows no cooldown for attacks
     */
    public static boolean allowsNoCooldown(Player player) {
        // Could add rhythm checking here, but for now just return false
        // The rhythm checking should be done in the attack classes
        return false;
    }

    /**
     * Check if player has used their death save this life
     */
    public static boolean hasUsedDeathSave(Player player) {
        return playersUsedDeathSave.contains(player.getUUID());
    }

    /**
     * Reset death save when player dies/respawns
     */
    public static void onPlayerActualDeath(Player player) {
        playersUsedDeathSave.remove(player.getUUID());
        if (!player.level().isClientSide) {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Reset death save for {} (actual death)", player.getName().getString());
        }
    }

    /**
     * Check if player has Sound Breathing
     */
    private static boolean hasSoundBreathing(Player player) {
        if (player.level().isClientSide) {
            return true; // Skip client-side check
        }

        try {
            var data = PlayerDataProvider.getData(player);
            if (data != null) {
                var breathingData = data.getBreathingStyleData();
                if (breathingData != null) {
                    String movesetId = breathingData.getMovesetId();
                    boolean hasSound = "sound_breathing".equals(movesetId);

                    if (!hasSound) {
                        com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Player {} does not have Sound Breathing (has: {})", player.getName().getString(), movesetId);
                    }

                    return hasSound;
                }
            }

            com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Player {} has no breathing style data", player.getName().getString());
            return false;
        } catch (Exception e) {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.error("Error checking Sound Breathing for {}: {}", player.getName().getString(), e.getMessage());
            return true; // Default to true on error to avoid removing effect incorrectly
        }
    }

    // Debug commands for testing
    public static void debugActivate(Player player) {
        activate(player);
    }

    public static void debugClearDeathSave(Player player) {
        playersUsedDeathSave.remove(player.getUUID());
        com.xirc.nichirin.BreathOfNichirin.LOGGER.info("DEBUG: Cleared death save for {}", player.getName().getString());
    }
}