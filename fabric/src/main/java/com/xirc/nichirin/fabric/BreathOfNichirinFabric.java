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
        // Add a debug message here to confirm it's being called
        System.out.println("[Nichirin] About to call NichirinTerraFabric.onModInitialized()");
        NichirinTerraFabric.onModInitialized();
    }
}