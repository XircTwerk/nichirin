package com.xirc.nichirin.datagen;

import com.xirc.nichirin.registry.NichirinBlockRegistry;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

public class NichirinRecipeProvider extends FabricRecipeProvider {

    public NichirinRecipeProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> exporter) {
        // Katana recipe - cross shape
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.KATANA.get())
                .define('S', NichirinItemRegistry.SCARLET_GEM.get())
                .define('C', NichirinItemRegistry.SCARLET_CRIMSON_IRON_GEM.get())
                .define('I', Items.IRON_SWORD)
                .pattern(" S ")
                .pattern("CIC")
                .pattern(" S ")
                .unlockedBy("has_iron_sword", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_SWORD))
                .unlockedBy("has_scarlet_gem", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.SCARLET_GEM.get()))
                .unlockedBy("has_scarlet_crimson_iron_gem", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.SCARLET_CRIMSON_IRON_GEM.get()))
                .save(exporter);

        // Thunder katana recipe
        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, NichirinItemRegistry.THUNDER_KATANA.get())
                .requires(NichirinItemRegistry.KATANA.get())
                .requires(Items.YELLOW_DYE)
                .unlockedBy("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .unlockedBy("has_yellow_dye", InventoryChangeTrigger.TriggerInstance.hasItems(Items.YELLOW_DYE))
                .save(exporter);

        // Flame katana recipe - updated to use red dye
        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, NichirinItemRegistry.FLAME_KATANA.get())
                .requires(NichirinItemRegistry.KATANA.get())
                .requires(Items.RED_DYE)
                .unlockedBy("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .unlockedBy("has_red_dye", InventoryChangeTrigger.TriggerInstance.hasItems(Items.RED_DYE))
                .save(exporter);

        // Insect katana recipe - updated to use magenta dye
        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, NichirinItemRegistry.INSECT_KATANA.get())
                .requires(NichirinItemRegistry.KATANA.get())
                .requires(Items.MAGENTA_DYE)
                .unlockedBy("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .unlockedBy("has_magenta_dye", InventoryChangeTrigger.TriggerInstance.hasItems(Items.MAGENTA_DYE))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, NichirinItemRegistry.SOUND_KATANAS.get())
                .requires(NichirinItemRegistry.KATANA.get())
                .requires(NichirinItemRegistry.KATANA.get())
                .requires(Items.WHITE_DYE)
                .unlockedBy("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .unlockedBy("has_white_dye", InventoryChangeTrigger.TriggerInstance.hasItems(Items.WHITE_DYE))
                .save(exporter);

        // Smoke bomb recipe
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.SMOKE_BOMB.get(), 4)
                .define('G', Items.GUNPOWDER)
                .define('R', Items.RED_DYE)
                .define('P', Items.PAPER)
                .define('E', Items.EGG)
                .define('D', Items.GRAY_DYE)
                .pattern("GRG")
                .pattern("DED")
                .pattern("GPG")
                .unlockedBy("has_gunpowder", InventoryChangeTrigger.TriggerInstance.hasItems(Items.GUNPOWDER))
                .unlockedBy("has_egg", InventoryChangeTrigger.TriggerInstance.hasItems(Items.EGG))
                .save(exporter);

        // Flash bomb recipe - 2x2 checker pattern with gunpowder and paper
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.FLASH_BOMB.get(), 8)
                .define('G', Items.GUNPOWDER)
                .define('P', Items.PAPER)
                .pattern("GP")
                .pattern("PG")
                .unlockedBy("has_gunpowder", InventoryChangeTrigger.TriggerInstance.hasItems(Items.GUNPOWDER))
                .unlockedBy("has_paper", InventoryChangeTrigger.TriggerInstance.hasItems(Items.PAPER))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, NichirinItemRegistry.BENTO_BOX.get())
                .define('W', ItemTags.WOODEN_SLABS)
                .pattern("WWW")
                .pattern("W W")
                .pattern("WWW")
                .unlockedBy("has_oak_slab", InventoryChangeTrigger.TriggerInstance.hasItems(Items.OAK_SLAB))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, NichirinBlockRegistry.KATANA_HOLDER_ITEM.get())
                .define('I', Items.IRON_INGOT)
                .define('N', Items.IRON_NUGGET)
                .pattern("N N")
                .pattern("III")
                .unlockedBy("has_iron_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                .save(exporter);

        // ===== ZENITSU ARMOR RECIPES =====
        // Zenitsu Helmet - Yellow secondary, Orange primary
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.ZENITSU_HEADPIECE.get())
                .define('Y', Items.YELLOW_DYE) // Secondary
                .define('S', Items.STRING)
                .define('O', Items.ORANGE_DYE) // Primary
                .define('H', Items.NETHERITE_HELMET)
                .pattern("YSY")
                .pattern("OHO")
                .pattern("   ")
                .unlockedBy("has_netherite_helmet", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_HELMET))
                .save(exporter);

        // Zenitsu Cape/Chestplate - Yellow surrounding, Orange bottom corners
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.ZENITSU_CAPE.get())
                .define('Y', Items.YELLOW_DYE)
                .define('C', Items.NETHERITE_CHESTPLATE)
                .define('O', Items.ORANGE_DYE)
                .pattern("YYY")
                .pattern("YCY")
                .pattern("O O")
                .unlockedBy("has_netherite_chestplate", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_CHESTPLATE))
                .save(exporter);

        // Zenitsu Leggings - Brown surrounding, Orange bottom corners
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.ZENITSU_LEGGINGS.get())
                .define('B', Items.BROWN_DYE)
                .define('L', Items.NETHERITE_LEGGINGS)
                .define('O', Items.ORANGE_DYE)
                .pattern("BBB")
                .pattern("BLB")
                .pattern("O O")
                .unlockedBy("has_netherite_leggings", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_LEGGINGS))
                .save(exporter);

        // Zenitsu Boots - White on sides, Orange underneath
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.ZENITSU_BOOTS.get())
                .define('W', Items.ORANGE_DYE)
                .define('B', Items.NETHERITE_BOOTS)
                .define('O', Items.WHITE_DYE)
                .pattern("   ")
                .pattern("WBW")
                .pattern("O O")
                .unlockedBy("has_netherite_boots", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_BOOTS))
                .save(exporter);

        // ===== RENGOKU ARMOR RECIPES =====
        // Rengoku Helmet - Orange secondary, Red primary
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.RENGOKU_HEADPIECE.get())
                .define('O', Items.ORANGE_DYE) // Secondary
                .define('S', Items.STRING)
                .define('R', Items.RED_DYE) // Primary
                .define('H', Items.NETHERITE_HELMET)
                .pattern("OSO")
                .pattern("RHR")
                .pattern("   ")
                .unlockedBy("has_netherite_helmet", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_HELMET))
                .save(exporter);

        // Rengoku Cape/Chestplate - Orange surrounding, Red bottom corners
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.RENGOKU_CAPE.get())
                .define('O', Items.WHITE_DYE)
                .define('C', Items.NETHERITE_CHESTPLATE)
                .define('R', Items.RED_DYE)
                .pattern("OOO")
                .pattern("OCO")
                .pattern("R R")
                .unlockedBy("has_netherite_chestplate", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_CHESTPLATE))
                .save(exporter);

        // Rengoku Leggings - Brown surrounding, Red bottom corners
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.RENGOKU_LEGGINGS.get())
                .define('B', Items.BROWN_DYE)
                .define('L', Items.NETHERITE_LEGGINGS)
                .define('R', Items.RED_DYE)
                .pattern("BBB")
                .pattern("BLB")
                .pattern("R R")
                .unlockedBy("has_netherite_leggings", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_LEGGINGS))
                .save(exporter);

        // Rengoku Boots - White on sides, Red underneath
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.RENGOKU_BOOTS.get())
                .define('W', Items.RED_DYE)
                .define('B', Items.NETHERITE_BOOTS)
                .define('R', Items.WHITE_DYE)
                .pattern("   ")
                .pattern("WBW")
                .pattern("R R")
                .unlockedBy("has_netherite_boots", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_BOOTS))
                .save(exporter);

        // ===== TENGEN ARMOR RECIPES =====
        // Tengen Helmet - White secondary, Chain (Iron Nugget) primary
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.TENGEN_HEADPIECE.get())
                .define('W', Items.WHITE_DYE) // Secondary
                .define('S', Items.STRING)
                .define('I', Items.IRON_NUGGET) // Primary (representing chains)
                .define('H', Items.NETHERITE_HELMET)
                .pattern("WSW")
                .pattern("IHI")
                .pattern("   ")
                .unlockedBy("has_netherite_helmet", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_HELMET))
                .save(exporter);

        // Tengen Accessories/Chestplate - White surrounding, Chain bottom corners
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.TENGEN_ACCESSORIES.get())
                .define('W', Items.WHITE_DYE)
                .define('C', Items.NETHERITE_CHESTPLATE)
                .define('I', Items.IRON_NUGGET)
                .pattern("WWW")
                .pattern("WCW")
                .pattern("I I")
                .unlockedBy("has_netherite_chestplate", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_CHESTPLATE))
                .save(exporter);

        // Tengen Leggings - Brown surrounding, Chain bottom corners
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.TENGEN_LEGGINGS.get())
                .define('B', Items.BROWN_DYE)
                .define('L', Items.NETHERITE_LEGGINGS)
                .define('I', Items.IRON_NUGGET)
                .pattern("BBB")
                .pattern("BLB")
                .pattern("I I")
                .unlockedBy("has_netherite_leggings", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_LEGGINGS))
                .save(exporter);

        // Tengen Boots - White on sides, Chain underneath
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.TENGEN_BOOTS.get())
                .define('W', Items.WHITE_DYE)
                .define('B', Items.NETHERITE_BOOTS)
                .define('I', Items.IRON_NUGGET)
                .pattern("   ")
                .pattern("WBW")
                .pattern("I I")
                .unlockedBy("has_netherite_boots", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_BOOTS))
                .save(exporter);

        // ===== SHINOBU ARMOR RECIPES =====
        // Shinobu Helmet - Purple secondary, Black primary
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.SHINOBU_HEADPIECE.get())
                .define('P', Items.PURPLE_DYE) // Secondary
                .define('S', Items.STRING)
                .define('B', Items.BLACK_DYE) // Primary
                .define('H', Items.NETHERITE_HELMET)
                .pattern("PSP")
                .pattern("BHB")
                .pattern("   ")
                .unlockedBy("has_netherite_helmet", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_HELMET))
                .save(exporter);

        // Shinobu Cape/Chestplate - Purple surrounding, Black bottom corners
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.SHINOBU_CAPE.get())
                .define('P', Items.WHITE_DYE)
                .define('C', Items.NETHERITE_CHESTPLATE)
                .define('B', Items.PINK_DYE)
                .pattern("PPP")
                .pattern("PCP")
                .pattern("B B")
                .unlockedBy("has_netherite_chestplate", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_CHESTPLATE))
                .save(exporter);

        // Shinobu Leggings - Brown surrounding, Black bottom corners
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.SHINOBU_LEGGINGS.get())
                .define('B', Items.PURPLE_DYE)
                .define('L', Items.NETHERITE_LEGGINGS)
                .define('K', Items.BLACK_DYE)
                .pattern("BBB")
                .pattern("BLB")
                .pattern("K K")
                .unlockedBy("has_netherite_leggings", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_LEGGINGS))
                .save(exporter);

        // Shinobu Boots - White on sides, Black underneath
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.SHINOBU_BOOTS.get())
                .define('W', Items.PURPLE_DYE)
                .define('B', Items.NETHERITE_BOOTS)
                .define('K', Items.WHITE_DYE)
                .pattern("   ")
                .pattern("WBW")
                .pattern("K K")
                .unlockedBy("has_netherite_boots", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_BOOTS))
                .save(exporter);
    }
}