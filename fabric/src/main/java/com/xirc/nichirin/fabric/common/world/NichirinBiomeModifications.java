package com.xirc.nichirin.fabric.common.world;

import com.xirc.nichirin.BreathOfNichirin;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;

public class NichirinBiomeModifications {
    public static void addOres() {
        // Add Scarlet Crimson Iron Sand to ALL biomes, then filter by biome type
        BiomeModifications.addFeature(
                BiomeSelectors.all(), // Use all() instead of includeByKey to work with TerraBlender
                GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE,
                        new ResourceLocation(BreathOfNichirin.MOD_ID, "scarlet_crimson_iron_sand"))
        );

        // Add Scarlet Ore to ALL biomes, then filter by biome type
        BiomeModifications.addFeature(
                BiomeSelectors.all(), // Use all() instead of includeByKey to work with TerraBlender
                GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE,
                        new ResourceLocation(BreathOfNichirin.MOD_ID, "scarlet_ore"))
        );

        System.out.println("[Nichirin] Ore generation features added to ALL biomes for TerraBlender compatibility!");
    }
}