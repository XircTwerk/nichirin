package com.xirc.nichirin.common.item.tool;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BentoBoxItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(BentoBoxItem.class);
    private static final int BENTO_SIZE = 6; // 3x2 grid
    private static final String ITEMS_TAG = "Items";

    // We'll need to register this MenuType in your mod's initialization
    public static MenuType<BentoBoxMenu> BENTO_BOX_MENU_TYPE;

    public BentoBoxItem(Properties properties) {
        super(properties.stacksTo(1)); // Bento boxes shouldn't stack
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        LOGGER.info("BentoBox use() called - Hand: {}, Item: {}, Player: {}",
                hand, stack.getDisplayName().getString(), player.getName().getString());

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            // Get the actual slot index for the hand
            int slotIndex = hand == InteractionHand.MAIN_HAND ?
                    player.getInventory().selected : -1; // Offhand is slot -1

            LOGGER.info("Opening bento box - Hand: {}, SlotIndex: {}", hand, slotIndex);

            // Create context for this bento box
            BentoBoxContext context = new BentoBoxContext(hand, slotIndex);

            // Open the bento box GUI using Architectury's menu system
            MenuRegistry.openExtendedMenu(serverPlayer, new BentoBoxMenuProvider(context));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        NonNullList<ItemStack> items = getStoredItems(stack);
        int foodCount = 0;

        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                foodCount++;
            }
        }

        tooltip.add(Component.literal("§6Food Items: §f" + foodCount + "/" + BENTO_SIZE));
        tooltip.add(Component.literal("§7Right-click to open"));

        if (foodCount > 0) {
            tooltip.add(Component.literal("§8Contains:"));
            for (ItemStack item : items) {
                if (!item.isEmpty()) {
                    tooltip.add(Component.literal("§8• " + item.getCount() + "x " +
                            item.getDisplayName().getString()));
                }
            }
        }
    }

    public static NonNullList<ItemStack> getStoredItems(ItemStack bentoBox) {
        NonNullList<ItemStack> items = NonNullList.withSize(BENTO_SIZE, ItemStack.EMPTY);

        CompoundTag tag = bentoBox.getTag();
        if (tag != null && tag.contains(ITEMS_TAG)) {
            ListTag itemsList = tag.getList(ITEMS_TAG, 10); // 10 = CompoundTag type

            for (int i = 0; i < itemsList.size() && i < BENTO_SIZE; i++) {
                CompoundTag itemTag = itemsList.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;

                if (slot >= 0 && slot < BENTO_SIZE) {
                    items.set(slot, ItemStack.of(itemTag));
                }
            }
        }

        return items;
    }

    public static void setStoredItems(ItemStack bentoBox, NonNullList<ItemStack> items) {
        CompoundTag tag = bentoBox.getOrCreateTag();
        ListTag itemsList = new ListTag();

        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            if (!item.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                item.save(itemTag);
                itemsList.add(itemTag);
            }
        }

        tag.put(ITEMS_TAG, itemsList);
    }

    /**
     * Checks if an item can be stored in the bento box
     * Only allows food items (consumables with nutritional value)
     * PREVENTS SELF-INSERTION
     */
    public static boolean canStoreItem(ItemStack item) {
        if (item.isEmpty()) {
            return false;
        }

        // CRITICAL: Prevent bento boxes from being inserted into themselves
        if (item.getItem() instanceof BentoBoxItem) {
            LOGGER.warn("Attempted to insert bento box into itself! Blocking.");
            return false;
        }

        // Check if item is edible (has food properties)
        if (item.getItem().getFoodProperties() != null) {
            LOGGER.debug("Item {} accepted - has food properties", item.getDisplayName().getString());
            return true;
        }

        // Additional whitelist for special food items that might not have standard food properties
        String itemName = item.getItem().toString().toLowerCase();

        // Common food-related keywords
        boolean isFood = itemName.contains("food") ||
                itemName.contains("bread") ||
                itemName.contains("soup") ||
                itemName.contains("stew") ||
                itemName.contains("cake") ||
                itemName.contains("cookie") ||
                itemName.contains("pie") ||
                itemName.contains("tea") ||
                itemName.contains("rice") ||
                itemName.contains("noodle") ||
                itemName.contains("bento") ||
                itemName.contains("meal");

        if (isFood) {
            LOGGER.debug("Item {} accepted - matches food keyword", item.getDisplayName().getString());
        } else {
            LOGGER.debug("Item {} rejected - not food", item.getDisplayName().getString());
        }

        return isFood;
    }

    /**
     * Context class that handles bento box access - inspired by BackpackContext
     */
    public static class BentoBoxContext {
        private final InteractionHand hand;
        private final int slotIndex;

        public BentoBoxContext(InteractionHand hand, int slotIndex) {
            this.hand = hand;
            this.slotIndex = slotIndex;
        }

        public ItemStack getBentoBoxStack(Player player) {
            ItemStack stack;
            if (hand == InteractionHand.MAIN_HAND) {
                stack = player.getInventory().getItem(slotIndex);
                LOGGER.debug("Getting bento from main hand slot {} - Item: {}",
                        slotIndex, stack.getDisplayName().getString());
            } else {
                stack = player.getOffhandItem();
                LOGGER.debug("Getting bento from offhand - Item: {}",
                        stack.getDisplayName().getString());
            }
            return stack;
        }

        public boolean canInteractWith(Player player) {
            ItemStack stack = getBentoBoxStack(player);
            boolean canInteract = !stack.isEmpty() && stack.getItem() instanceof BentoBoxItem;

            if (!canInteract) {
                LOGGER.warn("Cannot interact with bento box - Stack: {}, Item: {}",
                        stack.isEmpty() ? "EMPTY" : stack.getDisplayName().getString(),
                        stack.isEmpty() ? "NONE" : stack.getItem().getClass().getSimpleName());
            }

            return canInteract;
        }

        public void toBuffer(FriendlyByteBuf buffer) {
            buffer.writeEnum(hand);
            buffer.writeInt(slotIndex);
        }

        public static BentoBoxContext fromBuffer(FriendlyByteBuf buffer) {
            return new BentoBoxContext(buffer.readEnum(InteractionHand.class), buffer.readInt());
        }

        public InteractionHand getHand() {
            return hand;
        }

        public int getSlotIndex() {
            return slotIndex;
        }
    }

    private static class BentoBoxMenuProvider implements ExtendedMenuProvider {
        private final BentoBoxContext context;

        public BentoBoxMenuProvider(BentoBoxContext context) {
            this.context = context;
        }

        @Override
        public Component getDisplayName() {
            return Component.translatable("container.bento_box");
        }

        @Override
        public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
            return new BentoBoxMenu(containerId, playerInventory, context);
        }

        @Override
        public void saveExtraData(FriendlyByteBuf buf) {
            context.toBuffer(buf);
        }
    }

    public static class BentoBoxMenu extends AbstractContainerMenu {
        private final BentoBoxContext context;
        private final Player player;
        private final SimpleContainer bentoContainer;
        private ItemStack bentoBoxStack;

        public BentoBoxMenu(int containerId, Inventory playerInventory, BentoBoxContext context) {
            super(BENTO_BOX_MENU_TYPE, containerId);
            this.context = context;
            this.player = playerInventory.player;
            this.bentoContainer = new SimpleContainer(BENTO_SIZE) {
                @Override
                public boolean canPlaceItem(int slot, ItemStack stack) {
                    return canStoreItem(stack);
                }
            };
            this.bentoBoxStack = context.getBentoBoxStack(player);

            LOGGER.info("Creating BentoBoxMenu - ContainerID: {}, Hand: {}, SlotIndex: {}",
                    containerId, context.getHand(), context.getSlotIndex());
            LOGGER.info("BentoBox stack: {}", bentoBoxStack.getDisplayName().getString());

            // Validate we have the right item
            if (bentoBoxStack.isEmpty() || !(bentoBoxStack.getItem() instanceof BentoBoxItem)) {
                LOGGER.error("CRITICAL: BentoBox menu created with invalid item! Stack: {}",
                        bentoBoxStack.isEmpty() ? "EMPTY" : bentoBoxStack.getDisplayName().getString());
            }

            // Load items from the bento box
            NonNullList<ItemStack> items = getStoredItems(bentoBoxStack);
            LOGGER.info("Loading {} items from bento box NBT", items.stream().mapToInt(s -> s.isEmpty() ? 0 : 1).sum());

            for (int i = 0; i < items.size(); i++) {
                ItemStack item = items.get(i);
                if (!item.isEmpty()) {
                    LOGGER.debug("Loading slot {}: {} x{}", i, item.getDisplayName().getString(), item.getCount());
                }
                bentoContainer.setItem(i, items.get(i));
            }

            // Add bento box slots (food only) - 3x2 grid
            for (int i = 0; i < BENTO_SIZE; i++) {
                int row = i / 3;
                int col = i % 3;
                this.addSlot(new FoodOnlySlot(bentoContainer, i, 62 + col * 18, 17 + row * 18));
                LOGGER.debug("Added bento slot {} at position ({}, {})", i, 62 + col * 18, 17 + row * 18);
            }

            // Add player inventory slots (3x9 grid)
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
                }
            }

            // Add player hotbar slots
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
            }

            LOGGER.info("BentoBoxMenu created successfully with {} bento slots and {} total slots",
                    BENTO_SIZE, this.slots.size());
        }

        // Factory method for network construction
        public static BentoBoxMenu fromNetwork(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
            BentoBoxContext context = BentoBoxContext.fromBuffer(buf);
            return new BentoBoxMenu(containerId, playerInventory, context);
        }

        @Override
        public boolean stillValid(Player player) {
            boolean valid = context.canInteractWith(player);

            if (!valid) {
                LOGGER.warn("BentoBox menu no longer valid for player {}", player.getName().getString());
            }

            return valid;
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            ItemStack result = ItemStack.EMPTY;
            Slot slot = this.slots.get(index);

            LOGGER.debug("QuickMoveStack called - index: {}, total slots: {}", index, this.slots.size());

            if (slot != null && slot.hasItem()) {
                ItemStack slotItem = slot.getItem();
                result = slotItem.copy();

                LOGGER.debug("Moving item: {} from slot {}", slotItem.getDisplayName().getString(), index);

                if (index < BENTO_SIZE) {
                    // Moving from bento to player inventory (slots 6-41)
                    LOGGER.debug("Moving from bento to player inventory");
                    if (!this.moveItemStackTo(slotItem, BENTO_SIZE, this.slots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // Moving from player inventory to bento (slots 0-5)
                    if (canStoreItem(slotItem)) {
                        LOGGER.debug("Moving food item from player to bento");
                        if (!this.moveItemStackTo(slotItem, 0, BENTO_SIZE, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        LOGGER.debug("Item rejected - not food: {}", slotItem.getDisplayName().getString());
                        return ItemStack.EMPTY; // Can't store non-food items
                    }
                }

                if (slotItem.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
            }

            return result;
        }

        @Override
        public void removed(Player player) {
            LOGGER.info("BentoBox menu closing for player {}", player.getName().getString());

            super.removed(player);

            // Save items back to the bento box
            NonNullList<ItemStack> items = NonNullList.withSize(BENTO_SIZE, ItemStack.EMPTY);
            for (int i = 0; i < BENTO_SIZE; i++) {
                ItemStack item = bentoContainer.getItem(i);
                items.set(i, item);
                if (!item.isEmpty()) {
                    LOGGER.debug("Saving slot {}: {} x{}", i, item.getDisplayName().getString(), item.getCount());
                }
            }

            // Get the current bento box stack and update it
            ItemStack currentBento = context.getBentoBoxStack(player);
            if (!currentBento.isEmpty() && currentBento.getItem() instanceof BentoBoxItem) {
                LOGGER.info("Saving items to bento box: {}", currentBento.getDisplayName().getString());
                setStoredItems(currentBento, items);
            } else {
                LOGGER.error("CRITICAL: Cannot save bento box items - current stack is invalid!");
                LOGGER.error("Current stack: {}", currentBento.isEmpty() ? "EMPTY" : currentBento.getDisplayName().getString());

                // Try to drop items into the world to prevent loss
                for (ItemStack item : items) {
                    if (!item.isEmpty()) {
                        LOGGER.warn("Dropping item into world: {} x{}", item.getDisplayName().getString(), item.getCount());
                        player.drop(item, false);
                    }
                }
            }
        }

        @Override
        public void broadcastChanges() {
            super.broadcastChanges();

            // Keep reference to current bento box fresh and validate it hasn't moved
            ItemStack newBentoStack = context.getBentoBoxStack(player);

            if (!ItemStack.isSameItem(this.bentoBoxStack, newBentoStack)) {
                LOGGER.warn("CRITICAL: BentoBox has moved or changed during use!");
                LOGGER.warn("Original: {}", this.bentoBoxStack.getDisplayName().getString());
                LOGGER.warn("Current: {}", newBentoStack.isEmpty() ? "EMPTY" : newBentoStack.getDisplayName().getString());

                // Force close the menu if the item has moved
                if (newBentoStack.isEmpty() || !(newBentoStack.getItem() instanceof BentoBoxItem)) {
                    LOGGER.error("Forcing menu close - bento box is gone!");
                    player.closeContainer();
                    return;
                }
            }

            this.bentoBoxStack = newBentoStack;
        }
    }

    /**
     * Custom slot that only accepts food items
     */
    private static class FoodOnlySlot extends Slot {
        public FoodOnlySlot(SimpleContainer container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            boolean canPlace = canStoreItem(stack);

            if (!canPlace && !stack.isEmpty()) {
                LOGGER.debug("FoodOnlySlot rejected item: {}", stack.getDisplayName().getString());
            }

            return canPlace;
        }

        @Override
        public int getMaxStackSize() {
            return 64; // Standard stack size for food items
        }
    }
}