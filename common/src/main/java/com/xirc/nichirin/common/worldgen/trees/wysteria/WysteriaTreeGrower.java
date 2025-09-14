package com.xirc.nichirin.common.worldgen.trees.wysteria;

import com.xirc.nichirin.registry.NichirinConfiguredFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;

/**
 * Tree grower for Wisteria saplings
 * Randomly selects between small, medium, and large wisteria variants
 */
public class WysteriaTreeGrower extends AbstractTreeGrower {

    @Override
    protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource random, boolean hasFlowers) {
        // Weighted random selection for tree variants
        int roll = random.nextInt(100);

        if (roll < 50) {
            // 50% chance for small wisteria (most common)
            return NichirinConfiguredFeatures.SMALL_WYSTERIA;
        } else if (roll < 80) {
            // 30% chance for medium wisteria
            return NichirinConfiguredFeatures.MEDIUM_WYSTERIA;
        } else {
            // 20% chance for large wisteria (rarest)
            return NichirinConfiguredFeatures.LARGE_WYSTERIA;
        }
    }
}