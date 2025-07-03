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

        // Vanilla emerald generates with CountPlacement.of(3-8) and no RarityFilter
        // For 1.5x frequency, we'll use CountPlacement.of(5-12)

        // Scarlet Crimson Iron Sand - emerald-like rarity but 1.5x more common
        context.register(SCARLET_CRIMSON_IRON_SAND, new PlacedFeature(
                scarletCrimsonIronSandConfigured,
                List.of(
                        CountPlacement.of(3), // Emerald uses 3-8, so we use 5 (middle of 4.5-12)
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(60), VerticalAnchor.absolute(256))
                        // No RarityFilter - emeralds don't use one
                )
        ));

        // Scarlet Ore - emerald-like rarity but 1.5x more common
        context.register(SCARLET_ORE, new PlacedFeature(
                scarletOreConfigured,
                List.of(
                        CountPlacement.of(5), // Same as above
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(80), VerticalAnchor.absolute(256))
                        // No RarityFilter - emeralds don't use one
                )
        ));
    }
}