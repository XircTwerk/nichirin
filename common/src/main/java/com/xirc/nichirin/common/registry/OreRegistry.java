package com.xirc.nichirin.common.registry;

import com.xirc.nichirin.common.blocks.ScarletCrimsonIronSandBlock;
import com.xirc.nichirin.common.blocks.ScarletOreBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class OreRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create("nichirin", Registries.BLOCK);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create("nichirin", Registries.ITEM);

    // Block Registration
    public static final RegistrySupplier<Block> SCARLET_CRIMSON_IRON_SAND = BLOCKS.register("scarlet_crimson_iron_sand.json",
            ScarletCrimsonIronSandBlock::new);

    public static final RegistrySupplier<Block> SCARLET_ORE = BLOCKS.register("scarlet_ore",
            ScarletOreBlock::new);

    // Block Item Registration
    public static final RegistrySupplier<Item> SCARLET_CRIMSON_IRON_SAND_ITEM = ITEMS.register("scarlet_crimson_iron_sand.json",
            () -> new BlockItem(SCARLET_CRIMSON_IRON_SAND.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> SCARLET_ORE_ITEM = ITEMS.register("scarlet_ore",
            () -> new BlockItem(SCARLET_ORE.get(), new Item.Properties()));

    public static void register() {
        BLOCKS.register();
        ITEMS.register();
    }
}