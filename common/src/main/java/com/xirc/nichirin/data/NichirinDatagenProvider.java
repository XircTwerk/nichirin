package com.xirc.nichirin.data;

import com.xirc.nichirin.common.registry.OreRegistry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NichirinDatagenProvider {

    // Define ResourceKeys for your features
    public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_ORE_CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation("nichirin", "scarlet_ore"));

    public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_SAND_CONFIGURED =
            ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation("nichirin", "scarlet_crimson_iron_sand"));

    public static final ResourceKey<PlacedFeature> SCARLET_ORE_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation("nichirin", "scarlet_ore_placed"));

    public static final ResourceKey<PlacedFeature> SCARLET_SAND_PLACED =
            ResourceKey.create(Registries.PLACED_FEATURE, new ResourceLocation("nichirin", "scarlet_sand_placed"));

    // Create the registry builder
    public static RegistrySetBuilder createBuilder() {
        return new RegistrySetBuilder()
                .add(Registries.CONFIGURED_FEATURE, NichirinDatagenProvider::configureFeatures)
                .add(Registries.PLACED_FEATURE, NichirinDatagenProvider::placeFeatures);
    }

    private static void configureFeatures(BootstapContext<ConfiguredFeature<?, ?>> context) {
        var stoneOres = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        var sandReplaceable = new BlockMatchTest(Blocks.SAND);

        // Register scarlet ore configured feature
        context.register(SCARLET_ORE_CONFIGURED,
                new ConfiguredFeature<>(Feature.ORE,
                        new OreConfiguration(
                                List.of(OreConfiguration.target(stoneOres, OreRegistry.SCARLET_ORE.get().defaultBlockState())),
                                4, // vein size
                                0.0f // no air exposure discard for surface ores
                        )
                )
        );

        // Register scarlet sand configured feature
        context.register(SCARLET_SAND_CONFIGURED,
                new ConfiguredFeature<>(Feature.ORE,
                        new OreConfiguration(
                                List.of(OreConfiguration.target(sandReplaceable, OreRegistry.SCARLET_CRIMSON_IRON_SAND.get().defaultBlockState())),
                                6, // vein size
                                0.0f // no air exposure discard for surface ores
                        )
                )
        );
    }

    private static void placeFeatures(BootstapContext<PlacedFeature> context) {
        var configured = context.lookup(Registries.CONFIGURED_FEATURE);

        // Scarlet ore placement - surface mountains
        context.register(SCARLET_ORE_PLACED,
                new PlacedFeature(
                        configured.getOrThrow(SCARLET_ORE_CONFIGURED),
                        List.of(
                                CountPlacement.of(5), // attempts per chunk
                                InSquarePlacement.spread(),
                                HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                                RandomOffsetPlacement.vertical(UniformInt.of(-3, 0)),
                                BiomeFilter.biome()
                        )
                )
        );

        // Scarlet sand placement - surface deserts/beaches
        context.register(SCARLET_SAND_PLACED,
                new PlacedFeature(
                        configured.getOrThrow(SCARLET_SAND_CONFIGURED),
                        List.of(
                                CountPlacement.of(10), // attempts per chunk
                                InSquarePlacement.spread(),
                                HeightmapPlacement.onHeightmap(Heightmap.Types.OCEAN_FLOOR_WG),
                                RandomOffsetPlacement.vertical(UniformInt.of(-2, 1)),
                                BiomeFilter.biome()
                        )
                )
        );
    }
}