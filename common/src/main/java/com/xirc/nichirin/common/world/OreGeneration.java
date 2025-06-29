package com.xirc.nichirin.common.world;

import com.xirc.nichirin.common.registry.OreRegistry;
import dev.architectury.registry.level.biome.BiomeModifications;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import dev.architectury.registry.level.biome.BiomeModifications.BiomeContext;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.tags.BlockTags;

import java.util.List;

public class OreGeneration {

    // Configured Features
    public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_ORE_CONFIGURED = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            new ResourceLocation("nichirin", "scarlet_ore"));

    public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_CRIMSON_IRON_SAND_CONFIGURED = ResourceKey.create(Registries.CONFIGURED_FEATURE,
            new ResourceLocation("nichirin", "scarlet_crimson_iron_sand"));

    // Placed Features
    public static final ResourceKey<PlacedFeature> SCARLET_ORE_PLACED = ResourceKey.create(Registries.PLACED_FEATURE,
            new ResourceLocation("nichirin", "scarlet_ore"));

    public static final ResourceKey<PlacedFeature> SCARLET_CRIMSON_IRON_SAND_PLACED = ResourceKey.create(Registries.PLACED_FEATURE,
            new ResourceLocation("nichirin", "scarlet_crimson_iron_sand"));

    public static void bootstrapConfiguredFeatures(BootstapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest stoneReplaceable = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest sandReplaceable = new BlockMatchTest(Blocks.SAND);

        // Scarlet Ore - replaces stone blocks
        List<OreConfiguration.TargetBlockState> scarletOreTargets = List.of(
                OreConfiguration.target(stoneReplaceable, OreRegistry.SCARLET_ORE.get().defaultBlockState())
        );

        // Scarlet Crimson Iron Sand - replaces sand blocks
        List<OreConfiguration.TargetBlockState> scarletSandTargets = List.of(
                OreConfiguration.target(sandReplaceable, OreRegistry.SCARLET_CRIMSON_IRON_SAND.get().defaultBlockState())
        );

        context.register(SCARLET_ORE_CONFIGURED, new ConfiguredFeature<>(Feature.ORE,
                new OreConfiguration(scarletOreTargets, 4))); // Vein size of 4

        context.register(SCARLET_CRIMSON_IRON_SAND_CONFIGURED, new ConfiguredFeature<>(Feature.ORE,
                new OreConfiguration(scarletSandTargets, 6))); // Vein size of 6
    }

    public static void bootstrapPlacedFeatures(BootstapContext<PlacedFeature> context) {
        var configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        // Scarlet Ore placement - mountains above Y=96, exposed to sky
        context.register(SCARLET_ORE_PLACED, new PlacedFeature(
                configuredFeatures.getOrThrow(SCARLET_ORE_CONFIGURED),
                List.of(
                        CountPlacement.of(3), // 3 attempts per chunk
                        InSquarePlacement.spread(), // Random XZ within chunk
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(96), VerticalAnchor.top()), // Y >= 96
                        EnvironmentScanPlacement.scanningFor(net.minecraft.core.Direction.UP,
                                net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate.ONLY_IN_AIR_PREDICATE, 1), // Must have air above (exposed to sky)
                        BiomeFilter.biome() // Only in valid biomes
                )
        ));

        // Scarlet Crimson Iron Sand placement - near sand, Y >= 70
        context.register(SCARLET_CRIMSON_IRON_SAND_PLACED, new PlacedFeature(
                configuredFeatures.getOrThrow(SCARLET_CRIMSON_IRON_SAND_CONFIGURED),
                List.of(
                        CountPlacement.of(8), // 8 attempts per chunk
                        InSquarePlacement.spread(), // Random XZ within chunk
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(70), VerticalAnchor.top()), // Y >= 70
                        BiomeFilter.biome() // Only in valid biomes
                )
        ));
    }

    public static void registerOreGeneration() {
        // Register Scarlet Ore in mountain biomes
        BiomeModifications.addProperties(
                biomeContext -> biomeContext.hasTag(BiomeTags.IS_MOUNTAIN),
                (biomeContext, mutable) -> {
                    mutable.getGenerationProperties().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, SCARLET_ORE_PLACED);
                }
        );

        // Register Scarlet Crimson Iron Sand in desert/sandy biomes (where sand is common)
        BiomeModifications.addProperties(
                biomeContext -> biomeContext.hasTag(BiomeTags.HAS_DESERT_PYRAMID) ||
                        biomeContext.hasTag(BiomeTags.IS_BEACH),
                (biomeContext, mutable) -> {
                    mutable.getGenerationProperties().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, SCARLET_CRIMSON_IRON_SAND_PLACED);
                }
        );
    }
}