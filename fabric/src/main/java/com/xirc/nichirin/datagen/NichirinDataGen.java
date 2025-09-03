package com.xirc.nichirin.datagen;

import com.xirc.nichirin.datagen.providers.assets.NichirinModelProvider;
import com.xirc.nichirin.datagen.providers.data.*;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;

public final class NichirinDataGen implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        final FabricDataGenerator.Pack pack = generator.createPack();

        // Add the model provider for assets generation
        pack.addProvider(NichirinModelProvider::new);

        // Add the loot table provider
        pack.addProvider(NichirinLootTableProvider::new);

        // Your existing providers that you already have
        pack.addProvider(NichirinWorldProvider::new);
        pack.addProvider(NichirinRecipeProvider::new);
        pack.addProvider(NichirinTagProvider::new);
        pack.addProvider(NichirinTagProvider.ItemTagProvider::new);
        pack.addProvider(NichirinAdvancementProvider::new);
    }

    @Override
    public void buildRegistry(RegistrySetBuilder registryBuilder) {
    }
}