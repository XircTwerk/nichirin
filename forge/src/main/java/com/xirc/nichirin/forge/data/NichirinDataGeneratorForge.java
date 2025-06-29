package com.xirc.nichirin.forge.data;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.data.NichirinData;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = BreathOfNichirin.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class NichirinDataGeneratorForge {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        // Add the datapack builtin entries provider
        generator.addProvider(
                event.includeServer(),
                new DatapackBuiltinEntriesProvider(
                        output,
                        lookupProvider,
                        NichirinData.BUILDER,
                        Set.of(BreathOfNichirin.MOD_ID)
                )
        );
    }
}