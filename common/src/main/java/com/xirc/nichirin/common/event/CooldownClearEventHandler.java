package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

/**
 * Event handler for clearing cooldowns on death and respawn
 * Ensures cooldowns don't persist through death
 */
public class CooldownClearEventHandler {

    /**
     * Register cooldown clearing events
     */
    public static void register() {
        // Clear cooldowns when player dies
        EntityEvent.LIVING_DEATH.register((entity, damageSource) -> {
            if (entity instanceof Player player && !player.level().isClientSide) {
                clearPlayerCooldowns(player);
                com.xirc.nichirin.BreathOfNichirin.LOGGER.debug("Cleared cooldowns for {} on death",
                        player.getName().getString());
            }
            return EventResult.pass();
        });

        // Clear cooldowns when player respawns
        PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd) -> {
            if (!conqueredEnd && !newPlayer.level().isClientSide) {
                clearPlayerCooldowns(newPlayer);
                com.xirc.nichirin.BreathOfNichirin.LOGGER.debug("Cleared cooldowns for {} on respawn",
                        newPlayer.getName().getString());
            }
        });

        // Client-side: Clear HUD cooldowns when player respawns
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            if (minecraft.player != null && minecraft.level != null) {
                // Simple detection: if player just spawned (low tick count and full health)
                if (minecraft.player.tickCount <= 10 && minecraft.player.getHealth() > 0) {
                    // Clear all cooldowns from the HUD display
                    com.xirc.nichirin.client.gui.CooldownHUD.clearAllCooldowns();
                }
            }
        });
    }

    /**
     * Clear all cooldowns for a player (server-side)
     */
    private static void clearPlayerCooldowns(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // Clear all other cooldowns by sending 0-duration packets
        // This covers any breathing technique cooldowns
        String[] commonCooldowns = {
                "Thunder Breathing",
                "Flame Breathing",
                "Sound Breathing",
                "Insect Breathing",
                "Water Breathing"
        };

        for (String cooldownName : commonCooldowns) {
            CooldownDisplayPacket.sendToClient(serverPlayer, cooldownName, 0);
        }

        // Send a general "clear all" packet if you want to implement that
        // CooldownDisplayPacket.sendToClient(serverPlayer, "CLEAR_ALL", -1);
    }
}