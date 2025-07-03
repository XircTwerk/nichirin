package com.xirc.nichirin.common.blocks;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ScarletCrimsonIronSandBlock extends DropExperienceBlock {

    public ScarletCrimsonIronSandBlock() {
        super(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED)
                        .strength(2.0f, 3.0f)
                        .requiresCorrectToolForDrops(),
                UniformInt.of(2, 5)); // This handles XP drops automatically
    }
}