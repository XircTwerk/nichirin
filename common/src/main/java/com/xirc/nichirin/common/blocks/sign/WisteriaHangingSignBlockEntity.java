package com.xirc.nichirin.common.blocks.sign;

import com.xirc.nichirin.registry.NichirinBlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WisteriaHangingSignBlockEntity extends HangingSignBlockEntity {
    public WisteriaHangingSignBlockEntity(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return NichirinBlockEntityRegistry.WISTERIA_HANGING_SIGN.get();
    }

    /**
     * {@link HangingSignBlockEntity}'s constructor hardcodes the vanilla {@code HANGING_SIGN} type
     * field, and {@link net.minecraft.world.level.block.entity.BlockEntity}'s constructor validates
     * the placed state against that field — which rejects the wisteria hanging-sign blocks. Validate
     * against the modded type instead so placement doesn't crash.
     */
    @Override
    public boolean isValidBlockState(BlockState state) {
        return getType().isValid(state);
    }
}
