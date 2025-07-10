package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.util.BreathingManager;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.event.events.common.PlayerEvent;

/**
 * Breathing system event handling
 */
public class BreathingEventHandler {

    /**
     * Registers all breathing-related events
     */
    public static void register() {
        // Server-side player tick for breathing
        TickEvent.PLAYER_POST.register(player -> {
            if (!player.level().isClientSide) { // SERVER SIDE ONLY
                // Tick breathing system
                BreathingManager.tick(player);
            }
        });

        // Initial sync when player joins
        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (!player.level().isClientSide) {
                // Force initialize breathing if needed
                float currentBreath = BreathingManager.getBreath(player);

                // If breath is 0, restore it to full
                if (currentBreath <= 0) {
                    BreathingManager.restoreFull(player);
                }

                // Force sync
                BreathingManager.forceSyncToClient(player);
            }
        });

        // Clean up when player disconnects
        PlayerEvent.PLAYER_QUIT.register(player -> {
            BreathingManager.cleanupPlayer(player);
        });

        // Restore full breath on respawn
        PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd) -> {
            if (!conqueredEnd) {
                BreathingManager.restoreFull(newPlayer);

                // Force sync after respawn
                BreathingManager.forceSyncToClient(newPlayer);
            }
        });
    }
}