package com.xirc.nichirin.common.blocks;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ScarletOreBlock extends DropExperienceBlock {

    public ScarletOreBlock() {
        super(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED)
                        .strength(3.0f, 3.0f)
                        .requiresCorrectToolForDrops(),
                UniformInt.of(3, 7)); // This handles XP drops automatically
    }
}