package com.xirc.nichirin.common.item.katana;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.InteractionHand;

/**
 * Individual beast katana (left or right) that converts back to beast katanas when the pair is broken.
 * 1:1 with IndividualSoundKatana.
 */
public class IndividualBeastKatana extends SimpleKatana {

    private final boolean isRightKatana;
    private static final Map<UUID, Integer> activePairTicks = new HashMap<>();

    public IndividualBeastKatana(Properties properties, boolean isRightKatana) {
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
            entity.spawnAtLocation(new ItemStack(NichirinItemRegistry.BEAST_KATANAS.get()));
            stack.shrink(stack.getCount());
            return;
        }

        if (hasBeastKatanasItem(player)) {
            stack.shrink(stack.getCount());
            return;
        }

        UUID playerId = player.getUUID();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean properPair = (isRightBeast(mainHand) || isLeftBeast(mainHand)) &&
                (isRightBeast(offHand) || isLeftBeast(offHand)) &&
                !mainHand.equals(offHand);

        if (properPair) {
            activePairTicks.put(playerId, activePairTicks.getOrDefault(playerId, 0) + 1);
        } else {
            int prev = activePairTicks.getOrDefault(playerId, 0);
            if (prev >= 5) handlePairLoss(player, stack, slotId);
            activePairTicks.put(playerId, 0);
        }
    }

    private void handlePairLoss(Player player, ItemStack stack, int slotId) {
        UUID playerId = player.getUUID();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean inInventorySlot = slotId >= 0 && slotId < 36 &&
                slotId != player.getInventory().selected && !isOffhandSlot(slotId);

        if (inInventorySlot) {
            player.getInventory().setItem(slotId, new ItemStack(NichirinItemRegistry.BEAST_KATANAS.get()));
        } else {
            player.spawnAtLocation(new ItemStack(NichirinItemRegistry.BEAST_KATANAS.get()));
            player.level().getEntitiesOfClass(ItemEntity.class,
                    player.getBoundingBox().inflate(3.0)).forEach(ie -> {
                ItemStack dropped = ie.getItem();
                if (isRightBeast(dropped) || isLeftBeast(dropped)) ie.discard();
            });
        }

        if (isRightBeast(mainHand) || isLeftBeast(mainHand)) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        if (isRightBeast(offHand) || isLeftBeast(offHand)) {
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }

        BeastKatana.restorePlayerOffhand(player);
        activePairTicks.put(playerId, 0);
    }

    private boolean isOffhandSlot(int slotId) {
        return slotId == 40;
    }

    private boolean isRightBeast(ItemStack stack) {
        return stack.getItem() == NichirinItemRegistry.RIGHT_BEAST_KATANA.get();
    }

    private boolean isLeftBeast(ItemStack stack) {
        return stack.getItem() == NichirinItemRegistry.LEFT_BEAST_KATANA.get();
    }

    private boolean hasBeastKatanasItem(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).getItem() == NichirinItemRegistry.BEAST_KATANAS.get()) return true;
        }
        return false;
    }

    public static void cleanupPlayerState(UUID playerId) {
        activePairTicks.remove(playerId);
    }
}
