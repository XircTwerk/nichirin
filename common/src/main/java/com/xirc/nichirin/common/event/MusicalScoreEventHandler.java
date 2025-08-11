package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.effect.MusicalScoreEffect;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Event handler for Musical Score auto-activation
 * Simple survival mechanic: Sound Breathing users survive lethal damage with 1 heart
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

        // Skip if player already has Musical Score effect (cooldown)
        if (player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.MUSICAL_SCORE.get())) {
            return false;
        }

        com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Attempting Musical Score death save for {}",
                player.getName().getString());

        // Try to activate Musical Score survival
        boolean activated = MusicalScoreEffect.activate(player);

        if (activated) {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.info("Musical Score death save activated for {}!",
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

            // Prevent death
            return true;
        } else {
            com.xirc.nichirin.BreathOfNichirin.LOGGER.warn("Musical Score death save failed for {} (cooldown: {} seconds)",
                    player.getName().getString(), MusicalScoreEffect.getRemainingCooldownSeconds(player));
        }

        // Don't prevent death if Musical Score didn't activate
        return false;
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
}