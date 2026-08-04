package com.xirc.nichirin.datagen.providers.data;

import com.xirc.nichirin.registry.NichirinBlockRegistry;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.concurrent.CompletableFuture;

public class NichirinRecipeProvider extends FabricRecipeProvider {

    public NichirinRecipeProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    public void buildRecipes(RecipeOutput exporter) {
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
        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, NichirinItemRegistry.FLAME_KATANA.get())
                .requires(NichirinItemRegistry.KATANA.get())
                .requires(Items.RED_DYE)
                .unlockedBy("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .unlockedBy("has_red_dye", InventoryChangeTrigger.TriggerInstance.hasItems(Items.RED_DYE))
                .save(exporter);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, NichirinItemRegistry.INSECT_KATANA.get())
                .requires(NichirinItemRegistry.KATANA.get())
                .requires(Items.MAGENTA_DYE)
                .unlockedBy("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .unlockedBy("has_magenta_dye", InventoryChangeTrigger.TriggerInstance.hasItems(Items.MAGENTA_DYE))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, NichirinItemRegistry.MIST_KATANA.get())
                .requires(NichirinItemRegistry.KATANA.get())
                .requires(Items.CYAN_DYE)
                .unlockedBy("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .unlockedBy("has_cyan_dye", InventoryChangeTrigger.TriggerInstance.hasItems(Items.CYAN_DYE))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, NichirinItemRegistry.SABITO_KATANA.get())
                .requires(NichirinItemRegistry.KATANA.get())
                .requires(Items.GREEN_DYE)
                .unlockedBy("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .unlockedBy("has_green_dye", InventoryChangeTrigger.TriggerInstance.hasItems(Items.GREEN_DYE))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, NichirinItemRegistry.UROKODAKI_KATANA.get())
                .requires(NichirinItemRegistry.KATANA.get())
                .requires(Items.LIGHT_BLUE_DYE)
                .unlockedBy("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .unlockedBy("has_light_blue_dye", InventoryChangeTrigger.TriggerInstance.hasItems(Items.LIGHT_BLUE_DYE))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, NichirinItemRegistry.GIYU_KATANA.get())
                .requires(NichirinItemRegistry.KATANA.get())
                .requires(Items.BLUE_DYE)
                .requires(Items.PINK_DYE)
                .unlockedBy("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.RIGHT_SOUND_KATANA.get())
                .define('K', NichirinItemRegistry.KATANA.get())
                .define('D', Items.WHITE_DYE)
                .pattern("K")
                .pattern("D")
                .unlockedBy("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.LEFT_SOUND_KATANA.get())
                .define('K', NichirinItemRegistry.KATANA.get())
                .define('D', Items.WHITE_DYE)
                .pattern("D")
                .pattern("K")
                .unlockedBy("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.RIGHT_BEAST_KATANA.get())
                .define('K', NichirinItemRegistry.KATANA.get())
                .define('D', Items.GRAY_DYE)
                .pattern("K")
                .pattern("D")
                .unlockedBy("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.LEFT_BEAST_KATANA.get())
                .define('K', NichirinItemRegistry.KATANA.get())
                .define('D', Items.GRAY_DYE)
                .pattern("D")
                .pattern("K")
                .unlockedBy("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, NichirinItemRegistry.SOUND_KATANAS.get())
                .requires(NichirinItemRegistry.RIGHT_SOUND_KATANA.get())
                .requires(NichirinItemRegistry.LEFT_SOUND_KATANA.get())
                .unlockedBy("has_sound_katanas", InventoryChangeTrigger.TriggerInstance.hasItems(
                        NichirinItemRegistry.RIGHT_SOUND_KATANA.get(), NichirinItemRegistry.LEFT_SOUND_KATANA.get()))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, NichirinItemRegistry.BEAST_KATANAS.get())
                .requires(NichirinItemRegistry.RIGHT_BEAST_KATANA.get())
                .requires(NichirinItemRegistry.LEFT_BEAST_KATANA.get())
                .unlockedBy("has_beast_katanas", InventoryChangeTrigger.TriggerInstance.hasItems(
                        NichirinItemRegistry.RIGHT_BEAST_KATANA.get(), NichirinItemRegistry.LEFT_BEAST_KATANA.get()))
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

        // Bullet - shotgun shell for Genya's double-barrel
        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, NichirinItemRegistry.BULLET.get(), 2)
                .requires(Items.GUNPOWDER)
                .requires(Items.IRON_NUGGET)
                .unlockedBy("has_gunpowder", InventoryChangeTrigger.TriggerInstance.hasItems(Items.GUNPOWDER))
                .save(exporter);

        // Genya's double-barrel recipe is hand-written in resources (data/nichirin/recipe/genya_db.json):
        // AzureLib stamps a random per-stack az_id component onto geo-item stacks at construction, so a
        // datagen-built result bakes a fresh random UUID into the recipe JSON on every run.

        // Wisteria signs (mirror the vanilla oak sign / hanging sign recipes)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, NichirinBlockRegistry.WISTERIA_SIGN_ITEM.get(), 3)
                .define('P', NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get())
                .define('S', Items.STICK)
                .pattern("PPP")
                .pattern("PPP")
                .pattern(" S ")
                .unlockedBy("has_wisteria_planks", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, NichirinBlockRegistry.WISTERIA_HANGING_SIGN_ITEM.get(), 6)
                .define('C', Items.CHAIN)
                .define('W', NichirinBlockRegistry.STRIPPED_WISTERIA_LOG_ITEM.get())
                .pattern("C C")
                .pattern("WWW")
                .pattern("WWW")
                .unlockedBy("has_stripped_wisteria_log", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinBlockRegistry.STRIPPED_WISTERIA_LOG_ITEM.get()))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, NichirinItemRegistry.WISTERIA_FLOWER.get(), 2)
                .requires(NichirinBlockRegistry.WISTERIA_LEAVES_ITEM.get())
                .unlockedBy("has_wisteria_leaves", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinBlockRegistry.WISTERIA_LEAVES_ITEM.get()))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BREWING, NichirinItemRegistry.WISTERIA_EXTRACT.get())
                .requires(NichirinItemRegistry.WISTERIA_FLOWER.get())
                .requires(Items.GLASS_BOTTLE)
                .unlockedBy("has_wisteria_flower", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.WISTERIA_FLOWER.get()))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BREWING, NichirinItemRegistry.WISTERIA_TEA.get())
                .requires(NichirinItemRegistry.WISTERIA_EXTRACT.get())
                .requires(Items.POTION)
                .unlockedBy("has_wisteria_extract", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.WISTERIA_EXTRACT.get()))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, NichirinItemRegistry.WISTERIA_ARROW.get(), 4)
                .requires(NichirinItemRegistry.WISTERIA_EXTRACT.get())
                .requires(Items.ARROW, 4)
                .unlockedBy("has_wisteria_extract", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.WISTERIA_EXTRACT.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, NichirinBlockRegistry.WISTERIA_LANTERN_ITEM.get())
                .define('E', NichirinItemRegistry.WISTERIA_EXTRACT.get())
                .define('L', Items.LANTERN)
                .pattern(" E ")
                .pattern("ELE")
                .pattern(" E ")
                .unlockedBy("has_wisteria_extract", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.WISTERIA_EXTRACT.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, NichirinBlockRegistry.BENTO_BOX_BLOCK.get())
                .define('W', ItemTags.WOODEN_SLABS)
                .pattern("WWW")
                .pattern("W W")
                .pattern("WWW")
                .unlockedBy("has_oak_slab", InventoryChangeTrigger.TriggerInstance.hasItems(Items.OAK_SLAB))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, NichirinBlockRegistry.KATANA_HOLDER_BLOCK.get())
                .define('I', Items.IRON_INGOT)
                .define('N', Items.IRON_NUGGET)
                .pattern("N N")
                .pattern("III")
                .unlockedBy("has_iron_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, NichirinItemRegistry.ONIGIRI.get(), 2)
                .requires(NichirinItemRegistry.RICE.get())
                .requires(Items.DRIED_KELP)
                .unlockedBy("has_rice", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.RICE.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, NichirinItemRegistry.DRINKING_GOURD.get())
                .define('B', Items.OAK_BUTTON)
                .define('G', Items.GLASS_BOTTLE)
                .define('P', Items.OAK_PLANKS)
                .pattern(" B ")
                .pattern(" G ")
                .pattern(" P ")
                .unlockedBy("has_glass_bottle", InventoryChangeTrigger.TriggerInstance.hasItems(Items.GLASS_BOTTLE))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get(), 4)
                .requires(Ingredient.of(
                        NichirinBlockRegistry.WISTERIA_LOG_ITEM.get(),
                        NichirinBlockRegistry.STRIPPED_WISTERIA_LOG_ITEM.get(),
                        NichirinBlockRegistry.WISTERIA_WOOD_ITEM.get(),
                        NichirinBlockRegistry.STRIPPED_WISTERIA_WOOD_ITEM.get()))
                .unlockedBy("has_wisteria_log", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinBlockRegistry.WISTERIA_LOG_ITEM.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, NichirinBlockRegistry.WISTERIA_WOOD_ITEM.get(), 3)
                .define('L', NichirinBlockRegistry.WISTERIA_LOG_ITEM.get())
                .pattern("LL")
                .pattern("LL")
                .unlockedBy("has_wisteria_log", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinBlockRegistry.WISTERIA_LOG_ITEM.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, NichirinBlockRegistry.STRIPPED_WISTERIA_WOOD_ITEM.get(), 3)
                .define('L', NichirinBlockRegistry.STRIPPED_WISTERIA_LOG_ITEM.get())
                .pattern("LL")
                .pattern("LL")
                .unlockedBy("has_stripped_wisteria_log", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinBlockRegistry.STRIPPED_WISTERIA_LOG_ITEM.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, NichirinBlockRegistry.WISTERIA_STAIRS_ITEM.get(), 4)
                .define('P', NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get())
                .pattern("P  ")
                .pattern("PP ")
                .pattern("PPP")
                .unlockedBy("has_wisteria_planks", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, NichirinBlockRegistry.WISTERIA_SLAB_ITEM.get(), 6)
                .define('P', NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get())
                .pattern("PPP")
                .unlockedBy("has_wisteria_planks", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, NichirinBlockRegistry.WISTERIA_FENCE_ITEM.get(), 3)
                .define('P', NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get())
                .define('S', Items.STICK)
                .pattern("PSP")
                .pattern("PSP")
                .unlockedBy("has_wisteria_planks", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, NichirinBlockRegistry.WISTERIA_FENCE_GATE_ITEM.get())
                .define('P', NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get())
                .define('S', Items.STICK)
                .pattern("SPS")
                .pattern("SPS")
                .unlockedBy("has_wisteria_planks", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, NichirinBlockRegistry.WISTERIA_DOOR_ITEM.get(), 3)
                .define('P', NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get())
                .pattern("PP")
                .pattern("PP")
                .pattern("PP")
                .unlockedBy("has_wisteria_planks", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, NichirinBlockRegistry.WISTERIA_TRAPDOOR_ITEM.get(), 2)
                .define('P', NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get())
                .pattern("PPP")
                .pattern("PPP")
                .unlockedBy("has_wisteria_planks", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get()))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, NichirinBlockRegistry.WISTERIA_PRESSURE_PLATE_ITEM.get())
                .define('P', NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get())
                .pattern("PP")
                .unlockedBy("has_wisteria_planks", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get()))
                .save(exporter);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, NichirinBlockRegistry.WISTERIA_BUTTON_ITEM.get())
                .requires(NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get())
                .unlockedBy("has_wisteria_planks", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinBlockRegistry.WISTERIA_PLANKS_ITEM.get()))
                .save(exporter);

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

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.JIGORO_HEADPIECE.get())
                .define('Y', Items.YELLOW_DYE)
                .define('S', Items.STRING)
                .define('W', Items.WHITE_DYE)
                .define('H', Items.NETHERITE_HELMET)
                .pattern("YSY")
                .pattern("WHW")
                .pattern("   ")
                .unlockedBy("has_netherite_helmet", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_HELMET))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.JIGORO_CAPE.get())
                .define('Y', Items.YELLOW_DYE)
                .define('C', Items.NETHERITE_CHESTPLATE)
                .define('W', Items.WHITE_DYE)
                .pattern("YYY")
                .pattern("YCY")
                .pattern("W W")
                .unlockedBy("has_netherite_chestplate", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_CHESTPLATE))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.JIGORO_LEGGINGS.get())
                .define('B', Items.BROWN_DYE)
                .define('L', Items.NETHERITE_LEGGINGS)
                .define('W', Items.WHITE_DYE)
                .pattern("BBB")
                .pattern("BLB")
                .pattern("W W")
                .unlockedBy("has_netherite_leggings", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_LEGGINGS))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.JIGORO_BOOTS.get())
                .define('Y', Items.YELLOW_DYE)
                .define('B', Items.NETHERITE_BOOTS)
                .define('W', Items.WHITE_DYE)
                .pattern("   ")
                .pattern("YBY")
                .pattern("W W")
                .unlockedBy("has_netherite_boots", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_BOOTS))
                .save(exporter);

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

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.SABITO_HEADPIECE.get())
                .define('G', Items.GREEN_DYE)
                .define('S', Items.STRING)
                .define('W', Items.WHITE_DYE)
                .define('H', Items.NETHERITE_HELMET)
                .pattern("GSG")
                .pattern("WHW")
                .pattern("   ")
                .unlockedBy("has_netherite_helmet", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_HELMET))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.SABITO_CAPE.get())
                .define('G', Items.GREEN_DYE)
                .define('C', Items.NETHERITE_CHESTPLATE)
                .define('O', Items.ORANGE_DYE)
                .pattern("GGG")
                .pattern("GCG")
                .pattern("O O")
                .unlockedBy("has_netherite_chestplate", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_CHESTPLATE))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.SABITO_LEGGINGS.get())
                .define('G', Items.GREEN_DYE)
                .define('L', Items.NETHERITE_LEGGINGS)
                .define('B', Items.BLACK_DYE)
                .pattern("GGG")
                .pattern("GLG")
                .pattern("B B")
                .unlockedBy("has_netherite_leggings", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_LEGGINGS))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.SABITO_BOOTS.get())
                .define('B', Items.BLACK_DYE)
                .define('O', Items.NETHERITE_BOOTS)
                .define('G', Items.GREEN_DYE)
                .pattern("   ")
                .pattern("BOB")
                .pattern("G G")
                .unlockedBy("has_netherite_boots", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_BOOTS))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.GIYU_HEADPIECE.get())
                .define('B', Items.BLACK_DYE)
                .define('S', Items.STRING)
                .define('C', Items.CYAN_DYE)
                .define('H', Items.NETHERITE_HELMET)
                .pattern("BSB")
                .pattern("CHC")
                .pattern("   ")
                .unlockedBy("has_netherite_helmet", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_HELMET))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.GIYU_CAPE.get())
                .define('G', Items.GREEN_DYE)
                .define('C', Items.NETHERITE_CHESTPLATE)
                .define('R', Items.RED_DYE)
                .pattern("GGG")
                .pattern("GCG")
                .pattern("R R")
                .unlockedBy("has_netherite_chestplate", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_CHESTPLATE))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.GIYU_LEGGINGS.get())
                .define('B', Items.BLACK_DYE)
                .define('L', Items.NETHERITE_LEGGINGS)
                .define('C', Items.CYAN_DYE)
                .pattern("BBB")
                .pattern("BLB")
                .pattern("C C")
                .unlockedBy("has_netherite_leggings", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_LEGGINGS))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.GIYU_BOOTS.get())
                .define('B', Items.BLACK_DYE)
                .define('O', Items.NETHERITE_BOOTS)
                .define('R', Items.RED_DYE)
                .pattern("   ")
                .pattern("BOB")
                .pattern("R R")
                .unlockedBy("has_netherite_boots", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_BOOTS))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.UROKODAKI_HEADPIECE.get())
                .define('L', Items.LIGHT_BLUE_DYE)
                .define('S', Items.STRING)
                .define('W', Items.WHITE_DYE)
                .define('H', Items.NETHERITE_HELMET)
                .pattern("LSL")
                .pattern("WHW")
                .pattern("   ")
                .unlockedBy("has_netherite_helmet", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_HELMET))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.UROKODAKI_CAPE.get())
                .define('L', Items.LIGHT_BLUE_DYE)
                .define('C', Items.NETHERITE_CHESTPLATE)
                .define('W', Items.WHITE_DYE)
                .pattern("LLL")
                .pattern("LCL")
                .pattern("W W")
                .unlockedBy("has_netherite_chestplate", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_CHESTPLATE))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.UROKODAKI_LEGGINGS.get())
                .define('L', Items.LIGHT_BLUE_DYE)
                .define('P', Items.NETHERITE_LEGGINGS)
                .define('G', Items.GRAY_DYE)
                .pattern("LLL")
                .pattern("LPL")
                .pattern("G G")
                .unlockedBy("has_netherite_leggings", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_LEGGINGS))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.UROKODAKI_BOOTS.get())
                .define('G', Items.GRAY_DYE)
                .define('B', Items.NETHERITE_BOOTS)
                .define('L', Items.LIGHT_BLUE_DYE)
                .pattern("   ")
                .pattern("GBG")
                .pattern("L L")
                .unlockedBy("has_netherite_boots", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_BOOTS))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.MUICHIRO_HEADPIECE.get())
                .define('C', Items.CYAN_DYE)
                .define('S', Items.STRING)
                .define('B', Items.BLACK_DYE)
                .define('H', Items.NETHERITE_HELMET)
                .pattern("CSC")
                .pattern("BHB")
                .pattern("   ")
                .unlockedBy("has_netherite_helmet", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_HELMET))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.MUICHIRO_CHESTPLATE.get())
                .define('C', Items.CYAN_DYE)
                .define('P', Items.NETHERITE_CHESTPLATE)
                .define('B', Items.BLACK_DYE)
                .pattern("CCC")
                .pattern("CPC")
                .pattern("B B")
                .unlockedBy("has_netherite_chestplate", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_CHESTPLATE))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.MUICHIRO_BOOTS.get())
                .define('B', Items.BLACK_DYE)
                .define('O', Items.NETHERITE_BOOTS)
                .define('C', Items.CYAN_DYE)
                .pattern("   ")
                .pattern("BOB")
                .pattern("C C")
                .unlockedBy("has_netherite_boots", InventoryChangeTrigger.TriggerInstance.hasItems(Items.NETHERITE_BOOTS))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.BOAR_HEAD.get())
                .define('B', Items.BROWN_DYE)
                .define('W', Items.WHITE_DYE)
                .define('L', Items.LEATHER)
                .define('H', Items.DIAMOND_HELMET)
                .pattern("BWB")
                .pattern("LHL")
                .pattern("   ")
                .unlockedBy("has_diamond_helmet", InventoryChangeTrigger.TriggerInstance.hasItems(Items.DIAMOND_HELMET))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.INOSUKE_LEGGINGS.get())
                .define('B', Items.BROWN_DYE)
                .define('L', Items.DIAMOND_LEGGINGS)
                .define('G', Items.LIGHT_GRAY_DYE)
                .pattern("BBB")
                .pattern("BLB")
                .pattern("G G")
                .unlockedBy("has_diamond_leggings", InventoryChangeTrigger.TriggerInstance.hasItems(Items.DIAMOND_LEGGINGS))
                .save(exporter);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, NichirinItemRegistry.INOSUKE_BOOTS.get())
                .define('G', Items.GRAY_DYE)
                .define('B', Items.DIAMOND_BOOTS)
                .define('R', Items.BROWN_DYE)
                .pattern("   ")
                .pattern("GBG")
                .pattern("R R")
                .unlockedBy("has_diamond_boots", InventoryChangeTrigger.TriggerInstance.hasItems(Items.DIAMOND_BOOTS))
                .save(exporter);

        // Sakuramochi
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, NichirinItemRegistry.SAKURAMOCHI.get())
                .requires(NichirinItemRegistry.PINK_MOCHI.get())
                .requires(Items.COCOA_BEANS)
                .requires(Items.CHERRY_LEAVES)
                .unlockedBy("has_mochi", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.MOCHI.get()))
                .save(exporter);
    }
}
