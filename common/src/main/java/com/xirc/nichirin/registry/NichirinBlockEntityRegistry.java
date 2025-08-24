package com.xirc.nichirin.registry;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.blocks.BentoBoxBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public interface NichirinBlockEntityRegistry {
    DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    RegistrySupplier<BlockEntityType<BentoBoxBlock.BentoBoxBlockEntity>> BENTO_BOX_BLOCK_ENTITY = BLOCK_ENTITIES.register("bento_box_block_entity",
            () -> BlockEntityType.Builder.of(BentoBoxBlock.BentoBoxBlockEntity::new, NichirinBlockRegistry.BENTO_BOX_BLOCK.get()).build(null));

    static void register() {
        BLOCK_ENTITIES.register();
    }
}