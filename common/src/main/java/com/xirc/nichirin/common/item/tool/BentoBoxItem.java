package com.xirc.nichirin.common.item.tool;

import com.xirc.nichirin.common.blocks.BentoBoxBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.MenuProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BentoBoxItem extends BlockItem {
    private static final Logger LOGGER = LoggerFactory.getLogger(BentoBoxItem.class);
    private static final int BENTO_SIZE = 9;
    private static final String ITEMS_TAG = "Items";

    public BentoBoxItem(Block block, Properties properties) {
        super(block, properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // Try to place the block first
        InteractionResult placeResult = super.useOn(context);

        // If placement was successful, play placement sound
        if (placeResult.consumesAction()) {
            Level level = context.getLevel();
            if (!level.isClientSide) {
                level.playSound(null, context.getClickedPos(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                LOGGER.info("Placed bento box block at {}", context.getClickedPos());
            }
        }

        return placeResult;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Prevent opening when using off-hand
        if (hand == InteractionHand.OFF_HAND) {
            return InteractionResultHolder.fail(stack);
        }

        // Prevent opening if player already has a container menu open (not their inventory)
        if (player.containerMenu != player.inventoryMenu) {
            return InteractionResultHolder.fail(stack);
        }

        // Check if player is sneaking - if so, allow normal block placement behavior
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        LOGGER.info("Bento box use() - Hand: {}, Player: {}", hand, player.getName().getString());

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        // Find the slot index of the bento box being opened
        int bentoBoxSlotIndex = -1;
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if (player.getInventory().items.get(i) == stack) {
                bentoBoxSlotIndex = i;
                break;
            }
        }

        player.openMenu(new BentoBoxMenuProvider(stack, bentoBoxSlotIndex));
        return InteractionResultHolder.success(stack);
    }

    /**
     * Returns the number of food items stored in the bento box
     */
    public static int getFoodCount(ItemStack stack) {
        if (!stack.hasTag() || !stack.getTag().contains(ITEMS_TAG)) {
            return 0;
        }

        ListTag nbtList = stack.getTag().getList(ITEMS_TAG, 10);
        return nbtList.size();
    }

    // Public methods for use by BentoBoxBlock
    public static AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Container inventory, int bentoBoxSlotIndex) {
        return new BentoBoxMenu(syncId, playerInventory, inventory, bentoBoxSlotIndex);
    }

    // Overloaded method for block entity form
    public static AbstractContainerMenu createBlockMenu(int syncId, Inventory playerInventory, Container inventory, BlockPos pos) {
        return new BentoBoxMenu(syncId, playerInventory, inventory, pos);
    }

    public static void loadItemsFromNbt(CompoundTag nbt, Container inventory) {
        if (!nbt.contains(ITEMS_TAG)) {
            return;
        }

        ListTag nbtList = nbt.getList(ITEMS_TAG, 10);
        for (int i = 0; i < nbtList.size(); i++) {
            CompoundTag itemTag = nbtList.getCompound(i);
            int slot = itemTag.getInt("Slot");

            if (slot >= 0 && slot < BENTO_SIZE) {
                ItemStack loadedStack = ItemStack.of(itemTag);
                inventory.setItem(slot, loadedStack);
            }
        }
    }

    public static void saveItemsToNbt(CompoundTag nbt, Container inventory) {
        ListTag nbtList = new ListTag();

        for (int i = 0; i < BENTO_SIZE; i++) {
            ItemStack slotStack = inventory.getItem(i);

            if (!slotStack.isEmpty() && canStoreItem(slotStack)) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                slotStack.save(itemTag);
                nbtList.add(itemTag);
            }
        }

        nbt.put(ITEMS_TAG, nbtList);
    }

    private static class BentoBoxMenuProvider implements MenuProvider {
        private final ItemStack bentoBoxStack;
        private final int bentoBoxSlotIndex;

        public BentoBoxMenuProvider(ItemStack stack, int slotIndex) {
            this.bentoBoxStack = stack;
            this.bentoBoxSlotIndex = slotIndex;
        }

        @Override
        public Component getDisplayName() {
            return Component.literal("Bento Box");
        }

        @Override
        public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
            SimpleContainer inventory = new SimpleContainer(BENTO_SIZE);
            loadItemsFromNbt(bentoBoxStack.getOrCreateTag(), inventory);

            return new BentoBoxMenu(syncId, playerInventory, inventory, bentoBoxSlotIndex);
        }
    }

    private static class BentoBoxMenu extends ChestMenu {
        private final int bentoBoxSlotIndex;
        private final BlockPos blockPos; // Add position for block entity validation

        public BentoBoxMenu(int syncId, Inventory playerInventory, Container inventory, int bentoBoxSlotIndex) {
            super(MenuType.GENERIC_9x1, syncId, playerInventory, inventory, 1);
            this.bentoBoxSlotIndex = bentoBoxSlotIndex;
            this.blockPos = null; // Item form - no block position

            // Replace the container slots with food-only slots
            for (int i = 0; i < BENTO_SIZE; i++) {
                Slot oldSlot = this.slots.get(i);
                Slot newSlot = new FoodOnlySlot(inventory, i, oldSlot.x, oldSlot.y);
                this.slots.set(i, newSlot);
            }

            // Replace the bento box slot with a disabled slot (only if it's in player inventory)
            if (bentoBoxSlotIndex >= 0) {
                replaceBentoBoxSlotWithDisabled(playerInventory);
            }

            LOGGER.info("Created bento box menu with {} slots", BENTO_SIZE);
        }

        // Constructor for block entity form
        public BentoBoxMenu(int syncId, Inventory playerInventory, Container inventory, BlockPos pos) {
            super(MenuType.GENERIC_9x1, syncId, playerInventory, inventory, 1);
            this.bentoBoxSlotIndex = -1; // Not in player inventory
            this.blockPos = pos; // Block form - has block position

            // Replace the container slots with food-only slots
            for (int i = 0; i < BENTO_SIZE; i++) {
                Slot oldSlot = this.slots.get(i);
                Slot newSlot = new FoodOnlySlot(inventory, i, oldSlot.x, oldSlot.y);
                this.slots.set(i, newSlot);
            }

            LOGGER.info("Created bento box block menu with {} slots at {}", BENTO_SIZE, pos);
        }

        private void replaceBentoBoxSlotWithDisabled(Inventory playerInventory) {
            // Find the slot in the menu that corresponds to the bento box
            for (int i = BENTO_SIZE; i < this.slots.size(); i++) {
                Slot slot = this.slots.get(i);
                if (slot.container == playerInventory && slot.getContainerSlot() == bentoBoxSlotIndex) {
                    // Replace with disabled slot
                    DisabledSlot disabledSlot = new DisabledSlot(playerInventory, bentoBoxSlotIndex, slot.x, slot.y);
                    this.slots.set(i, disabledSlot);
                    break;
                }
            }
        }

        @Override
        public void clicked(int slotId, int button, ClickType clickType, Player player) {
            // Prevent any interaction with the disabled bento box slot
            if (slotId >= 0 && slotId < this.slots.size()) {
                Slot slot = this.slots.get(slotId);
                if (slot instanceof DisabledSlot) {
                    // Don't call super - completely prevent the interaction
                    return;
                }
            }

            // For all other slots, proceed normally
            super.clicked(slotId, button, clickType, player);
        }

        @Override
        public void removed(Player player) {
            super.removed(player);
            // Save items back to the bento box (only if it's in player inventory)
            if (bentoBoxSlotIndex >= 0 && bentoBoxSlotIndex < player.getInventory().items.size()) {
                ItemStack bentoBoxStack = player.getInventory().items.get(bentoBoxSlotIndex);
                if (bentoBoxStack.getItem() instanceof BentoBoxItem) {
                    saveItemsToNbt(bentoBoxStack.getOrCreateTag(), (Container) this.getContainer());
                    LOGGER.info("Saved bento box for player: {}", player.getName().getString());
                }
            }
        }

        @Override
        public boolean stillValid(Player player) {
            // Block entity form - check if block still exists and player is in range
            if (blockPos != null) {
                if (player.level().getBlockEntity(blockPos) instanceof BentoBoxBlock.BentoBoxBlockEntity) {
                    return player.distanceToSqr(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D) <= 64.0D;
                }
                return false;
            }

            // Item form - check if the bento box is still in the correct slot
            if (bentoBoxSlotIndex >= 0 && bentoBoxSlotIndex < player.getInventory().items.size()) {
                ItemStack stack = player.getInventory().items.get(bentoBoxSlotIndex);
                return stack.getItem() instanceof BentoBoxItem;
            }
            return false;
        }
    }

    // Disabled slot that prevents pickup
    private static class DisabledSlot extends Slot {
        public DisabledSlot(Container container, int index, int xPosition, int yPosition) {
            super(container, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    public static boolean canStoreItem(ItemStack item) {
        if (item.isEmpty()) {
            return false;
        }

        if (item.getItem() instanceof BentoBoxItem) {
            return false;
        }

        if (item.getItem().getFoodProperties() != null) {
            return true;
        }

        TagKey<Item> bentotag = TagKey.create(Registries.ITEM, new ResourceLocation("nichirin", "can_be_bento_boxed"));
        return item.is(bentotag);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        int foodCount = getFoodCount(stack);

        tooltip.add(Component.literal("§6Food Items: §f" + foodCount + "/" + BENTO_SIZE));
        tooltip.add(Component.literal("§7Right-click to open"));
        tooltip.add(Component.literal("§7Right-click on block to place"));

        if (foodCount > 0) {
            tooltip.add(Component.literal("§8Contains food items"));
        }
    }

    private static class FoodOnlySlot extends Slot {
        public FoodOnlySlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return canStoreItem(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }
    }
}