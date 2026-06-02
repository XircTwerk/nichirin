package com.xirc.nichirin.client.util.fabric;

import com.xirc.nichirin.common.item.tool.BentoBoxItem;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

public class ItemPropertiesHelperImpl {

    public static void registerBentoBoxProperty() {
        ItemProperties.register(
                NichirinItemRegistry.BENTO_BOX.get(),
                ResourceLocation.fromNamespaceAndPath("nichirin", "filled"),
                (stack, level, entity, seed) -> {
                    if (!(stack.getItem() instanceof BentoBoxItem)) {
                        return 0.0f;
                    }
                    int foodCount = BentoBoxItem.getFoodCount(stack);
                    return foodCount > 0 ? 1.0f : 0.0f;
                }
        );
    }
}