package com.xirc.nichirin;

import com.xirc.nichirin.client.util.ItemPropertiesHelper;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BreathOfNichirin.MOD_ID)
public final class BreathOfNichirinForge {
    public BreathOfNichirinForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(BreathOfNichirin.MOD_ID, modEventBus);

        BreathOfNichirin.init();
    }
}