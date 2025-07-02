package com.xirc.nichirin.fabric.common.terrablender;

import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.core.registries.BuiltInRegistries;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;
import terrablender.api.TerraBlenderApi;

public class NichirinTerraFabric implements TerraBlenderApi {
    private static boolean initialized = false;
    private static boolean modInitialized = false;

    @Override
    public void onTerraBlenderInitialized() {
        System.out.println("[Nichirin] onTerraBlenderInitialized called, modInitialized=" + modInitialized);
        Regions.register(new OverworldRegionFabric(BreathOfNichirin.id("overworld"), 4));
        initialized = true;
        if (modInitialized) {
            registerSurfaceRules();
        }
    }

    public static void onModInitialized() {
        modInitialized = true;
        if (initialized)
            registerSurfaceRules();
    }

    private static void registerSurfaceRules() {
        SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, BreathOfNichirin.MOD_ID, MaterialRulesFabric.makeRules());
    }
}