package com.xirc.nichirin.forge.common.terrablender;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

public class MaterialRulesForge {

    public static SurfaceRules.RuleSource makeRules() {
        // Return a minimal rule that never actually triggers
        // This satisfies TerraBlender's requirement for at least one rule
        return SurfaceRules.ifTrue(
                SurfaceRules.yBlockCheck(VerticalAnchor.absolute(-64), 0), // Never true (below bedrock)
                SurfaceRules.state(Blocks.STONE.defaultBlockState())
        );
    }
}
