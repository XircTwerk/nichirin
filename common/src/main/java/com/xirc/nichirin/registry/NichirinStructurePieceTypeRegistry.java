package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.worldgen.structure.pieces.UrokodakiHousePiece;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface NichirinStructurePieceTypeRegistry {
    Logger LOGGER = LoggerFactory.getLogger("NichirinStructurePieceTypeRegistry");

    DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.STRUCTURE_PIECE);

    RegistrySupplier<StructurePieceType> UROKODAKI_HOUSE_PIECE =
            STRUCTURE_PIECE_TYPES.register("urokodaki_house_piece",
                    () -> UrokodakiHousePiece::new);

    static void init() {
        STRUCTURE_PIECE_TYPES.register();
    }
}