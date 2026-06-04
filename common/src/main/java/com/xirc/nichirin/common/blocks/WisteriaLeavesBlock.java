package com.xirc.nichirin.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WisteriaLeavesBlock extends LeavesBlock {

    public WisteriaLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties.lightLevel((state) -> 12));
    }

    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        super.animateTick(state, world, pos, random);

        if (random.nextInt(15) == 0) {
            float colorPhase = getColorPhase(world, pos);

            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
        }
    }

    // Helper method to get current color phase
    private float getColorPhase(Level world, BlockPos pos) {
        long time = world.getGameTime();
        float cycle = (time % 200L) / 200.0f;
        float positionVariation = (pos.getX() + pos.getZ()) * 0.1f;
        float colorPhase = cycle + positionVariation;
        return colorPhase - (float)Math.floor(colorPhase);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        // Only allow decay if explicitly set to non-persistent and at max distance
        return state.getValue(DISTANCE) == 7 && !state.getValue(PERSISTENT);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        if (!state.getValue(PERSISTENT)) {
            world.setBlock(pos, state.setValue(PERSISTENT, true), 3);
            return;
        }

        // Call super for normal leaf behavior only if not persistent
        super.randomTick(state, world, pos, random);
    }
}
