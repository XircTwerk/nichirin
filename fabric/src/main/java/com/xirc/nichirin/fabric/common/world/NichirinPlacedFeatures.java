package com.xirc.nichirin.fabric.common.world;

import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

        // Scarlet Crimson Iron Sand - surface generation near sand above Y=60
        context.register(SCARLET_CRIMSON_IRON_SAND, new PlacedFeature(
                scarletCrimsonIronSandConfigured,
                List.of(
                        CountPlacement.of(3), // Reduced from 6 to 3
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(60), VerticalAnchor.absolute(256)),
                        RarityFilter.onAverageOnceEvery(16) // Increased rarity from 8 to 16
                )
        ));

        // Scarlet Ore - surface generation in mountains above Y=80
        context.register(SCARLET_ORE, new PlacedFeature(
                scarletOreConfigured,
                List.of(
                        CountPlacement.of(20), // Increased from 12 to 20 for better generation
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(80), VerticalAnchor.absolute(256)), // Extended to world height
                        RarityFilter.onAverageOnceEvery(2) // Reduced rarity from 4 to 2
                )
        ));
    }
}