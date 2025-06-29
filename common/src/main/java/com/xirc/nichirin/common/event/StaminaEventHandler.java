package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.common.system.slayerabilities.PlayerDoubleJump;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.world.entity.player.Player;

/**
 * Stamina system integration with proper server-side handling
 */
public class StaminaEventHandler {

    /**
     * Registers all stamina-related events
     */
    public static void register() {
        // Server-side player tick for BOTH stamina and double jump
        TickEvent.PLAYER_POST.register(player -> {
            if (!player.level().isClientSide) { // SERVER SIDE ONLY
                // Tick stamina system
                StaminaManager.tick(player);

                // ALSO tick double jump state on server
                PlayerDoubleJump.tickPlayer(player);
            }
        });

        // Initial sync when player joins
        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (!player.level().isClientSide) {
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
            }
        });
    }
}