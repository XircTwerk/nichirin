package com.xirc.nichirin.common.world;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.registry.NichirinBlockRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class NichirinConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_CRIMSON_IRON_SAND = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "scarlet_crimson_iron_sand")
    );

    public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_ORE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "scarlet_ore")
    );

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest sandReplaceable = new TagMatchTest(BlockTags.SAND);
        RuleTest stoneReplaceable = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);

        // Scarlet Crimson Iron Sand - generates in sand with 0.0 air exposure discard for surface generation
        context.register(SCARLET_CRIMSON_IRON_SAND, new ConfiguredFeature<>(
                Feature.ORE,
                new OreConfiguration(
                        List.of(OreConfiguration.target(sandReplaceable, NichirinBlockRegistry.SCARLET_CRIMSON_IRON_SAND.get().defaultBlockState())),
                        4, // vein size
                        0.0f // 0.0 air exposure discard chance for surface generation
                )
        ));

        // Scarlet Ore - generates in stone with 0.0 air exposure discard for surface generation
        context.register(SCARLET_ORE, new ConfiguredFeature<>(
                Feature.ORE,
                new OreConfiguration(
                        List.of(OreConfiguration.target(stoneReplaceable, NichirinBlockRegistry.SCARLET_ORE.get().defaultBlockState())),
                        6, // vein size
                        0.0f // 0.0 air exposure discard chance for surface generation
                )
        ));
    }
}