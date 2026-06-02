package com.xirc.nichirin.common.worldgen.trees.wysteria;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WysteriaSaplingBlock extends SaplingBlock {

    public WysteriaSaplingBlock(BlockBehaviour.Properties properties) {
        super(WysteriaTreeGrower.SMALL, properties);
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.cycle(STAGE), 4);
        } else {
            WysteriaTreeGrower.select(random).growTree(
                    level, level.getChunkSource().getGenerator(), pos, state, random);
        }
    }
}