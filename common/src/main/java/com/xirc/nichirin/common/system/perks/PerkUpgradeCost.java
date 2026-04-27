package com.xirc.nichirin.common.system.perks;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Materials required to upgrade a perk to the next tier.
 */
public class PerkUpgradeCost {

    public final int xpLevels;
    public final List<ItemStack> requiredItems;

    private PerkUpgradeCost(int xpLevels, List<ItemStack> requiredItems) {
        this.xpLevels = xpLevels;
        this.requiredItems = List.copyOf(requiredItems);
    }

    public static Builder builder(int xpLevels) {
        return new Builder(xpLevels);
    }

    public static class Builder {
        private final int xpLevels;
        private final List<ItemStack> items = new ArrayList<>();

        Builder(int xpLevels) { this.xpLevels = xpLevels; }

        public Builder item(Item item, int count) {
            items.add(new ItemStack(item, count));
            return this;
        }

        public PerkUpgradeCost build() {
            return new PerkUpgradeCost(xpLevels, items);
        }
    }
}
