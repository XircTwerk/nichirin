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

    public static void registerBentoBoxProperty() {
        registerProperty();
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
                    (stack, level, entity, seed) -> {
                        if (!(stack.getItem() instanceof BentoBoxItem)) {
                            return 0.0f;
                        }
                        int foodCount = BentoBoxItem.getFoodCount(stack);
                        return foodCount > 0 ? 1.0f : 0.0f;
                    }
            );
            System.out.println("BENTO BOX PROPERTY REGISTERED!");
        } catch (Exception e) {
            System.out.println("FAILED TO REGISTER BENTO BOX PROPERTY: " + e.getMessage());
            e.printStackTrace();
        }
    }
}