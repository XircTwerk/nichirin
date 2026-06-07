package com.xirc.nichirin.common.event.item;

import com.xirc.nichirin.registry.NichirinBlockRegistry;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class WisteriaStrippingHandler {

    private WisteriaStrippingHandler() {
    }

    public static void register() {
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof AxeItem)) return EventResult.pass();

            Level level = player.level();
            BlockState state = level.getBlockState(pos);
            BlockState stripped = strippedState(state);
            if (stripped == null) return EventResult.pass();

            if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                stripped = stripped.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
            }

            if (!level.isClientSide) {
                level.setBlock(pos, stripped, 11);
                level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return EventResult.interruptTrue();
        });
    }

    private static BlockState strippedState(BlockState state) {
        if (state.is(NichirinBlockRegistry.WISTERIA_LOG.get())) {
            return NichirinBlockRegistry.STRIPPED_WISTERIA_LOG.get().defaultBlockState();
        }
        if (state.is(NichirinBlockRegistry.WISTERIA_WOOD.get())) {
            return NichirinBlockRegistry.STRIPPED_WISTERIA_WOOD.get().defaultBlockState();
        }
        return null;
    }
}
