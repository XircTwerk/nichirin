package com.xirc.nichirin.fabric.datagen;

import com.xirc.nichirin.registry.NichirinOreRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.concurrent.CompletableFuture;

public class NichirinTagProvider extends FabricTagProvider.BlockTagProvider {

    // Create custom item tag for bento box compatibility
    public static final TagKey<Item> CAN_BE_BENTO_BOXED = TagKey.create(Registries.ITEM,
            new ResourceLocation("nichirin", "can_be_bento_boxed"));

    public NichirinTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        // Add blocks that need iron tools
        getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL)
                .add(NichirinOreRegistry.SCARLET_CRIMSON_IRON_SAND.get())
                .add(NichirinOreRegistry.SCARLET_ORE.get());

        // Add blocks mineable with shovel
        getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_SHOVEL)
                .add(NichirinOreRegistry.SCARLET_CRIMSON_IRON_SAND.get());

        // Add blocks mineable with pickaxe
        getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(NichirinOreRegistry.SCARLET_ORE.get());
    }

    // Add ItemTagProvider as nested class for item tags
    public static class ItemTagProvider extends FabricTagProvider.ItemTagProvider {

        public ItemTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
            super(output, completableFuture);
        }

        @Override
        protected void addTags(HolderLookup.Provider arg) {
            // Create the can_be_bento_boxed tag with all food items
            getOrCreateTagBuilder(CAN_BE_BENTO_BOXED)
                    // Add all vanilla food items
                    .add(Items.APPLE)
            // You can also add entire tags instead of individual items
            // .addTag(ItemTags.FISHES) // This would add all fish items
            // Add any modded food items here as needed
            ;
        }
    }
}