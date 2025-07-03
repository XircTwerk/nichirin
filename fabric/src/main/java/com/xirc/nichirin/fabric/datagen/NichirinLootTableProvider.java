package com.xirc.nichirin.fabric.datagen;

import com.xirc.nichirin.registry.OreRegistry;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public class NichirinLootTableProvider extends FabricBlockLootTableProvider {
    public NichirinLootTableProvider(FabricDataOutput dataOutput) {
        super(dataOutput);
    }

    @Override
    public void generate() {
        // Scarlet Crimson Iron Sand - drops itself with silk touch, otherwise drops gem
        add(OreRegistry.SCARLET_CRIMSON_IRON_SAND.get(),
                createSilkTouchDispatchTable(
                        OreRegistry.SCARLET_CRIMSON_IRON_SAND.get(),
                        LootItem.lootTableItem(NichirinItemRegistry.SCARLET_CRIMSON_IRON_GEM.get())
                                .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))
                )
        );

        // Scarlet Ore - drops itself with silk touch, otherwise drops gem
        add(OreRegistry.SCARLET_ORE.get(),
                createSilkTouchDispatchTable(
                        OreRegistry.SCARLET_ORE.get(),
                        LootItem.lootTableItem(NichirinItemRegistry.SCARLET_GEM.get())
                                .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))
                )
        );
    }
}