package com.xirc.nichirin.common.worldgen.trees.wysteria;

import com.xirc.nichirin.registry.NichirinBlockRegistry;
import com.xirc.nichirin.registry.NichirinConfiguredFeatures;
import net.minecraft.data.worldgen.BootstapContext;
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

public class WysteriaConfiguredFeatures {

    public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
        register(context, NichirinConfiguredFeatures.SMALL_WYSTERIA, Feature.TREE, createSmallWysteria().build());
        register(context, NichirinConfiguredFeatures.MEDIUM_WYSTERIA, Feature.TREE, createMediumWysteria().build());
        register(context, NichirinConfiguredFeatures.LARGE_WYSTERIA, Feature.TREE, createLargeWysteria().build());
    }

    private static TreeConfiguration.TreeConfigurationBuilder createSmallWysteria() {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(NichirinBlockRegistry.WYSTERIA_LOG.get()),
                new WysteriaTrunkPlacer(3, 2, 1, UniformInt.of(1, 3)), // Short, can branch early
                BlockStateProvider.simple(NichirinBlockRegistry.WYSTERIA_LEAVES.get()),
                new WysteriaSmallFoliagePlacer(ConstantInt.of(2), ConstantInt.of(0)),
                new TwoLayersFeatureSize(1, 0, 1)
        ).decorators(List.of(
                new WysteriaRootDecorator(0.3f), // 30% chance for surface roots
                new WysteriaHangingLeavesDecorator(2, 5) // Short hanging clusters
        ));
    }

    private static TreeConfiguration.TreeConfigurationBuilder createMediumWysteria() {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(NichirinBlockRegistry.WYSTERIA_LOG.get()),
                new WysteriaTrunkPlacer(4, 3, 2, UniformInt.of(2, 4)), // Medium height, more branches
                BlockStateProvider.simple(NichirinBlockRegistry.WYSTERIA_LEAVES.get()),
                new WysteriaMediumFoliagePlacer(ConstantInt.of(3), ConstantInt.of(1)),
                new TwoLayersFeatureSize(2, 0, 2)
        ).decorators(List.of(
                new WysteriaRootDecorator(0.6f), // 60% chance for surface roots
                new WysteriaHangingLeavesDecorator(4, 8) // Medium hanging clusters
        ));
    }

    private static TreeConfiguration.TreeConfigurationBuilder createLargeWysteria() {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(NichirinBlockRegistry.WYSTERIA_LOG.get()),
                new WysteriaTrunkPlacer(6, 4, 3, UniformInt.of(3, 6)), // THIS is what creates branches
                BlockStateProvider.simple(NichirinBlockRegistry.WYSTERIA_LEAVES.get()),
                new WysteriaLargeFoliagePlacer(ConstantInt.of(4), ConstantInt.of(2)),
                new TwoLayersFeatureSize(3, 0, 3)
        ).decorators(List.of(  // THIS adds the roots
                new WysteriaRootDecorator(0.8f),
                new WysteriaHangingLeavesDecorator(6, 12)
        ));
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>>
    void register(BootstapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC configuration) {
        context.register(key, new ConfiguredFeature<>(feature, configuration));
    }
}
