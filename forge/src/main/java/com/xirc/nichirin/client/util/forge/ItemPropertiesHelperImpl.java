package com.xirc.nichirin.client.util.forge;

import com.xirc.nichirin.common.item.tool.BentoBoxItem;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "nichirin", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ItemPropertiesHelperImpl {

    public static void registerBentoBoxProperty() {
        // This gets called from FMLClientSetupEvent, but we'll also register in ModelEvent for safety
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerProperty();
        });
    }

    @SubscribeEvent
    public static void onRegisterAdditional(ModelEvent.RegisterAdditional event) {
        registerProperty();
    }

    private static void registerProperty() {
        try {
            ItemProperties.register(
                    NichirinItemRegistry.BENTO_BOX.get(),
                    new ResourceLocation("nichirin", "filled"),
                    BentoBoxItem.getFilledPropertyFunction()
            );
            System.out.println("BENTO BOX PROPERTY REGISTERED!");
        } catch (Exception e) {
            System.out.println("FAILED TO REGISTER BENTO BOX PROPERTY: " + e.getMessage());
            e.printStackTrace();
        }
    }
}