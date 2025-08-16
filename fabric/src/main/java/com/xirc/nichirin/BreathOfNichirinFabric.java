package com.xirc.nichirin;

import com.xirc.nichirin.client.util.fabric.ItemPropertiesHelperImpl;
import com.xirc.nichirin.common.terrablender.NichirinTerraFabric;
import net.fabricmc.api.ModInitializer;

public final class BreathOfNichirinFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BreathOfNichirin.LOGGER.info("Initializing Nichirin for Fabric");
        BreathOfNichirin.init();
        NichirinTerraFabric.onModInitialized();
        ItemPropertiesHelperImpl.registerBentoBoxProperty();
    }
}