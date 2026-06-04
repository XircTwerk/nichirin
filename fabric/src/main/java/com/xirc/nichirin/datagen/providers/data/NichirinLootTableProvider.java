package com.xirc.nichirin.datagen.providers.data;

import com.xirc.nichirin.registry.NichirinBlockRegistry;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;

import java.util.concurrent.CompletableFuture;

public class NichirinLootTableProvider extends FabricBlockLootTableProvider {
    private final Holder<Enchantment> fortune;

    public NichirinLootTableProvider(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(dataOutput, registriesFuture);
        fortune = registriesFuture.join().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
    }

    @Override
    public void generate() {
        // Scarlet Crimson Iron Sand - drops itself with silk touch, otherwise drops gem
        add(NichirinBlockRegistry.SCARLET_CRIMSON_IRON_SAND.get(),
                createSilkTouchDispatchTable(
                        NichirinBlockRegistry.SCARLET_CRIMSON_IRON_SAND.get(),
                        LootItem.lootTableItem(NichirinItemRegistry.SCARLET_CRIMSON_IRON_GEM.get())
                                .apply(ApplyBonusCount.addOreBonusCount(fortune))
                )
        );

        // Scarlet Ore - drops itself with silk touch, otherwise drops gem
        add(NichirinBlockRegistry.SCARLET_ORE.get(),
                createSilkTouchDispatchTable(
                        NichirinBlockRegistry.SCARLET_ORE.get(),
                        LootItem.lootTableItem(NichirinItemRegistry.SCARLET_GEM.get())
                                .apply(ApplyBonusCount.addOreBonusCount(fortune))
                )
        );

        // Regular blocks that drop themselves
        dropSelf(NichirinBlockRegistry.KATANA_HOLDER_BLOCK.get());
        dropSelf(NichirinBlockRegistry.BENTO_BOX_BLOCK.get());

        // Wisteria wood set - all drop themselves
        dropSelf(NichirinBlockRegistry.WISTERIA_LOG.get());
        dropSelf(NichirinBlockRegistry.STRIPPED_WISTERIA_LOG.get());
        dropSelf(NichirinBlockRegistry.WISTERIA_WOOD.get());
        dropSelf(NichirinBlockRegistry.STRIPPED_WISTERIA_WOOD.get());
        dropSelf(NichirinBlockRegistry.WISTERIA_PLANKS.get());
        dropSelf(NichirinBlockRegistry.WISTERIA_STAIRS.get());
        dropSelf(NichirinBlockRegistry.WISTERIA_SLAB.get());
        dropSelf(NichirinBlockRegistry.WISTERIA_FENCE.get());
        dropSelf(NichirinBlockRegistry.WISTERIA_FENCE_GATE.get());
        dropSelf(NichirinBlockRegistry.WISTERIA_PRESSURE_PLATE.get());
        dropSelf(NichirinBlockRegistry.WISTERIA_BUTTON.get());
        dropSelf(NichirinBlockRegistry.WISTERIA_TRAPDOOR.get());

        // Special cases
        // Leaves should drop themselves with silk touch, otherwise drop saplings/sticks (but we don't have saplings yet)
        add(NichirinBlockRegistry.WISTERIA_LEAVES.get(), createLeavesDrops(
                NichirinBlockRegistry.WISTERIA_LEAVES.get(),
                NichirinBlockRegistry.WISTERIA_LEAVES.get(), // Use leaves as placeholder for now
                NORMAL_LEAVES_SAPLING_CHANCES));

        // Door drops the item, not the block
        add(NichirinBlockRegistry.WISTERIA_DOOR.get(),
                createDoorTable(NichirinBlockRegistry.WISTERIA_DOOR.get()));
    }
}