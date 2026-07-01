package com.xirc.nichirin.common.item.katana;

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
 * Holder item that transforms into a dual-wielded pair of katanas when selected in the main hand
 * (Sound, Beast, …). Concrete subclasses only supply the three items via {@link KatanaSet}.
 *
 * <p>The previous offhand item is stashed per-player so it can be restored when the pair reverts.
 * The stash and pair-state maps are shared across all dual-katana types, which is safe because a
 * player can only hold one holder at a time (the tick removes mismatched individuals).</p>
 */
public abstract class AbstractDualKatana extends Item implements KatanaSet {

    // Shared across dual-katana types: a player can only have one set transformed at a time.
    private static final Map<UUID, ItemStack> STORED_OFFHAND = new HashMap<>();

    protected AbstractDualKatana(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player)) return;
        if (level.isClientSide) return; // Server-side only

        // Mutual exclusion: never leave loose individual halves lying around when holding the holder.
        if (hasIndividualKatanas(player)) {
            removeAllIndividualKatanas(player);
        }

        UUID playerId = player.getUUID();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        // Transform: holder in the selected main hand → dual pair (right main, left off).
        if (isHolder(stack) && isSelected && slotId == player.getInventory().selected) {
            if (isHolder(mainHand)) { // Double-check we're still holding it
                STORED_OFFHAND.put(playerId, offHand.copy());
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(rightItem()));
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(leftItem()));
            }
        }

        // Wrong-placement prevention: an individual half in the wrong hand reverts to the holder.
        if (isLeft(mainHand) || isRight(offHand)) {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(holderItem()));
            restorePlayerOffhand(player);
        }
    }

    private boolean hasIndividualKatanas(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (isIndividual(player.getInventory().getItem(i))) return true;
        }
        return false;
    }

    private void removeAllIndividualKatanas(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (isIndividual(player.getInventory().getItem(i))) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /** True if the player currently has the right/left pair in the correct hands. */
    public boolean isPlayerDualWielding(Player player) {
        return player.getMainHandItem().getItem() == rightItem()
                && player.getOffhandItem().getItem() == leftItem();
    }

    /** Restores (or clears) the offhand item stashed when the holder transformed. */
    public static void restorePlayerOffhand(Player player) {
        UUID playerId = player.getUUID();
        ItemStack previous = STORED_OFFHAND.remove(playerId);
        player.setItemInHand(InteractionHand.OFF_HAND,
                previous != null && !previous.isEmpty() ? previous : ItemStack.EMPTY);
    }

    /** Drop the stashed offhand entry for a disconnected player so the map can't grow unbounded. */
    public static void cleanupDisconnectedPlayer(UUID playerId) {
        STORED_OFFHAND.remove(playerId);
    }
}
