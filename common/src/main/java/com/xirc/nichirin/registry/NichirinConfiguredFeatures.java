package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public interface NichirinConfiguredFeatures {

    // Wisteria tree variants
    ResourceKey<ConfiguredFeature<?, ?>> SMALL_WYSTERIA = createKey("small_wysteria");
    ResourceKey<ConfiguredFeature<?, ?>> MEDIUM_WYSTERIA = createKey("medium_wysteria");
    ResourceKey<ConfiguredFeature<?, ?>> LARGE_WYSTERIA = createKey("large_wysteria");

    // Helper method to create resource keys
    private static ResourceKey<ConfiguredFeature<?, ?>> createKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, name));
    }
}