package com.xirc.nichirin;

import com.xirc.nichirin.common.terrablender.NichirinTerraFabric;
import com.xirc.nichirin.common.world.NichirinBiomeModifications;
import net.fabricmc.api.ModInitializer;

public final class BreathOfNichirinFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BreathOfNichirin.init();
        NichirinTerraFabric.onModInitialized();
        NichirinBiomeModifications.addSpawns();
    }
}