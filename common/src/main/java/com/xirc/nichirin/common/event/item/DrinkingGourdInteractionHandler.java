package com.xirc.nichirin.common.event.item;

import com.xirc.nichirin.common.item.tool.DrinkingGourdItem;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DrinkingGourdInteractionHandler {

    public static void register() {
        // Register block interaction (cauldrons only)
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
            Level level = player.level();
            ItemStack stack = player.getItemInHand(hand);

            if (!(stack.getItem() instanceof DrinkingGourdItem)) {
                return EventResult.pass();
            }

            BlockState state = level.getBlockState(pos);
            int currentDrinks = DrinkingGourdItem.getDrinks(stack);

            // Can't fill if already full
            if (currentDrinks >= 8) {
                return EventResult.pass();
            }

            // Fill from water cauldron
            if (state.is(Blocks.WATER_CAULDRON) && state.hasProperty(LayeredCauldronBlock.LEVEL)) {
                DrinkingGourdItem.addDrink(stack, DrinkingGourdItem.LiquidType.WATER);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);

                // Decrease cauldron level if not creative
                if (!player.isCreative()) {
                    int cauldronLevel = state.getValue(LayeredCauldronBlock.LEVEL);
                    if (cauldronLevel > 1) {
                        level.setBlock(pos, state.setValue(LayeredCauldronBlock.LEVEL, cauldronLevel - 1), 3);
                    } else {
                        level.setBlock(pos, Blocks.CAULDRON.defaultBlockState(), 3);
                    }
                }

                return EventResult.interruptTrue();
            }

            return EventResult.pass();
        });

        // Register entity interaction (cows only)
        InteractionEvent.INTERACT_ENTITY.register((player, entity, hand) -> {
            Level level = player.level();
            ItemStack stack = player.getItemInHand(hand);

            if (!(stack.getItem() instanceof DrinkingGourdItem)) {
                return EventResult.pass();
            }

            int currentDrinks = DrinkingGourdItem.getDrinks(stack);

            // Can't fill if already full
            if (currentDrinks >= 8) {
                return EventResult.pass();
            }

            // Fill from cow (one drink at a time)
            if (entity instanceof Cow cow && !cow.isBaby()) {
                DrinkingGourdItem.addDrink(stack, DrinkingGourdItem.LiquidType.MILK);
                level.playSound(null, entity.blockPosition(), SoundEvents.COW_MILK, SoundSource.NEUTRAL, 1.0F, 1.0F);
                return EventResult.interruptTrue();
            }

            return EventResult.pass();
        });
    }
}