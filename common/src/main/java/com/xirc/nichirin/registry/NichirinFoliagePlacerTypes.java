package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.worldgen.trees.wysteria.*;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

public interface NichirinFoliagePlacerTypes {
    DeferredRegister<FoliagePlacerType<?>> FOLIAGE_PLACERS = DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.FOLIAGE_PLACER_TYPE);

    RegistrySupplier<FoliagePlacerType<WysteriaSmallFoliagePlacer>> WYSTERIA_SMALL_FOLIAGE_PLACER = FOLIAGE_PLACERS.register("wysteria_small_foliage_placer",
            () -> new FoliagePlacerType<>(WysteriaSmallFoliagePlacer.CODEC));

    RegistrySupplier<FoliagePlacerType<WysteriaMediumFoliagePlacer>> WYSTERIA_MEDIUM_FOLIAGE_PLACER = FOLIAGE_PLACERS.register("wysteria_medium_foliage_placer",
            () -> new FoliagePlacerType<>(WysteriaMediumFoliagePlacer.CODEC));

    RegistrySupplier<FoliagePlacerType<WysteriaLargeFoliagePlacer>> WYSTERIA_LARGE_FOLIAGE_PLACER = FOLIAGE_PLACERS.register("wysteria_large_foliage_placer",
            () -> new FoliagePlacerType<>(WysteriaLargeFoliagePlacer.CODEC));

    static void register() {
        FOLIAGE_PLACERS.register();
    }
}