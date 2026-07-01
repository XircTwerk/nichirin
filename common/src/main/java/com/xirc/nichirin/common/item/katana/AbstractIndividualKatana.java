package com.xirc.nichirin.common.item.katana;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One half (left or right) of a dual katana set. Converts the whole set back into its single-stack
 * holder when the pair is broken (a half is moved out of hand, dropped, or the holder reappears).
 * Concrete subclasses only supply the three items via {@link KatanaSet}.
 */
public abstract class AbstractIndividualKatana extends Katana implements KatanaSet {

    /** Whether this is the right-hand half (vs left). Kept for the registry constructor signature. */
    protected final boolean isRightKatana;

    // Shared across individual-katana types: only one set can be wielded at a time.
    private static final Map<UUID, Integer> ACTIVE_PAIR_TICKS = new HashMap<>();
    private static final int PAIR_GRACE_TICKS = 5;

    protected AbstractIndividualKatana(Properties properties, boolean isRightKatana) {
        super(properties);
        this.isRightKatana = isRightKatana;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player)) return;
        if (level.isClientSide) return;

        // Outside the normal player inventory (chest, hopper, …): drop the holder and remove this half.
        if (slotId < 0 || slotId > 40) {
            entity.spawnAtLocation(new ItemStack(holderItem()));
            stack.shrink(stack.getCount());
            return;
        }

        // Holder already present somewhere → this loose half is redundant, delete it.
        if (hasHolderInInventory(player)) {
            stack.shrink(stack.getCount());
            return;
        }

        UUID playerId = player.getUUID();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean properPair = isIndividual(mainHand) && isIndividual(offHand) && !mainHand.equals(offHand);

        if (properPair) {
            ACTIVE_PAIR_TICKS.merge(playerId, 1, Integer::sum);
        } else {
            if (ACTIVE_PAIR_TICKS.getOrDefault(playerId, 0) >= PAIR_GRACE_TICKS) {
                handlePairLoss(player, slotId);
            }
            ACTIVE_PAIR_TICKS.put(playerId, 0);
        }
    }

    private void handlePairLoss(Player player, int slotId) {
        UUID playerId = player.getUUID();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean inInventorySlot = slotId >= 0 && slotId < 36
                && slotId != player.getInventory().selected
                && !isOffhandSlot(slotId);

        if (inInventorySlot) {
            // Moved to inventory — convert in place to the holder.
            player.getInventory().setItem(slotId, new ItemStack(holderItem()));
        } else {
            // Likely dropped — spawn the holder and clean up any dropped individual halves nearby.
            player.spawnAtLocation(new ItemStack(holderItem()));
            player.level().getEntitiesOfClass(ItemEntity.class,
                    player.getBoundingBox().inflate(3.0)).forEach(itemEntity -> {
                if (isIndividual(itemEntity.getItem())) itemEntity.discard();
            });
        }

        // Always clear the other half and restore the stashed offhand.
        if (isIndividual(mainHand)) player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (isIndividual(offHand)) player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

        AbstractDualKatana.restorePlayerOffhand(player);
        ACTIVE_PAIR_TICKS.put(playerId, 0);
    }

    private boolean isOffhandSlot(int slotId) {
        return slotId == 40;
    }

    private boolean hasHolderInInventory(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (isHolder(player.getInventory().getItem(i))) return true;
        }
        return false;
    }

    public static void cleanupPlayerState(UUID playerId) {
        ACTIVE_PAIR_TICKS.remove(playerId);
    }
}
