package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import mod.azure.azurelib.render.armor.AzArmorRenderer;
import mod.azure.azurelib.render.armor.AzArmorRendererConfig;
import mod.azure.azurelib.render.armor.AzArmorRendererRegistry;
import mod.azure.azurelib.animation.cache.AzIdentityRegistry;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public class ArmorRendererManager {

    // Cache renderer instances to prevent constant recreation
    private static final Map<Item, AzArmorRenderer> RENDERER_CACHE = new HashMap<>();

    /**
     * Register all armor renderers and identities.
     * Call this once during client initialization.
     */
    public static void registerAll() {
        // Register Shinobu armor
        registerArmor(NichirinItemRegistry.SHINOBU_HEADPIECE.get(), ShinobuUniformRenderer::new);
        registerArmor(NichirinItemRegistry.SHINOBU_CAPE.get(), ShinobuCapeRenderer::new);
        registerArmor(NichirinItemRegistry.SHINOBU_LEGGINGS.get(), ShinobuUniformRenderer::new);
        registerArmor(NichirinItemRegistry.SHINOBU_BOOTS.get(), ShinobuUniformRenderer::new);

        // Register Zenitsu armor
        registerArmor(NichirinItemRegistry.ZENITSU_HEADPIECE.get(), ZenitsuUniformRenderer::new);
        registerArmor(NichirinItemRegistry.ZENITSU_CAPE.get(), ZenitsuCapeRenderer::new);
        registerArmor(NichirinItemRegistry.ZENITSU_LEGGINGS.get(), ZenitsuUniformRenderer::new);
        registerArmor(NichirinItemRegistry.ZENITSU_BOOTS.get(), ZenitsuUniformRenderer::new);

        // Register Rengoku armor
        registerArmor(NichirinItemRegistry.RENGOKU_HEADPIECE.get(), RengokuUniformRenderer::new);
        registerArmor(NichirinItemRegistry.RENGOKU_CAPE.get(), RengokuCapeRenderer::new);
        registerArmor(NichirinItemRegistry.RENGOKU_LEGGINGS.get(), RengokuUniformRenderer::new);
        registerArmor(NichirinItemRegistry.RENGOKU_BOOTS.get(), RengokuUniformRenderer::new);

        // Register Tengen armor
        registerArmor(NichirinItemRegistry.TENGEN_HEADPIECE.get(), TengenUniformRenderer::new);
        registerArmor(NichirinItemRegistry.TENGEN_ACCESSORIES.get(), TengenAccessoriesRenderer::new);
        registerArmor(NichirinItemRegistry.TENGEN_LEGGINGS.get(), TengenUniformRenderer::new);
        registerArmor(NichirinItemRegistry.TENGEN_BOOTS.get(), TengenUniformRenderer::new);

        // Register Sabito armor
        registerArmor(NichirinItemRegistry.SABITO_HEADPIECE.get(), SabitoUniformRenderer::new);
        registerArmor(NichirinItemRegistry.SABITO_CAPE.get(), SabitoCapeRenderer::new);
        registerArmor(NichirinItemRegistry.SABITO_LEGGINGS.get(), SabitoUniformRenderer::new);
        registerArmor(NichirinItemRegistry.SABITO_BOOTS.get(), SabitoUniformRenderer::new);
    }

    /**
     * Register a single armor piece with its renderer
     */
    private static void registerArmor(Item item, java.util.function.Supplier<AzArmorRenderer> rendererSupplier) {
        // Register with AzureLib
        AzArmorRendererRegistry.register(item, rendererSupplier);

        // Register identity
        AzIdentityRegistry.register(item);
    }

    /**
     * Get or create a renderer for the given armor item (legacy support)
     */
    public static AzArmorRenderer getRendererForItem(Item item) {
        return RENDERER_CACHE.computeIfAbsent(item, ArmorRendererManager::createRendererForItem);
    }

    private static AzArmorRenderer createRendererForItem(Item item) {
        // Shinobu armor
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

        // Zenitsu armor
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

        // Rengoku armor
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

        // Tengen armor
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

        // Sabito armor
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

        // Default fallback
        return new AzArmorRenderer(
                AzArmorRendererConfig.builder(
                        BreathOfNichirin.id("geo/default_armor.geo.json"),
                        BreathOfNichirin.id("textures/armor/default_armor.png")
                ).build()
        );
    }
}