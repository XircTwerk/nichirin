package com.xirc.nichirin.common.worldgen.trees.wisteria;

import com.xirc.nichirin.registry.NichirinConfiguredFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

import java.util.Optional;

/**
 * Tree grower for Wisteria saplings
 * Randomly selects between small, medium, and large wisteria variants
 */
public final class WisteriaTreeGrower {

    static final TreeGrower SMALL = create("small", NichirinConfiguredFeatures.SMALL_WISTERIA);
    private static final TreeGrower MEDIUM = create("medium", NichirinConfiguredFeatures.MEDIUM_WISTERIA);
    private static final TreeGrower LARGE = create("large", NichirinConfiguredFeatures.LARGE_WISTERIA);

    private WisteriaTreeGrower() {}

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

    private static TreeGrower create(String size, ResourceKey<ConfiguredFeature<?, ?>> feature) {
        return new TreeGrower("nichirin:wisteria_" + size, Optional.empty(), Optional.of(feature), Optional.empty());
    }
}
