package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.system.StanceManager;
import com.xirc.nichirin.common.system.blocking.KatanaBlock;
import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.common.system.abilities.PlayerDoubleJump;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.event.events.common.PlayerEvent;

/**
 * Stamina system integration with proper server-side handling
 */
public class StaminaEventHandler {

    /**
     * Registers all stamina-related events
     */
    public static void register() {
        // Server-side player tick for stamina ONLY (double jump is handled by mixin)
        TickEvent.PLAYER_POST.register(player -> {
            if (!player.level().isClientSide) { // SERVER SIDE ONLY
                // Tick stamina system
                StaminaManager.tick(player);

                StanceManager.tick(player);
                KatanaBlock.tick(player);
            }
        });

        // Initial sync when player joins
        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (!player.level().isClientSide) {

                // Force initialize stamina if needed
                float currentStamina = StaminaManager.getStamina(player);
                float maxStamina = StaminaManager.getMaxStamina(player);

                // If stamina is 0, restore it to full
                if (currentStamina <= 0) {
                    StaminaManager.restoreFull(player);
                }

                // Force sync
                StaminaManager.forceSyncToClient(player);

            }
        });

        // Clean up when player disconnects
        PlayerEvent.PLAYER_QUIT.register(player -> {
            StaminaManager.cleanupPlayer(player);
        });

        // Restore full stamina on respawn
        PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd) -> {
            if (!conqueredEnd) {
                StaminaManager.restoreFull(newPlayer);
                PlayerDoubleJump.resetDoubleJump(newPlayer);

                // Force sync after respawn
                StaminaManager.forceSyncToClient(newPlayer);
            }
        });
    }
}