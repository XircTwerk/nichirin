package com.xirc.nichirin;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BreathOfNichirin.MOD_ID)
public final class BreathOfNichirinForge {
    public BreathOfNichirinForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(BreathOfNichirin.MOD_ID, modEventBus);

        // Run our common setup
        BreathOfNichirin.init();
    }
}