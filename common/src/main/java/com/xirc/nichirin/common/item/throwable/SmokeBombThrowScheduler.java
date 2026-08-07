package com.xirc.nichirin.common.item.throwable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Delays the actual smoke bomb release (entity spawn + throw) until a fixed number of
 * ticks after use(), so it lines up with the throw animation's release frame instead of
 * firing the instant the item is right-clicked. Same static-map + TickEvent.SERVER_POST
 * pattern as Dash/Dodge — see PlayerTickHandler#register.
 */
public class SmokeBombThrowScheduler {

    private static final Map<ServerPlayer, PendingThrow> pending = new HashMap<>();

    public static void schedule(ServerPlayer player, ItemStack renderStack, int delayTicks) {
        pending.put(player, new PendingThrow(renderStack, delayTicks));
    }

    public static void tick() {
        pending.entrySet().removeIf(entry -> {
            ServerPlayer player = entry.getKey();
            PendingThrow state = entry.getValue();

            if (!player.isAlive() || player.isRemoved()) {
                return true;
            }

            state.remainingTicks--;
            if (state.remainingTicks <= 0) {
                if (player.level() instanceof ServerLevel serverLevel) {
                    SmokeBombItem.releaseGrenade(serverLevel, player, state.renderStack);
                }
                return true;
            }
            return false;
        });
    }

    private static class PendingThrow {
        final ItemStack renderStack;
        int remainingTicks;

        PendingThrow(ItemStack renderStack, int remainingTicks) {
            this.renderStack = renderStack;
            this.remainingTicks = remainingTicks;
        }
    }
}