package com.xirc.nichirin.fabric.common.world;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.registry.OreRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
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
            new ResourceLocation(BreathOfNichirin.MOD_ID, "scarlet_crimson_iron_sand")
    );

    public static final ResourceKey<ConfiguredFeature<?, ?>> SCARLET_ORE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            new ResourceLocation(BreathOfNichirin.MOD_ID, "scarlet_ore")
    );

    public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest sandReplaceable = new TagMatchTest(BlockTags.SAND);
        RuleTest stoneReplaceable = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);

        // Scarlet Crimson Iron Sand - generates in sand with 0.0 air exposure discard for surface generation
        context.register(SCARLET_CRIMSON_IRON_SAND, new ConfiguredFeature<>(
                Feature.ORE,
                new OreConfiguration(
                        List.of(OreConfiguration.target(sandReplaceable, OreRegistry.SCARLET_CRIMSON_IRON_SAND.get().defaultBlockState())),
                        4, // vein size
                        0.0f // CRITICAL: 0.0 air exposure discard chance for surface generation
                )
        ));

        // Scarlet Ore - generates in stone with 0.0 air exposure discard for surface generation
        context.register(SCARLET_ORE, new ConfiguredFeature<>(
                Feature.ORE,
                new OreConfiguration(
                        List.of(OreConfiguration.target(stoneReplaceable, OreRegistry.SCARLET_ORE.get().defaultBlockState())),
                        6, // vein size
                        0.0f // CRITICAL: 0.0 air exposure discard chance for surface generation
                )
        ));
    }
}