package com.xirc.nichirin.registry;

import com.xirc.nichirin.common.blocks.*;
import com.xirc.nichirin.common.blocks.sign.*;
import com.xirc.nichirin.common.worldgen.trees.wisteria.WisteriaSaplingBlock;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;

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

    RegistrySupplier<Block> WISTERIA_LOG = BLOCKS.register("wisteria_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> STRIPPED_WISTERIA_LOG = BLOCKS.register("stripped_wisteria_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WISTERIA_WOOD = BLOCKS.register("wisteria_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_WOOD).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> STRIPPED_WISTERIA_WOOD = BLOCKS.register("stripped_wisteria_wood",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_WOOD).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WISTERIA_PLANKS = BLOCKS.register("wisteria_planks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WISTERIA_LEAVES = BLOCKS.register("wisteria_leaves",
            () -> new WisteriaLeavesBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LEAVES)
                    .mapColor(MapColor.COLOR_PURPLE)
                    .lightLevel(state -> 8)));

    RegistrySupplier<Block> WISTERIA_GLOW_LICHEN = BLOCKS.register("wisteria_glow_lichen",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SHROOMLIGHT)
                    .mapColor(MapColor.COLOR_PURPLE)
                    .lightLevel(state -> 10)));

    RegistrySupplier<Block> WISTERIA_GLOW_BERRIES = BLOCKS.register("wisteria_glow_berries",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.SHROOMLIGHT)
                    .mapColor(MapColor.COLOR_PURPLE)
                    .lightLevel(state -> 12)));

    RegistrySupplier<Block> WISTERIA_LANTERN = BLOCKS.register("wisteria_lantern",
            () -> new LanternBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
                    .mapColor(MapColor.COLOR_PURPLE)
                    .lightLevel(state -> 15)));

    RegistrySupplier<Block> WISTERIA_STAIRS = BLOCKS.register("wisteria_stairs",
            () -> new StairBlock(WISTERIA_PLANKS.get().defaultBlockState(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_STAIRS).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WISTERIA_SLAB = BLOCKS.register("wisteria_slab",
            () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SLAB).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WISTERIA_FENCE = BLOCKS.register("wisteria_fence",
            () -> new FenceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WISTERIA_FENCE_GATE = BLOCKS.register("wisteria_fence_gate",
            () -> new FenceGateBlock(NichirinWoodTypes.WISTERIA,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_FENCE_GATE).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WISTERIA_DOOR = BLOCKS.register("wisteria_door",
            () -> new DoorBlock(NichirinBlockSetTypes.WISTERIA,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_DOOR).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WISTERIA_TRAPDOOR = BLOCKS.register("wisteria_trapdoor",
            () -> new TrapDoorBlock(NichirinBlockSetTypes.WISTERIA,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_TRAPDOOR).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WISTERIA_PRESSURE_PLATE = BLOCKS.register("wisteria_pressure_plate",
            () -> new PressurePlateBlock(NichirinBlockSetTypes.WISTERIA,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WISTERIA_BUTTON = BLOCKS.register("wisteria_button",
            () -> new ButtonBlock(NichirinBlockSetTypes.WISTERIA, 30,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON).mapColor(MapColor.COLOR_PURPLE)));

    RegistrySupplier<Block> WISTERIA_SAPLING = BLOCKS.register("wisteria_sapling",
            () -> new WisteriaSaplingBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)));

    // Fresh sign properties (mirrors vanilla oak signs). Deliberately NOT ofFullCopy(OAK_SIGN):
    // that copies oak's loot-table reference, so the block would ignore its own loot table and drop
    // an oak sign. With no copied drops, each block derives its own "nichirin:blocks/<id>" table.
    static BlockBehaviour.Properties signProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_PURPLE)
                .forceSolidOn()
                .instrument(NoteBlockInstrument.BASS)
                .noCollission()
                .strength(1.0F)
                .sound(SoundType.WOOD)
                .ignitedByLava();
    }

    RegistrySupplier<Block> WISTERIA_SIGN = BLOCKS.register("wisteria_sign",
            () -> new WisteriaStandingSignBlock(NichirinWoodTypes.WISTERIA, signProperties()));

    RegistrySupplier<Block> WISTERIA_WALL_SIGN = BLOCKS.register("wisteria_wall_sign",
            () -> new WisteriaWallSignBlock(NichirinWoodTypes.WISTERIA, signProperties()));

    RegistrySupplier<Block> WISTERIA_HANGING_SIGN = BLOCKS.register("wisteria_hanging_sign",
            () -> new WisteriaCeilingHangingSignBlock(NichirinWoodTypes.WISTERIA, signProperties()));

    RegistrySupplier<Block> WISTERIA_WALL_HANGING_SIGN = BLOCKS.register("wisteria_wall_hanging_sign",
            () -> new WisteriaWallHangingSignBlock(NichirinWoodTypes.WISTERIA, signProperties()));

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

    RegistrySupplier<Item> WISTERIA_LOG_ITEM = ITEMS.register("wisteria_log",
            () -> new BlockItem(WISTERIA_LOG.get(), new Item.Properties()));

    RegistrySupplier<Item> STRIPPED_WISTERIA_LOG_ITEM = ITEMS.register("stripped_wisteria_log",
            () -> new BlockItem(STRIPPED_WISTERIA_LOG.get(), new Item.Properties()));

    RegistrySupplier<Item> WISTERIA_WOOD_ITEM = ITEMS.register("wisteria_wood",
            () -> new BlockItem(WISTERIA_WOOD.get(), new Item.Properties()));

    RegistrySupplier<Item> STRIPPED_WISTERIA_WOOD_ITEM = ITEMS.register("stripped_wisteria_wood",
            () -> new BlockItem(STRIPPED_WISTERIA_WOOD.get(), new Item.Properties()));

    RegistrySupplier<Item> WISTERIA_PLANKS_ITEM = ITEMS.register("wisteria_planks",
            () -> new BlockItem(WISTERIA_PLANKS.get(), new Item.Properties()));

    RegistrySupplier<Item> WISTERIA_LEAVES_ITEM = ITEMS.register("wisteria_leaves",
            () -> new BlockItem(WISTERIA_LEAVES.get(), new Item.Properties()));

    RegistrySupplier<Item> WISTERIA_GLOW_LICHEN_ITEM = ITEMS.register("wisteria_glow_lichen",
            () -> new BlockItem(WISTERIA_GLOW_LICHEN.get(), new Item.Properties()));

    RegistrySupplier<Item> WISTERIA_GLOW_BERRIES_ITEM = ITEMS.register("wisteria_glow_berries",
            () -> new BlockItem(WISTERIA_GLOW_BERRIES.get(), new Item.Properties()));

    RegistrySupplier<Item> WISTERIA_LANTERN_ITEM = ITEMS.register("wisteria_lantern",
            () -> new BlockItem(WISTERIA_LANTERN.get(), new Item.Properties()));

    RegistrySupplier<Item> WISTERIA_STAIRS_ITEM = ITEMS.register("wisteria_stairs",
            () -> new BlockItem(WISTERIA_STAIRS.get(), new Item.Properties()));

    RegistrySupplier<Item> WISTERIA_SLAB_ITEM = ITEMS.register("wisteria_slab",
            () -> new BlockItem(WISTERIA_SLAB.get(), new Item.Properties()));

    RegistrySupplier<Item> WISTERIA_FENCE_ITEM = ITEMS.register("wisteria_fence",
            () -> new BlockItem(WISTERIA_FENCE.get(), new Item.Properties()));

    RegistrySupplier<Item> WISTERIA_FENCE_GATE_ITEM = ITEMS.register("wisteria_fence_gate",
            () -> new BlockItem(WISTERIA_FENCE_GATE.get(), new Item.Properties()));

    RegistrySupplier<Item> WISTERIA_DOOR_ITEM = ITEMS.register("wisteria_door",
            () -> new BlockItem(WISTERIA_DOOR.get(), new Item.Properties()));

    RegistrySupplier<Item> WISTERIA_TRAPDOOR_ITEM = ITEMS.register("wisteria_trapdoor",
            () -> new BlockItem(WISTERIA_TRAPDOOR.get(), new Item.Properties()));

    RegistrySupplier<Item> WISTERIA_SIGN_ITEM = ITEMS.register("wisteria_sign",
            () -> new SignItem(new Item.Properties().stacksTo(16),
                    WISTERIA_SIGN.get(), WISTERIA_WALL_SIGN.get()));

    RegistrySupplier<Item> WISTERIA_HANGING_SIGN_ITEM = ITEMS.register("wisteria_hanging_sign",
            () -> new HangingSignItem(WISTERIA_HANGING_SIGN.get(), WISTERIA_WALL_HANGING_SIGN.get(),
                    new Item.Properties().stacksTo(16)));

    RegistrySupplier<Item> WISTERIA_PRESSURE_PLATE_ITEM = ITEMS.register("wisteria_pressure_plate",
            () -> new BlockItem(WISTERIA_PRESSURE_PLATE.get(), new Item.Properties()));

    RegistrySupplier<Item> WISTERIA_BUTTON_ITEM = ITEMS.register("wisteria_button",
            () -> new BlockItem(WISTERIA_BUTTON.get(), new Item.Properties()));

    RegistrySupplier<Item> WISTERIA_SAPLING_ITEM = ITEMS.register("wisteria_sapling",
            () -> new BlockItem(WISTERIA_SAPLING.get(), new Item.Properties()));

    static void register() {
        BLOCKS.register();
        ITEMS.register();
    }
}
