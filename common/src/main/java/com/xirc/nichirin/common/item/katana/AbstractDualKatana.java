package com.xirc.nichirin.common.item.katana;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Holder item that equips its independently usable right/left katanas when selected. */
public abstract class AbstractDualKatana extends Item implements KatanaSet {

    protected AbstractDualKatana(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player) || level.isClientSide) return;
        if (!isSelected || slotId != player.getInventory().selected) return;
        if (!isHolder(player.getMainHandItem()) || !player.getOffhandItem().isEmpty()) return;

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(rightItem()));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(leftItem()));
    }

    public boolean isPlayerDualWielding(Player player) {
        return player.getMainHandItem().getItem() == rightItem()
                && player.getOffhandItem().getItem() == leftItem();
    }
}
