package com.xirc.nichirin.client.util.neoforge;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.item.tool.BentoBoxItem;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = "nichirin", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
                    ResourceLocation.fromNamespaceAndPath("nichirin", "filled"),
                    (stack, level, entity, seed) -> {
                        if (!(stack.getItem() instanceof BentoBoxItem)) {
                            return 0.0f;
                        }
                        int foodCount = BentoBoxItem.getFoodCount(stack);
                        return foodCount > 0 ? 1.0f : 0.0f;
                    }
            );
        } catch (Exception e) {
            BreathOfNichirin.LOGGER.error("Failed to register bento box item property", e);
        }
    }
}