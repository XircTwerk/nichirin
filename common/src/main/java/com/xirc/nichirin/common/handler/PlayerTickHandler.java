package com.xirc.nichirin.common.handler;

import com.xirc.nichirin.common.system.movement.Dash;
import com.xirc.nichirin.common.system.movement.Dodge;
import com.xirc.nichirin.common.system.abilities.PlayerDoubleJump;
import com.xirc.nichirin.common.system.DemonManager;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.world.entity.player.Player;

public class PlayerTickHandler {

    public static void register() {
        TickEvent.PLAYER_POST.register(PlayerTickHandler::onPlayerTick);

        // Register server tick for global systems
        TickEvent.SERVER_POST.register(server -> {
            Dodge.tick(); // Global cleanup
            Dash.tickAllDashes(); // Move this here - once per server tick
        });
    }

    private static void onPlayerTick(Player player) {
        PlayerDoubleJump.tickPlayer(player);

        // Only per-player systems on server side
        if (!player.level().isClientSide) {
            Dodge.tickForPlayer(player);

            // Tick demon passives (sun damage, blood regen, infinite stamina, blood drain)
            DemonManager.tickDemon(player);
        }
    }
}