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
        generator.createTrivialCube(NichirinBlockRegistry.SCARLET_ORE.get());
        generator.createTrivialCube(NichirinBlockRegistry.SCARLET_CRIMSON_IRON_SAND.get());
        generator.createTrivialCube(NichirinBlockRegistry.KATANA_HOLDER_BLOCK.get());

        createWisteriaWoodSet(generator);
        createMiscellaneousBlocks(generator);
    }

    private void createMiscellaneousBlocks(BlockModelGenerators generator){
        generator.woodProvider(NichirinBlockRegistry.TATAMI_BLOCK.get())
                .logWithHorizontal(NichirinBlockRegistry.TATAMI_BLOCK.get());
    }

    private void createWisteriaWoodSet(BlockModelGenerators generator) {
        // Use the wood provider system like vanilla does
        generator.woodProvider(NichirinBlockRegistry.WISTERIA_LOG.get())
                .logWithHorizontal(NichirinBlockRegistry.WISTERIA_LOG.get())
                .wood(NichirinBlockRegistry.WISTERIA_WOOD.get());

        generator.woodProvider(NichirinBlockRegistry.STRIPPED_WISTERIA_LOG.get())
                .logWithHorizontal(NichirinBlockRegistry.STRIPPED_WISTERIA_LOG.get())
                .wood(NichirinBlockRegistry.STRIPPED_WISTERIA_WOOD.get());

        // Simple blocks
        generator.createTrivialBlock(NichirinBlockRegistry.WISTERIA_LEAVES.get(), TexturedModel.LEAVES);
        generator.createTrivialBlock(NichirinBlockRegistry.WISTERIA_GLOW_LICHEN.get(), TexturedModel.CUBE);
        generator.createTrivialBlock(NichirinBlockRegistry.WISTERIA_GLOW_BERRIES.get(), TexturedModel.CUBE);
        generator.createTrivialBlock(NichirinBlockRegistry.INFINITY_GLASS1.get(), TexturedModel.CUBE);
        generator.createTrivialBlock(NichirinBlockRegistry.INFINITY_GLASS2.get(), TexturedModel.CUBE);

        // Use BlockFamily system for consistent generation
        generator.family(NichirinBlockRegistry.WISTERIA_PLANKS.get())
                .stairs(NichirinBlockRegistry.WISTERIA_STAIRS.get())
                .slab(NichirinBlockRegistry.WISTERIA_SLAB.get())
                .fence(NichirinBlockRegistry.WISTERIA_FENCE.get())
                .fenceGate(NichirinBlockRegistry.WISTERIA_FENCE_GATE.get())
                .pressurePlate(NichirinBlockRegistry.WISTERIA_PRESSURE_PLATE.get())
                .button(NichirinBlockRegistry.WISTERIA_BUTTON.get());


        // Door and trapdoor - these use their own textures
        generator.createDoor(NichirinBlockRegistry.WISTERIA_DOOR.get());
        generator.createTrapdoor(NichirinBlockRegistry.WISTERIA_TRAPDOOR.get());
    }

    @Override
    public void generateItemModels(ItemModelGenerators generator) {
        generator.generateFlatItem(NichirinItemRegistry.SCARLET_CRIMSON_IRON_GEM.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.SCARLET_GEM.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.ONIGIRI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.SAKURAMOCHI.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.WISTERIA_FLOWER.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.WISTERIA_EXTRACT.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.WISTERIA_TEA.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.WISTERIA_ARROW.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinBlockRegistry.WISTERIA_LANTERN_ITEM.get(), ModelTemplates.FLAT_ITEM);
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
        generator.generateFlatItem(NichirinItemRegistry.DRINKING_GOURD.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.RICE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.PERK_SCROLL.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.CURSED_SCROLL.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinBlockRegistry.WISTERIA_SAPLING_ITEM.get(), ModelTemplates.FLAT_ITEM);

        generator.generateFlatItem(NichirinItemRegistry.BOAR_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.TEMPLE_DEMON_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.WATER_BREATHING_TRAINER_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.THUNDER_BREATHING_TRAINER_SPAWN_EGG.get(), ModelTemplates.FLAT_ITEM);

        // Armor items
        generateArmorItems(generator);
    }

    private void generateArmorItems(ItemModelGenerators generator) {
        // Zenitsu armor
        generator.generateFlatItem(NichirinItemRegistry.ZENITSU_HEADPIECE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.ZENITSU_CAPE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.ZENITSU_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.ZENITSU_BOOTS.get(), ModelTemplates.FLAT_ITEM);

        generator.generateFlatItem(NichirinItemRegistry.JIGORO_HEADPIECE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.JIGORO_CAPE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.JIGORO_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.JIGORO_BOOTS.get(), ModelTemplates.FLAT_ITEM);

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

        // Sabito armor
        generator.generateFlatItem(NichirinItemRegistry.SABITO_HEADPIECE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.SABITO_CAPE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.SABITO_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.SABITO_BOOTS.get(), ModelTemplates.FLAT_ITEM);

        // Giyu armor
        generator.generateFlatItem(NichirinItemRegistry.GIYU_HEADPIECE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.GIYU_CAPE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.GIYU_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.GIYU_BOOTS.get(), ModelTemplates.FLAT_ITEM);

        // Urokodaki armor
        generator.generateFlatItem(NichirinItemRegistry.UROKODAKI_HEADPIECE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.UROKODAKI_CAPE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.UROKODAKI_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.UROKODAKI_BOOTS.get(), ModelTemplates.FLAT_ITEM);

        // Muichiro armor
        generator.generateFlatItem(NichirinItemRegistry.MUICHIRO_HEADPIECE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.MUICHIRO_CHESTPLATE.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.MUICHIRO_BOOTS.get(), ModelTemplates.FLAT_ITEM);

        // Inosuke armor
        generator.generateFlatItem(NichirinItemRegistry.BOAR_HEAD.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.INOSUKE_LEGGINGS.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.INOSUKE_BOOTS.get(), ModelTemplates.FLAT_ITEM);

        // Demon items
        generator.generateFlatItem(NichirinItemRegistry.DEMON_BLOOD_VIAL.get(), ModelTemplates.FLAT_ITEM);
        generator.generateFlatItem(NichirinItemRegistry.BLOODY_FLESH.get(), ModelTemplates.FLAT_ITEM);
    }
}
