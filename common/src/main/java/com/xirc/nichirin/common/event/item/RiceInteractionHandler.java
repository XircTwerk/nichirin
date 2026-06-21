package com.xirc.nichirin.common.event.item;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Handles rice interactions:
 *   Rice (in hand) + right-click water cauldron → Mochi (same as DrinkingGourdInteractionHandler)
 */
public class RiceInteractionHandler {

    public static void register() {
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
            Level level = player.level();
            ItemStack stack = player.getItemInHand(hand);

            if (!stack.is(NichirinItemRegistry.RICE.get())) {
                return EventResult.pass();
            }

            BlockState state = level.getBlockState(pos);

            // Water cauldron
            if (state.is(Blocks.WATER_CAULDRON) && state.hasProperty(LayeredCauldronBlock.LEVEL)) {
                if (level.isClientSide) return EventResult.pass();

                if (!player.isCreative()) {
                    stack.shrink(1);
                    int cauldronLevel = state.getValue(LayeredCauldronBlock.LEVEL);
                    if (cauldronLevel > 1) {
                        level.setBlock(pos, state.setValue(LayeredCauldronBlock.LEVEL, cauldronLevel - 1), 3);
                    } else {
                        level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
                    }
                }

                ItemStack mochi = new ItemStack(NichirinItemRegistry.MOCHI.get());
                if (!player.addItem(mochi)) player.drop(mochi, false);
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
                return EventResult.interruptTrue();
            }

            // Natural water block (rivers, oceans) — clicked block is water OR block above is water
            boolean isNaturalWater = state.is(Blocks.WATER)
                    || level.getBlockState(pos.above()).is(Blocks.WATER);
            if (isNaturalWater) {
                if (level.isClientSide) return EventResult.pass();

                if (!player.isCreative()) stack.shrink(1);

                ItemStack mochi = new ItemStack(NichirinItemRegistry.MOCHI.get());
                if (!player.addItem(mochi)) player.drop(mochi, false);
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
                return EventResult.interruptTrue();
            }

            return EventResult.pass();
        });
    }
}