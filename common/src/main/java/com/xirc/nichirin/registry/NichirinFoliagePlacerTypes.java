package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.worldgen.trees.wisteria.*;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

public interface NichirinFoliagePlacerTypes {
    DeferredRegister<FoliagePlacerType<?>> FOLIAGE_PLACERS = DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.FOLIAGE_PLACER_TYPE);

    RegistrySupplier<FoliagePlacerType<WisteriaSmallFoliagePlacer>> WISTERIA_SMALL_FOLIAGE_PLACER = FOLIAGE_PLACERS.register("wisteria_small_foliage_placer",
            () -> new FoliagePlacerType<>(WisteriaSmallFoliagePlacer.CODEC));

    RegistrySupplier<FoliagePlacerType<WisteriaMediumFoliagePlacer>> WISTERIA_MEDIUM_FOLIAGE_PLACER = FOLIAGE_PLACERS.register("wisteria_medium_foliage_placer",
            () -> new FoliagePlacerType<>(WisteriaMediumFoliagePlacer.CODEC));

    RegistrySupplier<FoliagePlacerType<WisteriaLargeFoliagePlacer>> WISTERIA_LARGE_FOLIAGE_PLACER = FOLIAGE_PLACERS.register("wisteria_large_foliage_placer",
            () -> new FoliagePlacerType<>(WisteriaLargeFoliagePlacer.CODEC));

    static void register() {
        FOLIAGE_PLACERS.register();
    }
}