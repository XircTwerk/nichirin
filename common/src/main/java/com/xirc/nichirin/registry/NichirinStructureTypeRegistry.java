package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.worldgen.structure.UrokodakiHouseStructure;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface NichirinStructureTypeRegistry {
    Logger LOGGER = LoggerFactory.getLogger("NichirinStructureTypeRegistry");

    DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.STRUCTURE_TYPE);

    RegistrySupplier<StructureType<UrokodakiHouseStructure>> UROKODAKI_HOUSE =
            STRUCTURE_TYPES.register("urokodaki_house",
                    () -> () -> UrokodakiHouseStructure.CODEC);

    static void init() {
        STRUCTURE_TYPES.register();
    }
}