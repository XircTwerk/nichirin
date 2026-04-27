package com.xirc.nichirin.common.event.system;

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

    public static void register() {
        TickEvent.PLAYER_POST.register(player -> {
            if (!player.level().isClientSide) {
                StaminaManager.tick(player);

                StanceManager.tick(player);
                KatanaBlock.tick(player);
            }
        });

        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (!player.level().isClientSide) {
                float currentStamina = StaminaManager.getStamina(player);

                if (currentStamina <= 0) {
                    StaminaManager.restoreFull(player);
                }

                StaminaManager.forceSyncToClient(player);
            }
        });

        PlayerEvent.PLAYER_QUIT.register(player -> {
            StaminaManager.cleanupPlayer(player);
        });

        PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd) -> {
            if (!conqueredEnd) {
                StaminaManager.restoreFull(newPlayer);
                PlayerDoubleJump.resetDoubleJump(newPlayer);
                StaminaManager.forceSyncToClient(newPlayer);
            }
        });
    }
}