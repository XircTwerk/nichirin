package com.xirc.nichirin.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WysteriaLeavesBlock extends LeavesBlock {

    public WysteriaLeavesBlock(BlockBehaviour.Properties properties) {
        super(properties.lightLevel((state) -> 8)); // Static light level
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
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        super.animateTick(state, world, pos, random);

        if (random.nextInt(15) == 0) {
            float colorPhase = getColorPhase(world, pos);

            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();

        }
    }

    // Enhanced random ticking for more frequent updates
    @Override
    public boolean isRandomlyTicking(BlockState state) {
        // Call super to maintain normal leaf decay behavior, but also add our updates
        return super.isRandomlyTicking(state) || true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        // Call super for normal leaf behavior (decay, etc.)
        super.randomTick(state, world, pos, random);
    }
}