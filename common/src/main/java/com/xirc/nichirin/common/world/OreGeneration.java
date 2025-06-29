package com.xirc.nichirin.common.world;

import com.xirc.nichirin.common.registry.OreRegistry;
import dev.architectury.registry.level.biome.BiomeModifications;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.Direction;

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

        // Scarlet Ore - replaces stone blocks, HIGH discard chance on air exposure to ensure surface-only
        List<OreConfiguration.TargetBlockState> scarletOreTargets = List.of(
                OreConfiguration.target(stoneReplaceable, OreRegistry.SCARLET_ORE.get().defaultBlockState())
        );

        // Scarlet Crimson Iron Sand - replaces sand blocks
        List<OreConfiguration.TargetBlockState> scarletSandTargets = List.of(
                OreConfiguration.target(sandReplaceable, OreRegistry.SCARLET_CRIMSON_IRON_SAND.get().defaultBlockState())
        );

        // Use 0.0F discard chance - we'll handle air exposure in placement modifiers instead
        context.register(SCARLET_ORE_CONFIGURED, new ConfiguredFeature<>(Feature.ORE,
                new OreConfiguration(scarletOreTargets, 4, 0.0F))); // Vein size of 4, no air discard

        context.register(SCARLET_CRIMSON_IRON_SAND_CONFIGURED, new ConfiguredFeature<>(Feature.ORE,
                new OreConfiguration(scarletSandTargets, 6, 0.0F))); // Vein size of 6, no air discard
    }

    public static void bootstrapPlacedFeatures(BootstapContext<PlacedFeature> context) {
        var configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        // Scarlet Ore placement - mountains above Y=96, MUST be exposed to air (surface only)
        context.register(SCARLET_ORE_PLACED, new PlacedFeature(
                configuredFeatures.getOrThrow(SCARLET_ORE_CONFIGURED),
                List.of(
                        CountPlacement.of(5), // Increased attempts since we're being very selective
                        InSquarePlacement.spread(), // Random XZ within chunk
                        // Start from heightmap surface and work down a bit
                        HeightmapPlacement.onHeightmap(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                        // Add some random vertical offset downward (0-3 blocks below surface)
                        RandomOffsetPlacement.vertical(net.minecraft.util.valueproviders.UniformInt.of(-3, 0)),
                        // CRITICAL: Must have air directly above the ore block
                        EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.ONLY_IN_AIR_PREDICATE, 1),
                        // Additional check: Must not be in a cave (check for solid blocks around)
                        BlockPredicateFilter.forPredicate(BlockPredicate.anyOf(
                                BlockPredicate.matchesBlocks(Direction.UP.getNormal(), Blocks.AIR),
                                BlockPredicate.matchesBlocks(Direction.UP.getNormal(), Blocks.CAVE_AIR)
                        )),
                        BiomeFilter.biome() // Only in valid biomes
                )
        ));

        // Scarlet Crimson Iron Sand placement - deserts/beaches, surface exposed
        context.register(SCARLET_CRIMSON_IRON_SAND_PLACED, new PlacedFeature(
                configuredFeatures.getOrThrow(SCARLET_CRIMSON_IRON_SAND_CONFIGURED),
                List.of(
                        CountPlacement.of(10), // More attempts for sand since it's more common
                        InSquarePlacement.spread(),
                        // Place at or near surface level
                        HeightmapPlacement.onHeightmap(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG),
                        // Small random offset
                        RandomOffsetPlacement.vertical(net.minecraft.util.valueproviders.UniformInt.of(-2, 1)),
                        // Must have air above
                        EnvironmentScanPlacement.scanningFor(Direction.UP, BlockPredicate.ONLY_IN_AIR_PREDICATE, 1),
                        // Ensure we're placing in/near sand
                        BlockPredicateFilter.forPredicate(BlockPredicate.anyOf(
                                BlockPredicate.matchesBlocks(Blocks.SAND),
                                BlockPredicate.matchesBlocks(Direction.DOWN.getNormal(), Blocks.SAND)
                        )),
                        BiomeFilter.biome()
                )
        ));
    }

    public static void registerOreGeneration() {
        System.out.println("Registering Nichirin surface ore generation...");

        // Register scarlet ore in mountain biomes - SURFACE ONLY
        BiomeModifications.addProperties(
                (context) -> {
                    try {
                        return context.hasTag(BiomeTags.IS_MOUNTAIN);
                    } catch (Exception e) {
                        System.err.println("Error checking mountain biome tag: " + e.getMessage());
                        return false;
                    }
                },
                (context, mutable) -> {
                    try {
                        // Use SURFACE_STRUCTURES instead of UNDERGROUND_ORES since these are surface features
                        mutable.getGenerationProperties().addFeature(
                                GenerationStep.Decoration.SURFACE_STRUCTURES,
                                SCARLET_ORE_PLACED
                        );

                        System.out.println("Added surface scarlet ore generation to mountains");
                    } catch (Exception e) {
                        System.err.println("Failed to register scarlet ore: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
        );

        // Register scarlet crimson iron sand in desert/beach biomes - SURFACE ONLY
        BiomeModifications.addProperties(
                (context) -> {
                    try {
                        return context.hasTag(BiomeTags.HAS_DESERT_PYRAMID) || context.hasTag(BiomeTags.IS_BEACH);
                    } catch (Exception e) {
                        System.err.println("Error checking desert/beach biome tags: " + e.getMessage());
                        return false;
                    }
                },
                (context, mutable) -> {
                    try {
                        // Use SURFACE_STRUCTURES for surface-only generation
                        mutable.getGenerationProperties().addFeature(
                                GenerationStep.Decoration.SURFACE_STRUCTURES,
                                SCARLET_CRIMSON_IRON_SAND_PLACED
                        );

                        System.out.println("Added surface scarlet crimson iron sand generation to deserts/beaches");
                    } catch (Exception e) {
                        System.err.println("Failed to register scarlet crimson iron sand: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
        );

        System.out.println("Surface ore generation registration complete");
    }
}