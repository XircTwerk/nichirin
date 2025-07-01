package com.xirc.nichirin.common.world;

import com.xirc.nichirin.BreathOfNichirin;
import dev.architectury.registry.level.biome.BiomeModifications;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep;

public class NichirinBiomeModifications {
    public static void addOres() {
        // Add Scarlet Crimson Iron Sand to beaches and rivers
        BiomeModifications.addProperties(biomeContext -> {
            var biomeKey = biomeContext.getKey();
            return biomeKey.equals(Biomes.BEACH) ||
                    biomeKey.equals(Biomes.SNOWY_BEACH) ||
                    biomeKey.equals(Biomes.STONY_SHORE) ||
                    biomeKey.equals(Biomes.RIVER) ||
                    biomeKey.equals(Biomes.FROZEN_RIVER) ||
                    biomeKey.equals(Biomes.DESERT);
        }, (biomeContext, mutable) -> {
            mutable.getGenerationProperties().addFeature(
                    GenerationStep.Decoration.UNDERGROUND_ORES,
                    ResourceKey.create(Registries.PLACED_FEATURE,
                            new ResourceLocation(BreathOfNichirin.MOD_ID, "scarlet_crimson_iron_sand"))
            );
        });

        // Add Scarlet Ore to mountain biomes
        BiomeModifications.addProperties(biomeContext -> {
            var biomeKey = biomeContext.getKey();
            return biomeKey.equals(Biomes.WINDSWEPT_HILLS) ||
                    biomeKey.equals(Biomes.WINDSWEPT_GRAVELLY_HILLS) ||
                    biomeKey.equals(Biomes.WINDSWEPT_FOREST) ||
                    biomeKey.equals(Biomes.MEADOW) ||
                    biomeKey.equals(Biomes.FROZEN_PEAKS) ||
                    biomeKey.equals(Biomes.JAGGED_PEAKS) ||
                    biomeKey.equals(Biomes.STONY_PEAKS) ||
                    biomeKey.equals(Biomes.SNOWY_SLOPES);
        }, (biomeContext, mutable) -> {
            mutable.getGenerationProperties().addFeature(
                    GenerationStep.Decoration.UNDERGROUND_ORES,
                    ResourceKey.create(Registries.PLACED_FEATURE,
                            new ResourceLocation(BreathOfNichirin.MOD_ID, "scarlet_ore"))
            );
        });
    }
}