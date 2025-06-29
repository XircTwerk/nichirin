package com.xirc.nichirin.common.world;

import com.xirc.nichirin.common.registry.OreRegistry;
import dev.architectury.registry.level.biome.BiomeModifications;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OreGeneration {
    private static final Logger LOGGER = LoggerFactory.getLogger("Nichirin");

    // Configured Features - these reference the JSON files
    public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_ORE_CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    new ResourceLocation("nichirin", "scarlet_ore"));

    public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_CRIMSON_IRON_SAND_CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    new ResourceLocation("nichirin", "scarlet_crimson_iron_sand"));

    // Placed Features - these reference the JSON files
    public static final ResourceKey<PlacedFeature> SCARLET_ORE_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE,
                    new ResourceLocation("nichirin", "scarlet_ore_surface"));

    public static final ResourceKey<PlacedFeature> SCARLET_CRIMSON_IRON_SAND_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE,
                    new ResourceLocation("nichirin", "scarlet_sand_surface"));

    public static void registerOreGeneration() {
        LOGGER.info("Registering Nichirin surface ore generation...");

        // Register scarlet ore in mountain biomes
        BiomeModifications.addProperties(
                (context) -> {
                    try {
                        return context.hasTag(BiomeTags.IS_MOUNTAIN);
                    } catch (Exception e) {
                        LOGGER.error("Error checking mountain biome tag: " + e.getMessage());
                        return false;
                    }
                },
                (context, mutable) -> {
                    try {
                        mutable.getGenerationProperties().addFeature(
                                GenerationStep.Decoration.UNDERGROUND_ORES,
                                SCARLET_ORE_PLACED
                        );
                        LOGGER.info("Added scarlet ore generation to mountain biomes");
                    } catch (Exception e) {
                        LOGGER.error("Failed to register scarlet ore: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
        );

        // Register scarlet crimson iron sand in desert/beach biomes
        BiomeModifications.addProperties(
                (context) -> {
                    try {
                        return context.hasTag(BiomeTags.IS_BEACH) ||
                                context.hasTag(BiomeTags.HAS_DESERT_PYRAMID);
                    } catch (Exception e) {
                        LOGGER.error("Error checking desert/beach biome tags: " + e.getMessage());
                        return false;
                    }
                },
                (context, mutable) -> {
                    try {
                        mutable.getGenerationProperties().addFeature(
                                GenerationStep.Decoration.UNDERGROUND_ORES,
                                SCARLET_CRIMSON_IRON_SAND_PLACED
                        );
                        LOGGER.info("Added scarlet crimson iron sand generation to desert/beach biomes");
                    } catch (Exception e) {
                        LOGGER.error("Failed to register scarlet crimson iron sand: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
        );

        LOGGER.info("Surface ore generation registration complete");
    }
}