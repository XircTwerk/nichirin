package com.xirc.nichirin.common.event;

import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.data.ProgressionHelper;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Handles unlocking Sound Breathing when playing a music disc in a jukebox
 */
public class SoundBreathingUnlockHandler {

    public static void register() {
        // Register for block interaction events to detect jukebox usage
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
            if (player instanceof ServerPlayer serverPlayer && !player.level().isClientSide) {
                checkJukeboxInteraction(serverPlayer, hand, pos);
            }

            return dev.architectury.event.EventResult.pass();
        });
    }

    /**
     * Checks if player is placing a music disc in a jukebox
     */
    private static void checkJukeboxInteraction(ServerPlayer player, InteractionHand hand, BlockPos pos) {
        // Check if player already has Sound Breathing unlocked
        if (ProgressionHelper.isStyleUnlocked(player, "sound_breathing")) {
            return; // Already unlocked
        }

        Level level = player.level();
        BlockState blockState = level.getBlockState(pos);

        // Check if the block is a jukebox
        if (blockState.getBlock() == Blocks.JUKEBOX) {
            ItemStack heldItem = player.getItemInHand(hand);

            // Check if the held item is a music disc (RecordItem)
            if (heldItem.getItem() instanceof RecordItem) {
                // Check if jukebox is empty (can accept the disc)
                if (!blockState.getValue(JukeboxBlock.HAS_RECORD)) {
                    // Player is about to play a music disc!
                    unlockSoundBreathing(player);
                }
            }
        }
    }

    /**
     * Unlocks Sound Breathing for the player
     */
    private static void unlockSoundBreathing(ServerPlayer player) {
        // Record the unlock in progression system (this handles all advancement triggers)
        ProgressionHelper.unlockStyle(player, "sound_breathing");

        // Set Sound Breathing as the player's style
        PlayerDataProvider.updateAndSync(player, "sound_breathing");

        // Send success message
        player.displayClientMessage(
                Component.literal("🎵 Let my musical score technique lead us to victory! Sound breathing unlocked! 🎵")
                        .withStyle(style -> style.withColor(0x00CED1).withBold(true)), // Dark turquoise color
                false
        );

        // Play note block sound effect
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.NOTE_BLOCK_CHIME.value(),
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

        // Play additional musical sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HARP.value(),
                net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.2f);

        // Spawn note particles around the player
        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 30; i++) {
                double offsetX = (player.getRandom().nextDouble() - 0.5) * 3.0;
                double offsetY = player.getRandom().nextDouble() * 2.5;
                double offsetZ = (player.getRandom().nextDouble() - 0.5) * 3.0;

                // Use note particles for music theme
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.NOTE,
                        player.getX() + offsetX,
                        player.getY() + offsetY,
                        player.getZ() + offsetZ,
                        1, 0, 0.1, 0, 0);
            }
        }
    }
}