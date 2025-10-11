package com.xirc.nichirin.datagen.providers.assets;

import com.xirc.nichirin.registry.NichirinBlockRegistry;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TexturedModel;

public class NichirinModelProvider extends FabricModelProvider {

    public NichirinModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generator) {
        // Your existing blocks - unchanged
        generator.createTrivialCube(NichirinBlockRegistry.SCARLET_ORE.get());
        generator.createTrivialCube(NichirinBlockRegistry.SCARLET_CRIMSON_IRON_SAND.get());
        generator.createTrivialCube(NichirinBlockRegistry.KATANA_HOLDER_BLOCK.get());

        // NEW: Wysteria Wood Set - using vanilla wood provider system
        createWysteriaWoodSet(generator);
        createMiscellaneousBlocks(generator);
    }

    private void createMiscellaneousBlocks(BlockModelGenerators generator){
        generator.woodProvider(NichirinBlockRegistry.TATAMI_BLOCK.get())
                .logWithHorizontal(NichirinBlockRegistry.TATAMI_BLOCK.get());
    }

    private void createWysteriaWoodSet(BlockModelGenerators generator) {
        // Use the wood provider system like vanilla does
        generator.woodProvider(NichirinBlockRegistry.WYSTERIA_LOG.get())
                .logWithHorizontal(NichirinBlockRegistry.WYSTERIA_LOG.get())
                .wood(NichirinBlockRegistry.WYSTERIA_WOOD.get());

        generator.woodProvider(NichirinBlockRegistry.STRIPPED_WYSTERIA_LOG.get())
                .logWithHorizontal(NichirinBlockRegistry.STRIPPED_WYSTERIA_LOG.get())
                .wood(NichirinBlockRegistry.STRIPPED_WYSTERIA_WOOD.get());

        // Simple blocks
        generator.createTrivialBlock(NichirinBlockRegistry.WYSTERIA_LEAVES.get(), TexturedModel.LEAVES);
        generator.createTrivialBlock(NichirinBlockRegistry.INFINITY_GLASS1.get(), TexturedModel.CUBE);
        generator.createTrivialBlock(NichirinBlockRegistry.INFINITY_GLASS2.get(), TexturedModel.CUBE);

        // Use BlockFamily system for consistent generation
        generator.family(NichirinBlockRegistry.WYSTERIA_PLANKS.get())
                .stairs(NichirinBlockRegistry.WYSTERIA_STAIRS.get())
                .slab(NichirinBlockRegistry.WYSTERIA_SLAB.get())
                .fence(NichirinBlockRegistry.WYSTERIA_FENCE.get())
                .fenceGate(NichirinBlockRegistry.WYSTERIA_FENCE_GATE.get())
                .pressurePlate(NichirinBlockRegistry.WYSTERIA_PRESSURE_PLATE.get())
                .button(NichirinBlockRegistry.WYSTERIA_BUTTON.get());


        // Door and trapdoor - these use their own textures
        generator.createDoor(NichirinBlockRegistry.WYSTERIA_DOOR.get());
        generator.createTrapdoor(NichirinBlockRegistry.WYSTERIA_TRAPDOOR.get());
    }

    @Override
    public void generateItemModels(ItemModelGenerators generator) {
        generator.generateFlatItem(NichirinItemRegistry.SCARLET_CRIMSON_IRON_GEM.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.SCARLET_GEM.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.ONIGIRI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.SAKURAMOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.BLACK_MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.BLUE_MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.BROWN_MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.MAGENTA_MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.CYAN_MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.GRAY_MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.GREEN_MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.LIGHT_BLUE_MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.LIGHT_GRAY_MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.LIME_MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.ORANGE_MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.PINK_MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.PURPLE_MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.RED_MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.WHITE_MOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.YELLOW_MOCHI.get(), ModelTemplates.FLAT_ITEM);

        generator.generateFlatItem(NichirinItemRegistry.SMOKE_BOMB.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.FLASH_BOMB.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.BENTO_BOX.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinBlockRegistry.WYSTERIA_SAPLING_ITEM.get(), ModelTemplates.FLAT_ITEM);

        generator.generateFlatItem(NichirinItemRegistry.KATANA.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.THUNDER_KATANA.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.FLAME_KATANA.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.INSECT_KATANA.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.SOUND_KATANAS.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.RIGHT_SOUND_KATANA.get(), ModelTemplates.FLAT_HANDHELD_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.LEFT_SOUND_KATANA.get(), ModelTemplates.FLAT_HANDHELD_ITEM);

        // Armor items
        generateArmorItems(generator);
    }

    private void generateArmorItems(ItemModelGenerators generator) {
        // Zenitsu armor
        generator.generateFlatItem(NichirinItemRegistry.ZENITSU_HEADPIECE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.ZENITSU_CAPE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.ZENITSU_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.ZENITSU_BOOTS.get(), ModelTemplates.FLAT_ITEM);

        // Rengoku armor
        generator.generateFlatItem(NichirinItemRegistry.RENGOKU_HEADPIECE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.RENGOKU_CAPE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.RENGOKU_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.RENGOKU_BOOTS.get(), ModelTemplates.FLAT_ITEM);

        // Tengen armor
        generator.generateFlatItem(NichirinItemRegistry.TENGEN_HEADPIECE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.TENGEN_ACCESSORIES.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.TENGEN_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.TENGEN_BOOTS.get(), ModelTemplates.FLAT_ITEM);

        // Shinobu armor
        generator.generateFlatItem(NichirinItemRegistry.SHINOBU_HEADPIECE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.SHINOBU_CAPE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.SHINOBU_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.SHINOBU_BOOTS.get(), ModelTemplates.FLAT_ITEM);
    }
}