package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.effect.MusicalScoreEffect;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Event handler for Musical Score auto-activation
 * Simple survival mechanic: Sound Breathing users survive lethal damage with 1 heart
 * Can only be used once per life - after respawn, the ability resets
 */
public class MusicalScoreEventHandler {

    /**
     * Register the Musical Score damage event handler
     */
    public static void register() {
        // Try to intercept at death event level - same as totem of undying
        EntityEvent.LIVING_DEATH.register((entity, damageSource) -> {
            if (entity instanceof Player player && !player.level().isClientSide) {
                // Check if Musical Score should save the player from death
                boolean saved = onPlayerDeath(player, damageSource);

                if (saved) {
                    // Musical Score activated - prevent death
                    com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score prevented death for {}",
                            player.getName().getString());
                    return EventResult.interruptTrue(); // Cancel death
                } else {
                    // Player is actually dying - reset death save for next life
                    MusicalScoreEffect.onPlayerActualDeath(player);
                }
            }
            return EventResult.pass(); // Allow death to proceed
        });
    }

    /**
     * Called when player would die - this is our last chance to save them
     * Returns true if death should be prevented (Musical Score activated)
     */
    public static boolean onPlayerDeath(Player player, DamageSource damageSource) {
        // Skip if player is creative or spectator
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }

        // Check if player has Sound Breathing
        if (!hasSoundBreathing(player)) {
            return false;
        }

        // IMPORTANT: If Musical Score is already active, let them die normally
        if (player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get())) {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Player {} died while Musical Score was active - allowing death",
                    player.getName().getString());
            return false; // Let them die
        }

        // Check if player can use Musical Score for death save (only once per life)
        if (!MusicalScoreEffect.canActivateForDeathSave(player)) {
            if (MusicalScoreEffect.hasUsedDeathSave(player)) {
                com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score death save unavailable for {} - already used this life",
                        player.getName().getString());
            } else {
                com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score death save unavailable for {} - no Sound Breathing",
                        player.getName().getString());
            }
            return false;
        }

        com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Attempting Musical Score death save for {}",
                player.getName().getString());

        // Try to activate Musical Score for death save
        boolean activated = MusicalScoreEffect.activateForDeathSave(player);

        if (activated) {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score death save activated for {}! Cannot be used again this life.",
                    player.getName().getString());

            // Set health to exactly 1 heart (2.0 health points)
            player.setHealth(2.0f);

            // Play dramatic activation sound
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.WARDEN_SONIC_BOOM,
                    net.minecraft.sounds.SoundSource.PLAYERS,
                    2.0f, 0.5f);

            // Send message to player
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Musical Score! You survived with 1 heart!")
                            .withStyle(style -> style.withColor(0xF5DEB3).withBold(true)),
                    true
            );

            // Send secondary message about one-time use (DEATH SAVE ONLY)
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("(Death save can only be used once per life)")
                            .withStyle(style -> style.withColor(0xFFAA00).withItalic(true)),
                    false
            );

            // Prevent death
            return true;
        } else {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.warn("Musical Score death save failed for {} (this shouldn't happen if canActivateForDeathSave returned true)",
                    player.getName().getString());
        }

        // Don't prevent death if Musical Score didn't activate
        return false;
    }

    /**
     * Called when player respawns - reset their death save ability
     */
    public static void onPlayerRespawn(Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Player {} respawned - resetting Musical Score death save",
                    player.getName().getString());

            // Reset death save tracking (no cooldown anymore)
            MusicalScoreEffect.onPlayerActualDeath(player);

            // Send welcome back message if they have Sound Breathing
            if (hasSoundBreathing(player)) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("Musical Score death save ability restored!")
                                .withStyle(style -> style.withColor(0xF5DEB3).withBold(false)),
                        false
                );
            }
        }
    }

    /**
     * Check if player has Sound Breathing
     */
    private static boolean hasSoundBreathing(Player player) {
        // Only works for server players (where data is stored)
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return false;
        }

        try {
            // Get player's breathing style data
            com.xirc.nichirin.common.data.PlayerData playerData =
                    com.xirc.nichirin.common.data.PlayerDataProvider.getData(serverPlayer);

            // Check if they have a breathing style selected
            String movesetId = playerData.getBreathingStyleData().getMovesetId();

            // Return true if they have Sound Breathing selected
            return "sound_breathing".equals(movesetId);

        } catch (Exception e) {
            // Log error and return false if data access fails
            com.xirc.nichirin.BreathOfNichirin.LOGGER.error("Failed to check Sound Breathing for player {}: {}",
                    player.getName().getString(), e.getMessage());
            return false;
        }
    }

    /**
     * Get debug information about a player's Musical Score status
     */
    public static void sendDebugInfo(Player player) {
        if (hasSoundBreathing(player)) {
            // Simple debug info since there's no cooldown anymore
            boolean hasEffect = player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get());
            boolean hasUsedDeathSave = MusicalScoreEffect.hasUsedDeathSave(player);

            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("Musical Score Status:")
                            .withStyle(style -> style.withColor(0xF5DEB3).withBold(true)),
                    false
            );

            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("- Sound Breathing: YES")
                            .withStyle(style -> style.withColor(0x00FF00)),
                    false
            );

            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("- Currently Active: " + (hasEffect ? "YES" : "NO"))
                            .withStyle(style -> style.withColor(hasEffect ? 0x00FF00 : 0xFFAA00)),
                    false
            );

            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("- Death Save Used: " + (hasUsedDeathSave ? "YES" : "NO"))
                            .withStyle(style -> style.withColor(hasUsedDeathSave ? 0xFF6B6B : 0x00FF00)),
                    false
            );

            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("- No Cooldown: Can activate anytime!")
                            .withStyle(style -> style.withColor(0x00FF00)),
                    false
            );

        } else {
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal("You don't have Sound Breathing equipped!")
                            .withStyle(style -> style.withColor(0xFF6B6B)),
                    false
            );
        }
    }
}