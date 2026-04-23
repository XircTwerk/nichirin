package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holder item that transforms into dual-wielded beast katanas (Inosuke style).
 * 1:1 with SoundKatana but for Beast Breathing.
 */
public class BeastKatana extends Item {

    private static final Map<UUID, ItemStack> storedOffhandItems = new HashMap<>();

    public BeastKatana(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player)) return;
        if (level.isClientSide) return;

        if (hasIndividualKatanas(player)) {
            removeAllIndividualKatanas(player);
        }

        UUID playerId = player.getUUID();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (isBeastKatanasItem(stack) && isSelected && slotId == player.getInventory().selected) {
            if (isBeastKatanasItem(mainHand)) {
                storedOffhandItems.put(playerId, offHand.copy());
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(NichirinItemRegistry.RIGHT_BEAST_KATANA.get()));
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(NichirinItemRegistry.LEFT_BEAST_KATANA.get()));
            }
        }

        if (isLeftBeastKatana(mainHand) || isRightBeastKatana(offHand)) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(NichirinItemRegistry.BEAST_KATANAS.get()));
            restoreOffhand(player);
        }
    }

    private boolean hasIndividualKatanas(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (isRightBeastKatana(s) || isLeftBeastKatana(s)) return true;
        }
        return false;
    }

    private void removeAllIndividualKatanas(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (isRightBeastKatana(s) || isLeftBeastKatana(s)) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }

    public static void restorePlayerOffhand(Player player) {
        UUID playerId = player.getUUID();
        ItemStack previous = storedOffhandItems.get(playerId);
        if (previous != null && !previous.isEmpty()) {
            player.setItemInHand(InteractionHand.OFF_HAND, previous);
        } else {
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
        storedOffhandItems.remove(playerId);
    }

    private void restoreOffhand(Player player) {
        restorePlayerOffhand(player);
    }

    private boolean isBeastKatanasItem(ItemStack stack) {
        return stack.getItem() == NichirinItemRegistry.BEAST_KATANAS.get();
    }

    private boolean isRightBeastKatana(ItemStack stack) {
        return stack.getItem() == NichirinItemRegistry.RIGHT_BEAST_KATANA.get();
    }

    private boolean isLeftBeastKatana(ItemStack stack) {
        return stack.getItem() == NichirinItemRegistry.LEFT_BEAST_KATANA.get();
    }

    public static boolean isPlayerDualWieldingBeastKatanas(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.getItem() == NichirinItemRegistry.RIGHT_BEAST_KATANA.get() &&
                off.getItem() == NichirinItemRegistry.LEFT_BEAST_KATANA.get();
    }

    public static void cleanupDisconnectedPlayer(UUID playerId) {
        storedOffhandItems.remove(playerId);
    }
}
