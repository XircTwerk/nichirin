package com.xirc.nichirin.common.item.food;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RiceItem extends Item {

    public RiceItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
        tooltipComponents.add(
                Component.literal("Use with a water bucket to make Mochi")
                        .withStyle(style -> style.withColor(0xAAAAAA).withItalic(true))
        );
    }
}
