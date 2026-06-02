package com.xirc.nichirin.neoforge;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.registry.NichirinEntityRegistry;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

@Mod(BreathOfNichirin.MOD_ID)
public final class BreathOfNichirinNeoForge {
    public BreathOfNichirinNeoForge(IEventBus modEventBus) {
        BreathOfNichirin.init();
        modEventBus.addListener(BreathOfNichirinNeoForge::registerSpawnPlacements);
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(NichirinEntityRegistry.TEMPLE_DEMON.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}