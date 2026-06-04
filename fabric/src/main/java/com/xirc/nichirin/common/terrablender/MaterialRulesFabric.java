package com.xirc.nichirin.common.terrablender;

import com.xirc.nichirin.common.world.WisteriaSurfaceRules;
import net.minecraft.world.level.levelgen.SurfaceRules;

public class MaterialRulesFabric {

    public static SurfaceRules.RuleSource makeRules() {
        return WisteriaSurfaceRules.makeRules();
    }
}
