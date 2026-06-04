package com.xirc.nichirin.common.worldgen.trees.wisteria;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WisteriaSaplingBlock extends SaplingBlock {

    public WisteriaSaplingBlock(BlockBehaviour.Properties properties) {
        super(WisteriaTreeGrower.SMALL, properties);
    }

    @Override
    public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        if (state.getValue(STAGE) == 0) {
            level.setBlock(pos, state.cycle(STAGE), 4);
        } else {
            WisteriaTreeGrower.select(random).growTree(
                    level, level.getChunkSource().getGenerator(), pos, state, random);
        }
    }
}