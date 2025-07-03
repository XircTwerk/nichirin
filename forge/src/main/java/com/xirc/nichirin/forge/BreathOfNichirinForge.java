package com.xirc.nichirin.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.xirc.nichirin.BreathOfNichirin;

@Mod(BreathOfNichirin.MOD_ID)
public final class BreathOfNichirinForge {
    public BreathOfNichirinForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(BreathOfNichirin.MOD_ID, modEventBus);

        // Run our common setup
        BreathOfNichirin.init();
    }
}