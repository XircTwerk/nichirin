package com.xirc.nichirin.client.util.forge;

import com.xirc.nichirin.common.item.tool.BentoBoxItem;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "nichirin", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ItemPropertiesHelperImpl {

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        ItemProperties.register(
                NichirinItemRegistry.BENTO_BOX.get(),
                new ResourceLocation("nichirin", "filled"),
                BentoBoxItem.getFilledPropertyFunction()
        );
    }
}