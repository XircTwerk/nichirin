package com.xirc.nichirin.forge.datagen;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.forge.common.world.NichirinBiomeModifications;
import com.xirc.nichirin.forge.common.world.NichirinConfiguredFeatures;
import com.xirc.nichirin.forge.common.world.NichirinPlacedFeatures;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = BreathOfNichirin.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NichirinDataGen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(event.includeServer(), new DatapackBuiltinEntriesProvider(
                output,
                lookupProvider,
                BUILDER,
                Set.of(BreathOfNichirin.MOD_ID)
        ));
    }

    private static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, NichirinConfiguredFeatures::bootstrap)
            .add(Registries.PLACED_FEATURE, NichirinPlacedFeatures::bootstrap)
            .add(ForgeRegistries.Keys.BIOME_MODIFIERS, NichirinBiomeModifications::bootstrap);
}