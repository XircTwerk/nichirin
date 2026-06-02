package com.xirc.nichirin.common.event.unlock;

import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.ProgressionHelper;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Handles unlocking Mist Breathing when a player kills a mob in a mountain biome while it's raining.
 */
public class MistBreathingUnlockHandler {

    public static void register() {
        EntityEvent.LIVING_DEATH.register((entity, damageSource) -> {
            // Must be killed by a player
            if (!(damageSource.getEntity() instanceof ServerPlayer player)) return EventResult.pass();

            // Must NOT be the player killing themselves (suicide doesn't count)
            if (entity == player) return EventResult.pass();

            // Already unlocked?
            if (ProgressionHelper.isStyleUnlocked(player, "mist_breathing")) return EventResult.pass();

            // Must be in a mountain biome
            var biome = player.level().getBiome(player.blockPosition());
            if (!biome.is(BiomeTags.IS_MOUNTAIN)) return EventResult.pass();

            // Must be raining ON the player (rules out covered areas and snow biomes — mist is born of rain).
            if (!player.level().isRainingAt(player.blockPosition())) return EventResult.pass();

            unlockMistBreathing(player);
            return EventResult.pass();
        });
    }

    private static void unlockMistBreathing(ServerPlayer player) {
        ProgressionHelper.unlockStyle(player, "mist_breathing");
        PlayerDataProvider.updateAndSync(player, "mist_breathing");

        player.displayClientMessage(
                Component.literal("🌫 The mountain mist embraces you. Mist Breathing unlocked! 🌫")
                        .withStyle(style -> style.withColor(0xB0C4DE).withBold(true)),
                false
        );

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.WEATHER_RAIN,
                SoundSource.PLAYERS, 1.0f, 1.2f);

        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 25; i++) {
                double offsetX = (player.getRandom().nextDouble() - 0.5) * 2.5;
                double offsetY = player.getRandom().nextDouble() * 2.5;
                double offsetZ = (player.getRandom().nextDouble() - 0.5) * 2.5;

                serverLevel.sendParticles(ParticleTypes.CLOUD,
                        player.getX() + offsetX,
                        player.getY() + offsetY,
                        player.getZ() + offsetZ,
                        1, 0, 0, 0, 0.02);
            }
        }
    }
}