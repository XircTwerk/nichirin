package com.xirc.nichirin.common.handler;

import com.xirc.nichirin.common.system.movement.AirDodge;
import com.xirc.nichirin.common.system.movement.Dash;
import com.xirc.nichirin.common.system.movement.Dodge;
import com.xirc.nichirin.common.system.slayerabilities.PlayerDoubleJump;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.world.entity.player.Player;

public class PlayerTickHandler {

    public static void register() {
        TickEvent.PLAYER_POST.register(PlayerTickHandler::onPlayerTick);
    }


    private static void onPlayerTick(Player player) {
        PlayerDoubleJump.tickPlayer(player);

        // Add movement system ticks
        if (!player.level().isClientSide) { // Server-side only
            // Tick these once per server tick, not per player
            if (player.level().getGameTime() % 1 == 0) { // Every tick
                Dodge.tick();
                AirDodge.tick();
                Dash.tickAllDashes();
            }
        }
    }
}