package com.xirc.nichirin.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.xirc.nichirin.BreathOfNichirin;

@Mod(BreathOfNichirin.MOD_ID)
public final class BreathOfNichirinForge {
    public BreathOfNichirinForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(BreathOfNichirin.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        BreathOfNichirin.init();
    }
}
