package com.xirc.nichirin.common.item.food;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;

import java.util.List;

/**
 * Mochi item with an effect tooltip.
 */
public class MochiItem extends Item {

    private final String effectDescription;

    public MochiItem(Properties properties, String effectDescription) {
        super(properties);
        this.effectDescription = effectDescription;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, context, tooltipComponents, isAdvanced);
        tooltipComponents.add(
                Component.literal("Effect: " + effectDescription)
                        .withStyle(style -> style.withColor(0xAAAAAA).withItalic(true))
        );
    }
}