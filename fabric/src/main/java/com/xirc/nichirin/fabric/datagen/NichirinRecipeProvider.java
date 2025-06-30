package com.xirc.nichirin.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.recipes.FinishedRecipe;

import java.util.function.Consumer;

public class NichirinRecipeProvider extends FabricRecipeProvider {

    public NichirinRecipeProvider(FabricDataOutput output) {
        super(output);
    }
    @Override
    public void buildRecipes(Consumer<FinishedRecipe> exporter) {

    }
}
