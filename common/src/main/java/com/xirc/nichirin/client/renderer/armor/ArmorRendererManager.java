package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.renderer.armor.*;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import mod.azure.azurelib.animatable.client.RenderProvider;
import mod.azure.azurelib.renderer.GeoArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class ArmorRendererManager {

    public static RenderProvider createRenderProvider() {
        return new RenderProvider() {
            @Override
            public HumanoidModel<LivingEntity> getHumanoidArmorModel(
                    LivingEntity entity,
                    ItemStack armorStack,
                    EquipmentSlot slot,
                    HumanoidModel<LivingEntity> baseModel) {

                GeoArmorRenderer<?> renderer = createRendererForArmor(armorStack);
                if (renderer != null) {
                    renderer.prepForRender(entity, armorStack, slot, baseModel);
                    return renderer;
                }

                // Fallback to base model if no specific renderer found
                return baseModel;
            }
        };
    }

    private static GeoArmorRenderer<?> createRendererForArmor(ItemStack armorStack) {
        // Shinobu armor - each piece gets its own renderer instance
        if (armorStack.is(NichirinItemRegistry.SHINOBU_HEADPIECE.get())) {
            return new ShinobuUniformRenderer();
        }
        if (armorStack.is(NichirinItemRegistry.SHINOBU_CAPE.get())) {
            return new ShinobuCapeRenderer();
        }
        if (armorStack.is(NichirinItemRegistry.SHINOBU_LEGGINGS.get())) {
            return new ShinobuUniformRenderer();
        }
        if (armorStack.is(NichirinItemRegistry.SHINOBU_BOOTS.get())) {
            return new ShinobuUniformRenderer();
        }

        // Zenitsu armor - each piece gets its own renderer instance
        if (armorStack.is(NichirinItemRegistry.ZENITSU_HEADPIECE.get())) {
            return new ZenitsuUniformRenderer();
        }
        if (armorStack.is(NichirinItemRegistry.ZENITSU_CAPE.get())) {
            return new ZenitsuCapeRenderer();
        }
        if (armorStack.is(NichirinItemRegistry.ZENITSU_LEGGINGS.get())) {
            return new ZenitsuUniformRenderer();
        }
        if (armorStack.is(NichirinItemRegistry.ZENITSU_BOOTS.get())) {
            return new ZenitsuUniformRenderer();
        }

        // Rengoku armor - each piece gets its own renderer instance
        if (armorStack.is(NichirinItemRegistry.RENGOKU_HEADPIECE.get())) {
            return new RengokuUniformRenderer();
        }
        if (armorStack.is(NichirinItemRegistry.RENGOKU_CAPE.get())) {
            return new RengokuCapeRenderer();
        }
        if (armorStack.is(NichirinItemRegistry.RENGOKU_LEGGINGS.get())) {
            return new RengokuUniformRenderer();
        }
        if (armorStack.is(NichirinItemRegistry.RENGOKU_BOOTS.get())) {
            return new RengokuUniformRenderer();
        }

        // Tengen armor - each piece gets its own renderer instance
        if (armorStack.is(NichirinItemRegistry.TENGEN_HEADPIECE.get())) {
            return new TengenUniformRenderer();
        }
        if (armorStack.is(NichirinItemRegistry.TENGEN_ACCESSORIES.get())) {
            return new TengenAccessoriesRenderer();
        }
        if (armorStack.is(NichirinItemRegistry.TENGEN_LEGGINGS.get())) {
            return new TengenUniformRenderer();
        }
        if (armorStack.is(NichirinItemRegistry.TENGEN_BOOTS.get())) {
            return new TengenUniformRenderer();
        }

        // Default fallback - using the base renderer
        return new NichirinArmorRenderer<>(new com.xirc.nichirin.client.model.NichirinArmorModel<>("default_armor"));
    }
}