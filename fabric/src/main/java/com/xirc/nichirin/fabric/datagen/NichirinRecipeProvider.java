package com.xirc.nichirin.fabric.datagen;

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
        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, NichirinItemRegistry.THUNDER_KATANA.get())
                .requires(NichirinItemRegistry.KATANA.get())
                .requires(Items.YELLOW_DYE)
                .unlockedBy("has_katana", InventoryChangeTrigger.TriggerInstance.hasItems(NichirinItemRegistry.KATANA.get()))
                .unlockedBy("has_yellow_dye", InventoryChangeTrigger.TriggerInstance.hasItems(Items.YELLOW_DYE))
                .save(exporter);
    }
}