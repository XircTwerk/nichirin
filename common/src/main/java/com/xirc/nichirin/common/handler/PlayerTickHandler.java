package com.xirc.nichirin.common.handler;

import com.xirc.nichirin.common.system.slayerabilities.PlayerDoubleJump;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.world.entity.player.Player;

public class PlayerTickHandler {

    public static void register() {
        TickEvent.PLAYER_POST.register(PlayerTickHandler::onPlayerTick);
    }

    private static void onPlayerTick(Player player) {
        PlayerDoubleJump.tickPlayer(player);
    }
}