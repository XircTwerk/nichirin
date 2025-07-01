package com.xirc.nichirin.fabric.datagen;

import com.xirc.nichirin.common.world.NichirinConfiguredFeatures;
import com.xirc.nichirin.common.world.NichirinPlacedFeatures;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class NichirinWorldProvider extends FabricDynamicRegistryProvider {
    public NichirinWorldProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(HolderLookup.Provider registries, Entries entries) {
        entries.addAll(registries.lookupOrThrow(Registries.CONFIGURED_FEATURE));
        entries.addAll(registries.lookupOrThrow(Registries.PLACED_FEATURE));
    }

    protected void buildRegistry(RegistrySetBuilder registryBuilder) {
        registryBuilder.add(Registries.CONFIGURED_FEATURE, NichirinConfiguredFeatures::bootstrap);
        registryBuilder.add(Registries.PLACED_FEATURE, NichirinPlacedFeatures::bootstrap);
    }

    @Override
    public @NotNull String getName() {
        return "World Gen";
    }
}