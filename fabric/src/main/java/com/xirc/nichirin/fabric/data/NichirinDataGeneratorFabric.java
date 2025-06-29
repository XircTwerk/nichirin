package com.xirc.nichirin.fabric.data;

import com.xirc.nichirin.data.NichirinData;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;

import java.util.concurrent.CompletableFuture;

public class NichirinDataGeneratorFabric implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(NichirinWorldGenProvider::new);
    }

    @Override
    public void buildRegistry(RegistrySetBuilder builder) {
        // Use the common registry builder
    }

    private static class NichirinWorldGenProvider extends FabricDynamicRegistryProvider {
        public NichirinWorldGenProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected void configure(HolderLookup.Provider registries, Entries entries) {
            // The actual registration happens through the RegistrySetBuilder
        }

        @Override
        public String getName() {
            return "Nichirin World Gen";
        }
    }
}