package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Individual Sound Katana that converts back to sound katanas when moved or dropped
 */
public class IndividualSoundKatana extends SimpleKatana {

    private final boolean isRightKatana;

    // Track which players have active katana pairs
    private static final Map<UUID, Integer> activePairTicks = new HashMap<>();

    public IndividualSoundKatana(Properties properties, boolean isRightKatana) {
        super(properties);
        this.isRightKatana = isRightKatana;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false; // Prevent moving to containers
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);

        if (!(entity instanceof Player player)) return;
        if (level.isClientSide) return;

        UUID playerId = player.getUUID();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        // Check if we have any pair of individual katanas (regardless of which is in which hand)
        boolean properPair = (isRightSoundKatana(mainHand) || isLeftSoundKatana(mainHand)) &&
                (isRightSoundKatana(offHand) || isLeftSoundKatana(offHand)) &&
                !mainHand.equals(offHand); // Make sure they're different items

        if (properPair) {
            // We have a proper pair, increment ticks
            activePairTicks.put(playerId, activePairTicks.getOrDefault(playerId, 0) + 1);
        } else {
            // No proper pair
            int previousTicks = activePairTicks.getOrDefault(playerId, 0);

            // If we had a pair for at least 5 ticks and now we don't, handle it
            if (previousTicks >= 5) {
                handlePairLoss(player, stack, slotId);
            }

            activePairTicks.put(playerId, 0);
        }
    }

    private void handlePairLoss(Player player, ItemStack stack, int slotId) {
        UUID playerId = player.getUUID();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        // Check if this katana is in an inventory slot (moved, not dropped)
        boolean inInventorySlot = slotId >= 0 && slotId < 36 &&
                slotId != player.getInventory().selected &&
                !isOffhandSlot(slotId);

        if (inInventorySlot) {
            // Moved to inventory - convert to sound katanas
            player.getInventory().setItem(slotId, new ItemStack(NichirinItemRegistry.SOUND_KATANAS.get()));
        } else {
            // Likely dropped - spawn sound katanas and remove dropped individual katanas
            ItemStack soundKatanas = new ItemStack(NichirinItemRegistry.SOUND_KATANAS.get());
            player.spawnAtLocation(soundKatanas);

            // Remove any dropped individual katana entities nearby
            player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    player.getBoundingBox().inflate(3.0)).forEach(itemEntity -> {
                ItemStack droppedStack = itemEntity.getItem();
                if (isRightSoundKatana(droppedStack) || isLeftSoundKatana(droppedStack)) {
                    itemEntity.discard(); // Remove the dropped individual katana
                }
            });
        }

        // Always remove the other katana and restore offhand
        if (isRightSoundKatana(mainHand) || isLeftSoundKatana(mainHand)) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        if (isRightSoundKatana(offHand) || isLeftSoundKatana(offHand)) {
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }

        SoundKatana.restorePlayerOffhand(player);
        activePairTicks.put(playerId, 0);
    }

    private boolean isOffhandSlot(int slotId) {
        return slotId == 40; // Offhand slot index
    }

    private boolean isRightSoundKatana(ItemStack stack) {
        return stack.getItem() == NichirinItemRegistry.RIGHT_SOUND_KATANA.get();
    }

    private boolean isLeftSoundKatana(ItemStack stack) {
        return stack.getItem() == NichirinItemRegistry.LEFT_SOUND_KATANA.get();
    }

    /**
     * Static cleanup method
     */
    public static void cleanupPlayerState(UUID playerId) {
        activePairTicks.remove(playerId);
    }
}