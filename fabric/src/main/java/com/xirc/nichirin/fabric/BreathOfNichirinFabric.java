package com.xirc.nichirin.fabric;

import net.fabricmc.api.ModInitializer;

import com.xirc.nichirin.BreathOfNichirin;

public final class BreathOfNichirinFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BreathOfNichirin.LOGGER.info("Initializing Nichirin for Fabric");
        BreathOfNichirin.init();
    }
}
