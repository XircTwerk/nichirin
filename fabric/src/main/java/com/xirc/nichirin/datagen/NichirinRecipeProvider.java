package com.xirc.nichirin.datagen;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
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
    }
}