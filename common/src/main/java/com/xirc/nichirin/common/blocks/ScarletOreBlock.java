package com.xirc.nichirin.common.blocks;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ScarletOreBlock extends DropExperienceBlock {

    public ScarletOreBlock() {
        super(UniformInt.of(3, 7), BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED)
                        .strength(3.0f, 3.0f)
                        .requiresCorrectToolForDrops()); // This handles XP drops automatically
    }
}