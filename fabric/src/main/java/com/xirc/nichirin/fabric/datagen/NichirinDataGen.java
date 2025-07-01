package com.xirc.nichirin.fabric.datagen;

import com.xirc.nichirin.common.world.NichirinConfiguredFeatures;
import com.xirc.nichirin.common.world.NichirinPlacedFeatures;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

public final class NichirinDataGen implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        final FabricDataGenerator.Pack pack = generator.createPack();

        pack.addProvider(NichirinWorldProvider::new);
        pack.addProvider(NichirinRecipeProvider::new);
    }

    @Override
    public void buildRegistry(RegistrySetBuilder registryBuilder) {
        registryBuilder.add(Registries.CONFIGURED_FEATURE, NichirinConfiguredFeatures::bootstrap);
        registryBuilder.add(Registries.PLACED_FEATURE, NichirinPlacedFeatures::bootstrap);
    }
}