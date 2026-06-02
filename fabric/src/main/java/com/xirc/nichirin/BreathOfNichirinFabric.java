package com.xirc.nichirin;

import com.xirc.nichirin.common.terrablender.NichirinTerraFabric;
import com.xirc.nichirin.common.world.NichirinBiomeModifications;
import com.xirc.nichirin.network.FabricPacketHandler;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import net.fabricmc.api.ModInitializer;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

public final class BreathOfNichirinFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BreathOfNichirin.init();
        SpawnPlacements.register(NichirinEntityRegistry.TEMPLE_DEMON.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules);
        FabricPacketHandler.registerServerPackets();
        NichirinTerraFabric.onModInitialized();
        NichirinBiomeModifications.addSpawns();
    }
}