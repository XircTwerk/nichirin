package com.xirc.nichirin.datagen.providers.data;

import com.xirc.nichirin.registry.NichirinBlockRegistry;
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
            ResourceLocation.fromNamespaceAndPath("nichirin", "can_be_bento_boxed"));

    public NichirinTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        // Add blocks that need iron tools
        getOrCreateTagBuilder(BlockTags.NEEDS_IRON_TOOL)
                .add(NichirinBlockRegistry.SCARLET_CRIMSON_IRON_SAND.get())
                .add(NichirinBlockRegistry.SCARLET_ORE.get())
                .add(NichirinBlockRegistry.KATANA_HOLDER_BLOCK.get());

        // Add blocks mineable with shovel
        getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_SHOVEL)
                .add(NichirinBlockRegistry.SCARLET_CRIMSON_IRON_SAND.get());

        // Add blocks mineable with pickaxe
        getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(NichirinBlockRegistry.SCARLET_ORE.get())
                .add(NichirinBlockRegistry.KATANA_HOLDER_BLOCK.get());

        // Fences must be in the fence tags or FenceBlock.connectsTo() won't link them to
        // other fences (it only connects to fence gates and solid blocks without this).
        // #minecraft:fences includes #minecraft:wooden_fences, so adding here covers both.
        getOrCreateTagBuilder(BlockTags.WOODEN_FENCES)
                .add(NichirinBlockRegistry.WISTERIA_FENCE.get());
        getOrCreateTagBuilder(BlockTags.FENCE_GATES)
                .add(NichirinBlockRegistry.WISTERIA_FENCE_GATE.get());

        getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_AXE)
                .add(NichirinBlockRegistry.WISTERIA_LOG.get())
                .add(NichirinBlockRegistry.STRIPPED_WISTERIA_LOG.get())
                .add(NichirinBlockRegistry.WISTERIA_WOOD.get())
                .add(NichirinBlockRegistry.STRIPPED_WISTERIA_WOOD.get())
                .add(NichirinBlockRegistry.WISTERIA_PLANKS.get())
                .add(NichirinBlockRegistry.WISTERIA_STAIRS.get())
                .add(NichirinBlockRegistry.WISTERIA_SLAB.get())
                .add(NichirinBlockRegistry.WISTERIA_FENCE.get())
                .add(NichirinBlockRegistry.WISTERIA_FENCE_GATE.get())
                .add(NichirinBlockRegistry.WISTERIA_DOOR.get())
                .add(NichirinBlockRegistry.WISTERIA_TRAPDOOR.get())
                .add(NichirinBlockRegistry.WISTERIA_PRESSURE_PLATE.get())
                .add(NichirinBlockRegistry.WISTERIA_BUTTON.get())
                .add(NichirinBlockRegistry.WISTERIA_SIGN.get())
                .add(NichirinBlockRegistry.WISTERIA_WALL_SIGN.get())
                .add(NichirinBlockRegistry.WISTERIA_HANGING_SIGN.get())
                .add(NichirinBlockRegistry.WISTERIA_WALL_HANGING_SIGN.get());

        getOrCreateTagBuilder(BlockTags.LOGS)
                .add(NichirinBlockRegistry.WISTERIA_LOG.get())
                .add(NichirinBlockRegistry.STRIPPED_WISTERIA_LOG.get())
                .add(NichirinBlockRegistry.WISTERIA_WOOD.get())
                .add(NichirinBlockRegistry.STRIPPED_WISTERIA_WOOD.get());
        getOrCreateTagBuilder(BlockTags.LOGS_THAT_BURN)
                .add(NichirinBlockRegistry.WISTERIA_LOG.get())
                .add(NichirinBlockRegistry.STRIPPED_WISTERIA_LOG.get())
                .add(NichirinBlockRegistry.WISTERIA_WOOD.get())
                .add(NichirinBlockRegistry.STRIPPED_WISTERIA_WOOD.get());
        getOrCreateTagBuilder(BlockTags.PLANKS)
                .add(NichirinBlockRegistry.WISTERIA_PLANKS.get());
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

            getOrCreateTagBuilder(ItemTags.LOGS)
                    .add(NichirinBlockRegistry.WISTERIA_LOG_ITEM.get())
                    .add(NichirinBlockRegistry.STRIPPED_WISTERIA_LOG_ITEM.get())
                    .add(NichirinBlockRegistry.WISTERIA_WOOD_ITEM.get())
                    .add(NichirinBlockRegistry.STRIPPED_WISTERIA_WOOD_ITEM.get());
            getOrCreateTagBuilder(ItemTags.LOGS_THAT_BURN)
                    .add(NichirinBlockRegistry.WISTERIA_LOG_ITEM.get())
                    .add(NichirinBlockRegistry.STRIPPED_WISTERIA_LOG_ITEM.get())
                    .add(NichirinBlockRegistry.WISTERIA_WOOD_ITEM.get())
                    .add(NichirinBlockRegistry.STRIPPED_WISTERIA_WOOD_ITEM.get());
        }
    }
}
