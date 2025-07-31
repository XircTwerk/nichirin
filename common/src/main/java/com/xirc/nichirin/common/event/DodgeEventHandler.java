package com.xirc.nichirin.common.event;

import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.EventResult;
import com.xirc.nichirin.common.system.movement.Dodge;
import net.minecraft.world.entity.player.Player;

public class DodgeEventHandler {

    public static void register() {
        // Register damage event with debug
        EntityEvent.LIVING_HURT.register((entity, damageSource, amount) -> {
            if (entity instanceof Player player) {
                System.out.println("DEBUG: Player " + player.getName().getString() +
                        " taking damage: " + amount + " from " + damageSource.getMsgId() +
                        ", immunity: " + player.invulnerableTime +
                        ", is dodging: " + Dodge.isPlayerDodging(player));

                if (Dodge.isPlayerDodging(player)) {
                    System.out.println("DEBUG: Dodge detected, marking successful and canceling damage");
                    Dodge.markDodgeSuccessful(player);
                    return EventResult.interruptFalse(); // Cancel damage
                }
            }
            return EventResult.pass(); // Allow damage
        });
    }
}