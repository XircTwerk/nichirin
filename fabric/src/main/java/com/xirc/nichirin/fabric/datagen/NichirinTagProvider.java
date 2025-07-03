package com.xirc.nichirin.fabric.datagen;

import com.xirc.nichirin.registry.OreRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.BlockTags;
import java.util.concurrent.CompletableFuture;

public class NichirinTagProvider extends FabricTagProvider.BlockTagProvider {

    public NichirinTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        // Add blocks that need iron tools
        getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL)
                .add(OreRegistry.SCARLET_CRIMSON_IRON_SAND.get())
                .add(OreRegistry.SCARLET_ORE.get());

        // Add blocks mineable with shovel
        getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_SHOVEL)
                .add(OreRegistry.SCARLET_CRIMSON_IRON_SAND.get());

        // Add blocks mineable with pickaxe
        getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(OreRegistry.SCARLET_ORE.get());
    }
}