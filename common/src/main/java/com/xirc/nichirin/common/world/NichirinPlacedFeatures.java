package com.xirc.nichirin.common.world;

import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.*;

import java.util.List;

public class NichirinPlacedFeatures {
    public static final ResourceKey<PlacedFeature> SCARLET_CRIMSON_IRON_SAND = ResourceKey.create(
            Registries.PLACED_FEATURE,
            new ResourceLocation(BreathOfNichirin.MOD_ID, "scarlet_crimson_iron_sand")
    );

    public static final ResourceKey<PlacedFeature> SCARLET_ORE = ResourceKey.create(
            Registries.PLACED_FEATURE,
            new ResourceLocation(BreathOfNichirin.MOD_ID, "scarlet_ore")
    );

    public static void bootstrap(BootstapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configuredFeatures = context.lookup(Registries.CONFIGURED_FEATURE);

        Holder<ConfiguredFeature<?, ?>> scarletCrimsonIronSandConfigured = configuredFeatures.getOrThrow(NichirinConfiguredFeatures.SCARLET_CRIMSON_IRON_SAND);
        Holder<ConfiguredFeature<?, ?>> scarletOreConfigured = configuredFeatures.getOrThrow(NichirinConfiguredFeatures.SCARLET_ORE);

        // Scarlet Crimson Iron Sand - generates near sand at beach level
        context.register(SCARLET_CRIMSON_IRON_SAND, new PlacedFeature(
                scarletCrimsonIronSandConfigured,
                List.of(
                        CountPlacement.of(4), // 4 attempts per chunk
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(40), VerticalAnchor.absolute(80)), // Beach/river levels
                        BiomeFilter.biome()
                )
        ));

        // Scarlet Ore - only above Y=80 in mountain biomes
        context.register(SCARLET_ORE, new PlacedFeature(
                scarletOreConfigured,
                List.of(
                        CountPlacement.of(8), // 8 attempts per chunk
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(80), VerticalAnchor.absolute(256)), // Above Y=80
                        BiomeFilter.biome()
                )
        ));
    }
}