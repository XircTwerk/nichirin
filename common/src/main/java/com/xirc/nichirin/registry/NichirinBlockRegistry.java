package com.xirc.nichirin.registry;

import com.xirc.nichirin.common.blocks.*;
import com.xirc.nichirin.common.worldgen.trees.wysteria.WysteriaSaplingBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public interface NichirinBlockRegistry {
    DeferredRegister<Block> BLOCKS = DeferredRegister.create("nichirin", Registries.BLOCK);
    DeferredRegister<Item> ITEMS = DeferredRegister.create("nichirin", Registries.ITEM);

    // Block Registration
    RegistrySupplier<Block> SCARLET_CRIMSON_IRON_SAND = BLOCKS.register("scarlet_crimson_iron_sand",
            ScarletCrimsonIronSandBlock::new);

    RegistrySupplier<Block> SCARLET_ORE = BLOCKS.register("scarlet_ore",
            ScarletOreBlock::new);

    RegistrySupplier<Block> BENTO_BOX_BLOCK = BLOCKS.register("bento_box_block",
            () -> new BentoBoxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    RegistrySupplier<Block> KATANA_HOLDER_BLOCK = BLOCKS.register("katana_holder_block",
            () -> new KatanaHolderBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()));

    RegistrySupplier<Block> WYSTERIA_LOG = BLOCKS.register("wysteria_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> STRIPPED_WYSTERIA_LOG = BLOCKS.register("stripped_wysteria_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WYSTERIA_WOOD = BLOCKS.register("wysteria_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> STRIPPED_WYSTERIA_WOOD = BLOCKS.register("stripped_wysteria_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WYSTERIA_PLANKS = BLOCKS.register("wysteria_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WYSTERIA_LEAVES = BLOCKS.register("wysteria_leaves",
            () -> new WysteriaLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WYSTERIA_STAIRS = BLOCKS.register("wysteria_stairs",
            () -> new StairBlock(WYSTERIA_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WYSTERIA_SLAB = BLOCKS.register("wysteria_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WYSTERIA_FENCE = BLOCKS.register("wysteria_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WYSTERIA_FENCE_GATE = BLOCKS.register("wysteria_fence_gate",
            () -> new FenceGateBlock(NichirinWoodTypes.WYSTERIA,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WYSTERIA_DOOR = BLOCKS.register("wysteria_door",
            () -> new DoorBlock(NichirinBlockSetTypes.WYSTERIA,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WYSTERIA_TRAPDOOR = BLOCKS.register("wysteria_trapdoor",
            () -> new TrapDoorBlock(NichirinBlockSetTypes.WYSTERIA,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WYSTERIA_PRESSURE_PLATE = BLOCKS.register("wysteria_pressure_plate",
            () -> new PressurePlateBlock(NichirinBlockSetTypes.WYSTERIA,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WYSTERIA_BUTTON = BLOCKS.register("wysteria_button",
            () -> new ButtonBlock(NichirinBlockSetTypes.WYSTERIA, 30,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WYSTERIA_SAPLING = BLOCKS.register("wysteria_sapling",
            () -> new WysteriaSaplingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));

    // Tatami shi by Nacho
    RegistrySupplier<Block> TATAMI_BLOCK = BLOCKS.register("tatami_block",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG)));

    RegistrySupplier<Item> TATAMI_BLOCK_ITEM = ITEMS.register("tatami_block",
            () -> new BlockItem(TATAMI_BLOCK.get(), new Item.Properties()));

    RegistrySupplier<Block> INFINITY_GLASS1 = BLOCKS.register("infinity_glass1",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)));

    RegistrySupplier<Block> INFINITY_GLASS2 = BLOCKS.register("infinity_glass2",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)));
    RegistrySupplier<Item> INFINITY_GLASS1_ITEM = ITEMS.register("infinity_glass1",
            () -> new BlockItem(INFINITY_GLASS1.get(), new Item.Properties()));
    RegistrySupplier<Item> INFINITY_GLASS2_ITEM = ITEMS.register("infinity_glass2",
            () -> new BlockItem(INFINITY_GLASS2.get(), new Item.Properties()));



    // Block Item Registration
    RegistrySupplier<Item> SCARLET_CRIMSON_IRON_SAND_ITEM = ITEMS.register("scarlet_crimson_iron_sand",
            () -> new BlockItem(SCARLET_CRIMSON_IRON_SAND.get(), new Item.Properties()));

    RegistrySupplier<Item> SCARLET_ORE_ITEM = ITEMS.register("scarlet_ore",
            () -> new BlockItem(SCARLET_ORE.get(), new Item.Properties()));

    RegistrySupplier<Item> KATANA_HOLDER_ITEM = ITEMS.register("katana_holder",
            () -> new BlockItem(KATANA_HOLDER_BLOCK.get(), new Item.Properties()));

    RegistrySupplier<Item> WYSTERIA_LOG_ITEM = ITEMS.register("wysteria_log",
            () -> new BlockItem(WYSTERIA_LOG.get(), new Item.Properties()));

    RegistrySupplier<Item> STRIPPED_WYSTERIA_LOG_ITEM = ITEMS.register("stripped_wysteria_log",
            () -> new BlockItem(STRIPPED_WYSTERIA_LOG.get(), new Item.Properties()));

    RegistrySupplier<Item> WYSTERIA_WOOD_ITEM = ITEMS.register("wysteria_wood",
            () -> new BlockItem(WYSTERIA_WOOD.get(), new Item.Properties()));

    RegistrySupplier<Item> STRIPPED_WYSTERIA_WOOD_ITEM = ITEMS.register("stripped_wysteria_wood",
            () -> new BlockItem(STRIPPED_WYSTERIA_WOOD.get(), new Item.Properties()));

    RegistrySupplier<Item> WYSTERIA_PLANKS_ITEM = ITEMS.register("wysteria_planks",
            () -> new BlockItem(WYSTERIA_PLANKS.get(), new Item.Properties()));

    RegistrySupplier<Item> WYSTERIA_LEAVES_ITEM = ITEMS.register("wysteria_leaves",
            () -> new BlockItem(WYSTERIA_LEAVES.get(), new Item.Properties()));

    RegistrySupplier<Item> WYSTERIA_STAIRS_ITEM = ITEMS.register("wysteria_stairs",
            () -> new BlockItem(WYSTERIA_STAIRS.get(), new Item.Properties()));

    RegistrySupplier<Item> WYSTERIA_SLAB_ITEM = ITEMS.register("wysteria_slab",
            () -> new BlockItem(WYSTERIA_SLAB.get(), new Item.Properties()));

    RegistrySupplier<Item> WYSTERIA_FENCE_ITEM = ITEMS.register("wysteria_fence",
            () -> new BlockItem(WYSTERIA_FENCE.get(), new Item.Properties()));

    RegistrySupplier<Item> WYSTERIA_FENCE_GATE_ITEM = ITEMS.register("wysteria_fence_gate",
            () -> new BlockItem(WYSTERIA_FENCE_GATE.get(), new Item.Properties()));

    RegistrySupplier<Item> WYSTERIA_DOOR_ITEM = ITEMS.register("wysteria_door",
            () -> new BlockItem(WYSTERIA_DOOR.get(), new Item.Properties()));

    RegistrySupplier<Item> WYSTERIA_TRAPDOOR_ITEM = ITEMS.register("wysteria_trapdoor",
            () -> new BlockItem(WYSTERIA_TRAPDOOR.get(), new Item.Properties()));

    RegistrySupplier<Item> WYSTERIA_PRESSURE_PLATE_ITEM = ITEMS.register("wysteria_pressure_plate",
            () -> new BlockItem(WYSTERIA_PRESSURE_PLATE.get(), new Item.Properties()));

    RegistrySupplier<Item> WYSTERIA_BUTTON_ITEM = ITEMS.register("wysteria_button",
            () -> new BlockItem(WYSTERIA_BUTTON.get(), new Item.Properties()));

    RegistrySupplier<Item> WYSTERIA_SAPLING_ITEM = ITEMS.register("wysteria_sapling",
            () -> new BlockItem(WYSTERIA_SAPLING.get(), new Item.Properties()));

    static void register() {
        BLOCKS.register();
        ITEMS.register();
    }
}