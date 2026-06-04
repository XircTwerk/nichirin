package com.xirc.nichirin.datagen;

import com.xirc.nichirin.common.worldgen.trees.wisteria.WisteriaConfiguredFeatures;
import com.xirc.nichirin.datagen.providers.assets.NichirinModelProvider;
import com.xirc.nichirin.datagen.providers.data.*;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

public final class NichirinDataGen implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        final FabricDataGenerator.Pack pack = generator.createPack();

        // Add the model provider for assets generation
        pack.addProvider(NichirinModelProvider::new);

        pack.addProvider(NichirinLootTableProvider::new);
        pack.addProvider(NichirinWorldProvider::new);
        pack.addProvider(NichirinRecipeProvider::new);
        pack.addProvider(NichirinTagProvider::new);
        pack.addProvider(NichirinTagProvider.ItemTagProvider::new);
        pack.addProvider(NichirinAdvancementProvider::new);
    }

    @Override
    public void buildRegistry(RegistrySetBuilder registryBuilder) {
        registryBuilder.add(Registries.CONFIGURED_FEATURE, WisteriaConfiguredFeatures::bootstrap);
    }
}