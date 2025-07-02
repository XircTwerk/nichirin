package com.xirc.nichirin.fabric.common.terrablender;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.fabric.common.world.NichirinBiomeModifications;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;
import terrablender.api.TerraBlenderApi;

public class NichirinTerraFabric implements TerraBlenderApi {
    private static boolean initialized = false;
    private static boolean modInitialized = false;

    @Override
    public void onTerraBlenderInitialized() {
        System.out.println("[Nichirin] TerraBlender initialized!");
        Regions.register(new OverworldRegionFabric(BreathOfNichirin.id("overworld"), 4));

        // Add ore features to biomes
        NichirinBiomeModifications.addOres();

        initialized = true;
        if (modInitialized) {
            registerSurfaceRules();
        }
    }

    public static void onModInitialized() {
        System.out.println("[Nichirin] Mod initialized for TerraBlender");
        modInitialized = true;
        if (initialized) {
            registerSurfaceRules();
        }
    }

    private static void registerSurfaceRules() {
        // We still register surface rules but they're minimal since we use features for ores
        SurfaceRuleManager.addSurfaceRules(
                SurfaceRuleManager.RuleCategory.OVERWORLD,
                BreathOfNichirin.MOD_ID,
                MaterialRulesFabric.makeRules()
        );
    }
}