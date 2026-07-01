package com.xirc.nichirin.common.event.unlock;

import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.ProgressionHelper;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles unlocking Flame Breathing when on fire for 15 seconds (300 ticks)
 */
public class FlameBreathingUnlockHandler {

    private static final Map<UUID, Integer> burningPlayers = new HashMap<>();
    private static final int REQUIRED_BURN_TICKS = 300; // 15 seconds * 20 ticks/second

    public static void register() {
        TickEvent.SERVER_PRE.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkPlayerBurning(player);
            }
        });
    }

    private static void checkPlayerBurning(ServerPlayer player) {
        UUID playerId = player.getUUID();

        if (player.isOnFire()) {
            int currentTicks = burningPlayers.getOrDefault(playerId, 0);
            currentTicks++;
            burningPlayers.put(playerId, currentTicks);

            if (currentTicks >= REQUIRED_BURN_TICKS) {
                checkFlameBreathingUnlock(player);
                burningPlayers.remove(playerId);
            }
        } else {
            burningPlayers.remove(playerId);
        }
    }

    private static void checkFlameBreathingUnlock(ServerPlayer player) {
        if (ProgressionHelper.isStyleUnlocked(player, "flame_breathing")) return;
        unlockFlameBreathing(player);
    }

    private static void unlockFlameBreathing(ServerPlayer player) {
        ProgressionHelper.unlockStyle(player, "flame_breathing");
        PlayerDataProvider.updateAndSync(player, "flame_breathing");

        player.displayClientMessage(
                Component.literal("If you are feeling disheartened, that you are somehow not enough. Set your heart ablaze! Flame Breathing unlocked!")
                        .withStyle(style -> style.withColor(0xFF5500).withBold(true)),
                false
        );

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FIRE_AMBIENT,
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public static int getBurnProgress(UUID playerId) {
        return burningPlayers.getOrDefault(playerId, 0);
    }

    public static int getRequiredBurnTicks() {
        return REQUIRED_BURN_TICKS;
    }
}