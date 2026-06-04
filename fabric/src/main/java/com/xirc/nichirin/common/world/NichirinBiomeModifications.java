package com.xirc.nichirin.common.world;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.GenerationStep;

public class NichirinBiomeModifications {
    public static void addSpawns() {
        BiomeModifications.addSpawn(
                BiomeSelectors.tag(BiomeTags.IS_OVERWORLD),
                MobCategory.MONSTER,
                NichirinEntityRegistry.TEMPLE_DEMON.get(),
                8, 1, 2
        );
    }

    public static void addOres() {
        // Add Scarlet Crimson Iron Sand to ALL biomes, then filter by biome type
        BiomeModifications.addFeature(
                BiomeSelectors.all(), // Use all() instead of includeByKey to work with TerraBlender
                GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE,
                        ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "scarlet_crimson_iron_sand"))
        );

        // Add Scarlet Ore to ALL biomes, then filter by biome type
        BiomeModifications.addFeature(
                BiomeSelectors.all(), // Use all() instead of includeByKey to work with TerraBlender
                GenerationStep.Decoration.UNDERGROUND_ORES,
                ResourceKey.create(Registries.PLACED_FEATURE,
                        ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "scarlet_ore"))
        );

    }
}