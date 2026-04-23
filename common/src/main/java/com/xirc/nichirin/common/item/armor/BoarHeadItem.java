package com.xirc.nichirin.common.item.armor;

import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;

/**
 * Inosuke's boar head helmet. Diamond-tier defense, completely unbreakable.
 * Obtained by killing a boar; worn to unlock Beast Breathing.
 */
public class BoarHeadItem extends NichirinArmorItem {

    public BoarHeadItem(Properties properties) {
        super(ArmorMaterials.DIAMOND, Type.HELMET, properties);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }
}
