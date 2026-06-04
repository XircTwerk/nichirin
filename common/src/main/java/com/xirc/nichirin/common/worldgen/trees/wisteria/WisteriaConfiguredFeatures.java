package com.xirc.nichirin.common.worldgen.trees.wisteria;

import com.xirc.nichirin.registry.NichirinBlockRegistry;
import com.xirc.nichirin.registry.NichirinConfiguredFeatures;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import java.util.List;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

public class WisteriaConfiguredFeatures {

    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        register(context, NichirinConfiguredFeatures.SMALL_WISTERIA, Feature.TREE, createSmallWisteria().build());
        register(context, NichirinConfiguredFeatures.MEDIUM_WISTERIA, Feature.TREE, createMediumWisteria().build());
        register(context, NichirinConfiguredFeatures.LARGE_WISTERIA, Feature.TREE, createLargeWisteria().build());
    }

    private static TreeConfiguration.TreeConfigurationBuilder createSmallWisteria() {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(NichirinBlockRegistry.WISTERIA_LOG.get()),
                new WisteriaTrunkPlacer(3, 2, 1, UniformInt.of(1, 3)), // Short, can branch early
                BlockStateProvider.simple(NichirinBlockRegistry.WISTERIA_LEAVES.get()),
                new WisteriaSmallFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0)),
                new TwoLayersFeatureSize(1, 0, 1)
        ).decorators(List.of(
                new WisteriaRootDecorator(0.3f), // 30% chance for surface roots
                new WisteriaHangingLeavesDecorator(2, 5) // Short hanging clusters
        ));
    }

    private static TreeConfiguration.TreeConfigurationBuilder createMediumWisteria() {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(NichirinBlockRegistry.WISTERIA_LOG.get()),
                new WisteriaTrunkPlacer(4, 3, 2, UniformInt.of(2, 4)), // Medium height, more branches
                BlockStateProvider.simple(NichirinBlockRegistry.WISTERIA_LEAVES.get()),
                new WisteriaMediumFoliagePlacer(ConstantInt.of(3), ConstantInt.of(1)),
                new TwoLayersFeatureSize(2, 0, 2)
        ).decorators(List.of(
                new WisteriaRootDecorator(0.6f), // 60% chance for surface roots
                new WisteriaHangingLeavesDecorator(4, 8) // Medium hanging clusters
        ));
    }

    private static TreeConfiguration.TreeConfigurationBuilder createLargeWisteria() {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(NichirinBlockRegistry.WISTERIA_LOG.get()),
                new WisteriaTrunkPlacer(6, 4, 3, UniformInt.of(3, 6)), // THIS is what creates branches
                BlockStateProvider.simple(NichirinBlockRegistry.WISTERIA_LEAVES.get()),
                new WisteriaLargeFoliagePlacer(ConstantInt.of(4), ConstantInt.of(2)),
                new TwoLayersFeatureSize(3, 0, 3)
        ).decorators(List.of(  // THIS adds the roots
                new WisteriaRootDecorator(0.8f),
                new WisteriaHangingLeavesDecorator(6, 12)
        ));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>>
    void register(BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}