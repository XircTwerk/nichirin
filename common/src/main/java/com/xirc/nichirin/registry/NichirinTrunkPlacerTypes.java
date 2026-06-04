package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.worldgen.trees.wisteria.WisteriaTrunkPlacer;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacerType;

public interface NichirinTrunkPlacerTypes {
    DeferredRegister<TrunkPlacerType<?>> TRUNK_PLACERS = DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.TRUNK_PLACER_TYPE);

    RegistrySupplier<TrunkPlacerType<WisteriaTrunkPlacer>> WISTERIA_TRUNK_PLACER = TRUNK_PLACERS.register("wisteria_trunk_placer",
            () -> new TrunkPlacerType<>(WisteriaTrunkPlacer.CODEC));

    static void register() {
        TRUNK_PLACERS.register();
    }
}