package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.model.NichirinArmorModel;
import com.xirc.nichirin.client.renderer.armor.*;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import mod.azure.azurelibarmor.animatable.client.RenderProvider;
import mod.azure.azurelibarmor.renderer.GeoArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ArmorRendererManager {

    // Cache renderer instances to prevent constant recreation
    private static final Map<Item, GeoArmorRenderer<?>> RENDERER_CACHE = new HashMap<>();

    // Singleton render provider to prevent multiple instances
    private static RenderProvider RENDER_PROVIDER_INSTANCE = null;

    public static RenderProvider createRenderProvider() {
        if (RENDER_PROVIDER_INSTANCE == null) {
            RENDER_PROVIDER_INSTANCE = createNewRenderProvider();
        }
        return RENDER_PROVIDER_INSTANCE;
    }

    private static RenderProvider createNewRenderProvider() {
        return new RenderProvider() {
            @Override
            public HumanoidModel<LivingEntity> getHumanoidArmorModel(
                    LivingEntity entity,
                    ItemStack armorStack,
                    EquipmentSlot slot,
                    HumanoidModel<LivingEntity> baseModel) {

                GeoArmorRenderer<?> renderer = getRendererForArmor(armorStack);
                if (renderer != null) {
                    renderer.prepForRender(entity, armorStack, slot, baseModel);
                    return renderer;
                }

                // Fallback to base model if no specific renderer found
                return baseModel;
            }
        };
    }

    private static GeoArmorRenderer<?> getRendererForArmor(ItemStack armorStack) {
        Item item = armorStack.getItem();

        // Return cached renderer or create new one if not cached
        return RENDERER_CACHE.computeIfAbsent(item, key -> createNewRendererForItem(key));
    }

    private static GeoArmorRenderer<?> createNewRendererForItem(Item item) {
        // Shinobu armor - each piece gets its own renderer instance
        if (item == NichirinItemRegistry.SHINOBU_HEADPIECE.get()) {
            return new ShinobuUniformRenderer();
        }
        if (item == NichirinItemRegistry.SHINOBU_CAPE.get()) {
            return new ShinobuCapeRenderer();
        }
        if (item == NichirinItemRegistry.SHINOBU_LEGGINGS.get()) {
            return new ShinobuUniformRenderer();
        }
        if (item == NichirinItemRegistry.SHINOBU_BOOTS.get()) {
            return new ShinobuUniformRenderer();
        }

        // Zenitsu armor - each piece gets its own renderer instance
        if (item == NichirinItemRegistry.ZENITSU_HEADPIECE.get()) {
            return new ZenitsuUniformRenderer();
        }
        if (item == NichirinItemRegistry.ZENITSU_CAPE.get()) {
            return new ZenitsuCapeRenderer();
        }
        if (item == NichirinItemRegistry.ZENITSU_LEGGINGS.get()) {
            return new ZenitsuUniformRenderer();
        }
        if (item == NichirinItemRegistry.ZENITSU_BOOTS.get()) {
            return new ZenitsuUniformRenderer();
        }

        // Rengoku armor - each piece gets its own renderer instance
        if (item == NichirinItemRegistry.RENGOKU_HEADPIECE.get()) {
            return new RengokuUniformRenderer();
        }
        if (item == NichirinItemRegistry.RENGOKU_CAPE.get()) {
            return new RengokuCapeRenderer();
        }
        if (item == NichirinItemRegistry.RENGOKU_LEGGINGS.get()) {
            return new RengokuUniformRenderer();
        }
        if (item == NichirinItemRegistry.RENGOKU_BOOTS.get()) {
            return new RengokuUniformRenderer();
        }

        // Tengen armor - each piece gets its own renderer instance
        if (item == NichirinItemRegistry.TENGEN_HEADPIECE.get()) {
            return new TengenUniformRenderer();
        }
        if (item == NichirinItemRegistry.TENGEN_ACCESSORIES.get()) {
            return new TengenAccessoriesRenderer();
        }
        if (item == NichirinItemRegistry.TENGEN_LEGGINGS.get()) {
            return new TengenUniformRenderer();
        }
        if (item == NichirinItemRegistry.TENGEN_BOOTS.get()) {
            return new TengenUniformRenderer();
        }

        if (item == NichirinItemRegistry.SABITO_HEADPIECE.get()) {
            return new SabitoUniformRenderer();
        }
        if (item == NichirinItemRegistry.SABITO_CAPE.get()) {
            return new SabitoCapeRenderer();
        }
        if (item == NichirinItemRegistry.SABITO_LEGGINGS.get()) {
            return new SabitoUniformRenderer();
        }
        if (item == NichirinItemRegistry.SABITO_BOOTS.get()) {
            return new SabitoUniformRenderer();
        }

        // Default fallback - using the base renderer
        return new NichirinArmorRenderer<>(new NichirinArmorModel<>("default_armor"));
    }
}