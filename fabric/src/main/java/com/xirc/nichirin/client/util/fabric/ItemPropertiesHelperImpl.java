package com.xirc.nichirin.client.util.fabric;

import com.xirc.nichirin.common.item.tool.BentoBoxItem;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.resources.ResourceLocation;

public class ItemPropertiesHelperImpl {

    public static void registerBentoBoxProperty() {
        FabricModelPredicateProviderRegistry.register(
                NichirinItemRegistry.BENTO_BOX.get(),
                new ResourceLocation("nichirin", "filled"),
                BentoBoxItem.getFilledPropertyFunction()
        );
    }
}