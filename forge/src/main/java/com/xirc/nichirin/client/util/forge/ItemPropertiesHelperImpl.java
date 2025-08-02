package com.xirc.nichirin.client.util.forge;

import com.xirc.nichirin.common.item.tool.BentoBoxItem;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

public class ItemPropertiesHelperImpl {

    public static void registerBentoBoxProperty() {
        ItemProperties.register(
                NichirinItemRegistry.BENTO_BOX.get(),
                new ResourceLocation("nichirin", "filled"),
                BentoBoxItem.getFilledPropertyFunction()
        );
    }
}