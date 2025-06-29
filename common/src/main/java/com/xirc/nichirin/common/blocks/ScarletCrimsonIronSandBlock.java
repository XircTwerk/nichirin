package com.xirc.nichirin.common.blocks;

import com.xirc.nichirin.common.registry.NichirinItemRegistry;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class ScarletCrimsonIronSandBlock extends DropExperienceBlock {

    public ScarletCrimsonIronSandBlock() {
        super(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.COLOR_RED)
                        .strength(0.5f)
                        .requiresCorrectToolForDrops()
                        .sound(SoundType.SAND),
                UniformInt.of(2, 5));
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ItemStack tool = builder.getParameter(LootContextParams.TOOL);

        // Check for Silk Touch
        if (tool != null && EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) {
            return List.of(new ItemStack(this));
        }

        return List.of(new ItemStack(NichirinItemRegistry.SCARLET_CRIMSON_IRON_GEM.get()));
    }
}