package com.xirc.nichirin.common.event.system;

import com.xirc.nichirin.common.system.movement.Dash;
import com.xirc.nichirin.common.system.movement.Dodge;
import com.xirc.nichirin.common.system.abilities.PlayerDoubleJump;
import com.xirc.nichirin.common.system.DemonManager;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.world.entity.player.Player;

public class PlayerTickHandler {

    public static void register() {
        TickEvent.PLAYER_POST.register(PlayerTickHandler::onPlayerTick);

        TickEvent.SERVER_POST.register(server -> {
            Dodge.tick();
            Dash.tickAllDashes();
        });
    }

    private static void onPlayerTick(Player player) {
        PlayerDoubleJump.tickPlayer(player);

        if (!player.level().isClientSide) {
            Dodge.tickForPlayer(player);
            DemonManager.tickDemon(player);
        }
    }
}