package com.xirc.nichirin.common.item.katana;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Shared contract for a "katana set" — a holder item that transforms into a left/right pair of
 * individually wielded katanas (Sound, Beast, …). Implemented by both {@link AbstractDualKatana}
 * (the holder) and {@link AbstractIndividualKatana} (the wielded halves) so the item-identity
 * logic lives in one place even though the two can't share a class parent ({@code Item} vs
 * {@link Katana}).
 */
public interface KatanaSet {

    /** The single-stack holder item (e.g. sound_katanas / beast_katanas). */
    Item holderItem();

    /** The right-hand individual katana. */
    Item rightItem();

    /** The left-hand individual katana. */
    Item leftItem();

    default boolean isHolder(ItemStack stack) {
        return stack.getItem() == holderItem();
    }

    default boolean isRight(ItemStack stack) {
        return stack.getItem() == rightItem();
    }

    default boolean isLeft(ItemStack stack) {
        return stack.getItem() == leftItem();
    }

    /** True if the stack is either half of the individual pair. */
    default boolean isIndividual(ItemStack stack) {
        return isRight(stack) || isLeft(stack);
    }
}
