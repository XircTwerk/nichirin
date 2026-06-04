package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public interface NichirinConfiguredFeatures {

    // Wisteria tree variants
    ResourceKey<ConfiguredFeature<?, ?>> SMALL_WISTERIA = createKey("small_wisteria");
    ResourceKey<ConfiguredFeature<?, ?>> MEDIUM_WISTERIA = createKey("medium_wisteria");
    ResourceKey<ConfiguredFeature<?, ?>> LARGE_WISTERIA = createKey("large_wisteria");

    // Helper method to create resource keys
    private static ResourceKey<ConfiguredFeature<?, ?>> createKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE,
                ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, name));
    }
}