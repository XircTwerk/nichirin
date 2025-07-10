package com.xirc.nichirin.fabric;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.fabric.common.terrablender.NichirinTerraFabric;
import com.xirc.nichirin.fabric.network.FabricPacketHandler;
import net.fabricmc.api.ModInitializer;

public final class BreathOfNichirinFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BreathOfNichirin.LOGGER.info("Initializing Nichirin for Fabric");
        BreathOfNichirin.init();
        FabricPacketHandler.registerServerPackets();
        NichirinTerraFabric.onModInitialized();
    }
}