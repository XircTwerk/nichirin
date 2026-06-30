package com.xirc.nichirin.common.event.unlock;

import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.ProgressionHelper;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.monster.Drowned;

/**
 * Handles unlocking Water Breathing when a player kills a Drowned with bare hands (empty main hand).
 */
public class WaterBreathingUnlockHandler {

    public static void register() {
        EntityEvent.LIVING_DEATH.register((entity, damageSource) -> {
            // Must be a Drowned
            if (!(entity instanceof Drowned)) return EventResult.pass();

            // Must be killed by a player
            if (!(damageSource.getEntity() instanceof ServerPlayer player)) return EventResult.pass();

            // Player's main hand must be empty (bare hands)
            if (!player.getMainHandItem().isEmpty()) return EventResult.pass();

            // Already unlocked?
            if (ProgressionHelper.isStyleUnlocked(player, "water_breathing")) return EventResult.pass();

            unlockWaterBreathing(player);
            return EventResult.pass();
        });
    }

    private static void unlockWaterBreathing(ServerPlayer player) {
        ProgressionHelper.unlockStyle(player, "water_breathing");
        PlayerDataProvider.updateAndSync(player, "water_breathing");

        player.displayClientMessage(
                Component.literal("The current of life flows through you. Water Breathing unlocked!")
                        .withStyle(style -> style.withColor(0x00AAFF).withBold(true)),
                false
        );

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.AMBIENT_UNDERWATER_ENTER,
                SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}