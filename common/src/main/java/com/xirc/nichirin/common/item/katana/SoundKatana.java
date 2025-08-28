package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holder item that transforms into dual-wielded sound katanas
 */
public class SoundKatana extends Item {

    // Store what was previously in the offhand for each player
    private static final Map<UUID, ItemStack> storedOffhandItems = new HashMap<>();

    public SoundKatana(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player)) return;
        if (level.isClientSide) return; // Server-side only

        // MUTUAL EXCLUSION: Check if player has any individual katanas and remove them
        if (hasIndividualKatanas(player)) {
            removeAllIndividualKatanas(player);
        }

        UUID playerId = player.getUUID();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        // TRANSFORM: Sound katanas in main hand → dual katanas
        if (isSoundKatanasItem(stack) && isSelected && slotId == player.getInventory().selected) {
            if (isSoundKatanasItem(mainHand)) { // Double-check we're still holding it
                // Store current offhand
                storedOffhandItems.put(playerId, offHand.copy());

                // Transform
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(NichirinItemRegistry.RIGHT_SOUND_KATANA.get()));
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(NichirinItemRegistry.LEFT_SOUND_KATANA.get()));
            }
        }

        // WRONG PLACEMENT PREVENTION
        if (isLeftSoundKatana(mainHand) || isRightSoundKatana(offHand)) {
            // Convert back to sound katanas
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(NichirinItemRegistry.SOUND_KATANAS.get()));
            restoreOffhand(player);
        }
    }

    private boolean hasIndividualKatanas(Player player) {
        // Check all inventory slots for individual katanas
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (isRightSoundKatana(slotStack) || isLeftSoundKatana(slotStack)) {
                return true;
            }
        }
        return false;
    }

    private void removeAllIndividualKatanas(Player player) {
        // Remove all individual katanas from inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (isRightSoundKatana(slotStack) || isLeftSoundKatana(slotStack)) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * Static method to restore a player's offhand
     */
    public static void restorePlayerOffhand(Player player) {
        UUID playerId = player.getUUID();
        ItemStack previousOffhand = storedOffhandItems.get(playerId);

        if (previousOffhand != null && !previousOffhand.isEmpty()) {
            player.setItemInHand(InteractionHand.OFF_HAND, previousOffhand);
        } else {
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }

        storedOffhandItems.remove(playerId);
    }

    private void restoreOffhand(Player player) {
        restorePlayerOffhand(player);
    }

    private boolean isSoundKatanasItem(ItemStack stack) {
        return stack.getItem() == NichirinItemRegistry.SOUND_KATANAS.get();
    }

    private boolean isRightSoundKatana(ItemStack stack) {
        return stack.getItem() == NichirinItemRegistry.RIGHT_SOUND_KATANA.get();
    }

    private boolean isLeftSoundKatana(ItemStack stack) {
        return stack.getItem() == NichirinItemRegistry.LEFT_SOUND_KATANA.get();
    }

    /**
     * Static utility method to check if a player is currently dual-wielding sound katanas
     */
    public static boolean isPlayerDualWieldingSoundKatanas(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        return mainHand.getItem() == NichirinItemRegistry.RIGHT_SOUND_KATANA.get() &&
                offHand.getItem() == NichirinItemRegistry.LEFT_SOUND_KATANA.get();
    }

    /**
     * Cleanup method to remove stored offhand items for disconnected players
     */
    public static void cleanupDisconnectedPlayer(UUID playerId) {
        storedOffhandItems.remove(playerId);
    }
}