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
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Individual Sound Katana that converts back to sound katanas when moved or dropped
 */
public class IndividualSoundKatana extends SimpleKatana {

    private final boolean isRightKatana;

    private static final Map<UUID, Integer> activePairTicks = new HashMap<>();

    public IndividualSoundKatana(Properties properties, boolean isRightKatana) {
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

        if (slotId < 0 || slotId > 40) {
            // Item is in a non-standard inventory (chest, hopper, etc.) â€” convert and drop
            ItemStack soundKatanas = new ItemStack(NichirinItemRegistry.SOUND_KATANAS.get());
            if (entity.level().getBlockEntity(entity.blockPosition()) != null) {
                entity.spawnAtLocation(soundKatanas);
            }
            stack.shrink(stack.getCount());
            return;
        }

        if (hasSoundKatanasItem(player)) {
            stack.shrink(stack.getCount());
            return;
        }

        UUID playerId = player.getUUID();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean properPair = (isRightSoundKatana(mainHand) || isLeftSoundKatana(mainHand)) &&
                (isRightSoundKatana(offHand) || isLeftSoundKatana(offHand)) &&
                !mainHand.equals(offHand);

        if (properPair) {
            activePairTicks.put(playerId, activePairTicks.getOrDefault(playerId, 0) + 1);
        } else {
            int previousTicks = activePairTicks.getOrDefault(playerId, 0);

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
            player.level().getEntitiesOfClass(ItemEntity.class,
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

    private boolean hasSoundKatanasItem(Player player) {
        // Check all inventory slots for sound_katanas item
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (slotStack.getItem() == NichirinItemRegistry.SOUND_KATANAS.get()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Static cleanup method
     */
    public static void cleanupPlayerState(UUID playerId) {
        activePairTicks.remove(playerId);
    }
}
