// Updated NichirinTerraForge.java
package com.xirc.nichirin.forge.common.terrablender;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.forge.common.terrablender.MaterialRulesForge;
import com.xirc.nichirin.forge.common.terrablender.OverworldRegionForge;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

public class NichirinTerraForge {
    private static boolean initialized = false;

    public static void onTerraBlenderInitialized() {
        if (initialized) return;

        try {
            System.out.println("[Nichirin] TerraBlender Forge initialized!");
            Regions.register(new OverworldRegionForge(BreathOfNichirin.id("overworld"), 4));
            registerSurfaceRules();
            initialized = true;
        } catch (Exception e) {
            BreathOfNichirin.LOGGER.error("Failed to initialize TerraBlender Forge integration", e);
        }
    }

    private static void registerSurfaceRules() {
        // We still register surface rules but they're minimal since we use features for ores
        SurfaceRuleManager.addSurfaceRules(
                SurfaceRuleManager.RuleCategory.OVERWORLD,
                BreathOfNichirin.MOD_ID,
                MaterialRulesForge.makeRules()
        );
    }
}