package com.xirc.nichirin.common.worldgen.trees.wysteria;

import com.xirc.nichirin.registry.NichirinConfiguredFeatures;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;

/**
 * Tree grower for Wisteria saplings
 * Randomly selects between small, medium, and large wisteria variants
 */
public final class WysteriaTreeGrower {

    static final TreeGrower SMALL = create("small", NichirinConfiguredFeatures.SMALL_WYSTERIA);
    private static final TreeGrower MEDIUM = create("medium", NichirinConfiguredFeatures.MEDIUM_WYSTERIA);
    private static final TreeGrower LARGE = create("large", NichirinConfiguredFeatures.LARGE_WYSTERIA);

    private WysteriaTreeGrower() {}

    static TreeGrower select(RandomSource random) {
        // Weighted random selection for tree variants
        int roll = random.nextInt(100);

        if (roll < 50) {
            // 50% chance for small wisteria (most common)
            return SMALL;
        } else if (roll < 80) {
            // 30% chance for medium wisteria
            return MEDIUM;
        } else {
            // 20% chance for large wisteria (rarest)
            return LARGE;
        }
    }

    private static TreeGrower create(String size, net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> feature) {
        return new TreeGrower("nichirin:wysteria_" + size, Optional.empty(), Optional.of(feature), Optional.empty());
    }
}