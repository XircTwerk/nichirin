package com.xirc.nichirin.common.blocks.sign;

import com.xirc.nichirin.registry.NichirinBlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WisteriaSignBlockEntity extends SignBlockEntity {
    public WisteriaSignBlockEntity(BlockPos pos, BlockState state) {
        super(NichirinBlockEntityRegistry.WISTERIA_SIGN.get(), pos, state);
    }
}
