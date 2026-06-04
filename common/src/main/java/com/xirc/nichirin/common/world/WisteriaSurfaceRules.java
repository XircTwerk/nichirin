package com.xirc.nichirin.common.world;

import com.xirc.nichirin.registry.NichirinBiomeRegistry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;

public final class WisteriaSurfaceRules {
    private WisteriaSurfaceRules() {
    }

    public static SurfaceRules.RuleSource makeRules() {
        SurfaceRules.RuleSource grass = SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState());
        SurfaceRules.RuleSource dirt = SurfaceRules.state(Blocks.DIRT.defaultBlockState());

        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(NichirinBiomeRegistry.WISTERIA_GROVE),
                SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, grass),
                        SurfaceRules.ifTrue(SurfaceRules.UNDER_FLOOR, dirt),
                        SurfaceRules.ifTrue(SurfaceRules.DEEP_UNDER_FLOOR, dirt)
                )
        );
    }
}
