package com.xirc.nichirin.common.item.tool;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BentoBoxItem extends Item {
    private static final Logger LOGGER = LoggerFactory.getLogger(BentoBoxItem.class);
    private static final int BENTO_SIZE = 9;
    private static final String ITEMS_TAG = "Items";

    public BentoBoxItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        LOGGER.info("BentoBox use() - Hand: {}, Player: {}", hand, player.getName().getString());

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Bento Box");
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                SimpleContainer inventory = new SimpleContainer(BENTO_SIZE);
                loadItemsFromNbt(stack, inventory);

                ChestMenu handler = new ChestMenu(MenuType.GENERIC_9x1, syncId, playerInventory, inventory, 1) {
                    @Override
                    public void removed(Player player) {
                        super.removed(player);
                        saveItemsToNbt(stack, inventory);
                        LOGGER.info("Saved bento box for player: {}", player.getName().getString());
                    }
                };

                for (int i = 0; i < BENTO_SIZE; i++) {
                    Slot oldSlot = handler.slots.get(i);
                    Slot newSlot = new FoodOnlySlot(inventory, i, oldSlot.x, oldSlot.y);
                    handler.slots.set(i, newSlot);
                }

                LOGGER.info("Created bento box menu with {} slots", BENTO_SIZE);
                return handler;
            }
        });

        return InteractionResultHolder.success(stack);
    }

    private void loadItemsFromNbt(ItemStack bentoBox, SimpleContainer inventory) {
        if (!bentoBox.hasTag() || !bentoBox.getTag().contains(ITEMS_TAG)) {
            return;
        }

        ListTag nbtList = bentoBox.getTag().getList(ITEMS_TAG, 10);
        for (int i = 0; i < nbtList.size(); i++) {
            CompoundTag itemTag = nbtList.getCompound(i);
            int slot = itemTag.getInt("Slot");

            if (slot >= 0 && slot < BENTO_SIZE) {
                ItemStack loadedStack = ItemStack.of(itemTag);
                inventory.setItem(slot, loadedStack);
            }
        }
    }

    private void saveItemsToNbt(ItemStack bentoBox, SimpleContainer inventory) {
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

        CompoundTag nbt = bentoBox.getOrCreateTag();
        nbt.put(ITEMS_TAG, nbtList);
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

        int foodCount = 0;
        if (stack.hasTag() && stack.getTag().contains(ITEMS_TAG)) {
            ListTag nbtList = stack.getTag().getList(ITEMS_TAG, 10);
            foodCount = nbtList.size();
        }

        tooltip.add(Component.literal("§6Food Items: §f" + foodCount + "/" + BENTO_SIZE));
        tooltip.add(Component.literal("§7Right-click to open"));

        if (foodCount > 0) {
            tooltip.add(Component.literal("§8Contains food items"));
        }
    }

    private static class FoodOnlySlot extends Slot {
        public FoodOnlySlot(SimpleContainer container, int slot, int x, int y) {
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