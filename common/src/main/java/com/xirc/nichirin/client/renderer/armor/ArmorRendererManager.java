package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import mod.azure.azurelib.animation.cache.AzIdentityRegistry;
import mod.azure.azurelib.render.armor.AzArmorRenderer;
import mod.azure.azurelib.render.armor.AzArmorRendererRegistry;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public class ArmorRendererManager {

    /**
     * Register all armor renderers and identities.
     * Call this once during client initialization.
     */
    public static void registerAll() {
        // Shinobu armor
        registerArmor(NichirinItemRegistry.SHINOBU_HEADPIECE.get(), ShinobuUniformRenderer::new);
        registerArmor(NichirinItemRegistry.SHINOBU_CAPE.get(), ShinobuCapeRenderer::new);
        registerArmor(NichirinItemRegistry.SHINOBU_LEGGINGS.get(), ShinobuUniformRenderer::new);
        registerArmor(NichirinItemRegistry.SHINOBU_BOOTS.get(), ShinobuUniformRenderer::new);

        // Zenitsu armor
        registerArmor(NichirinItemRegistry.ZENITSU_HEADPIECE.get(), ZenitsuUniformRenderer::new);
        registerArmor(NichirinItemRegistry.ZENITSU_CAPE.get(), ZenitsuCapeRenderer::new);
        registerArmor(NichirinItemRegistry.ZENITSU_LEGGINGS.get(), ZenitsuUniformRenderer::new);
        registerArmor(NichirinItemRegistry.ZENITSU_BOOTS.get(), ZenitsuUniformRenderer::new);

        // Rengoku armor
        registerArmor(NichirinItemRegistry.RENGOKU_HEADPIECE.get(), RengokuUniformRenderer::new);
        registerArmor(NichirinItemRegistry.RENGOKU_CAPE.get(), RengokuCapeRenderer::new);
        registerArmor(NichirinItemRegistry.RENGOKU_LEGGINGS.get(), RengokuUniformRenderer::new);
        registerArmor(NichirinItemRegistry.RENGOKU_BOOTS.get(), RengokuUniformRenderer::new);

        // Tengen armor
        registerArmor(NichirinItemRegistry.TENGEN_HEADPIECE.get(), TengenUniformRenderer::new);
        registerArmor(NichirinItemRegistry.TENGEN_ACCESSORIES.get(), TengenAccessoriesRenderer::new);
        registerArmor(NichirinItemRegistry.TENGEN_LEGGINGS.get(), TengenUniformRenderer::new);
        registerArmor(NichirinItemRegistry.TENGEN_BOOTS.get(), TengenUniformRenderer::new);

        // Sabito armor
        registerArmor(NichirinItemRegistry.SABITO_HEADPIECE.get(), SabitoUniformRenderer::new);
        registerArmor(NichirinItemRegistry.SABITO_CAPE.get(), SabitoCapeRenderer::new);
        registerArmor(NichirinItemRegistry.SABITO_LEGGINGS.get(), SabitoUniformRenderer::new);
        registerArmor(NichirinItemRegistry.SABITO_BOOTS.get(), SabitoUniformRenderer::new);

        // Giyu armor
        registerArmor(NichirinItemRegistry.GIYU_HEADPIECE.get(), GiyuUniformRenderer::new);
        registerArmor(NichirinItemRegistry.GIYU_CAPE.get(), GiyuCapeRenderer::new);
        registerArmor(NichirinItemRegistry.GIYU_LEGGINGS.get(), GiyuUniformRenderer::new);
        registerArmor(NichirinItemRegistry.GIYU_BOOTS.get(), GiyuUniformRenderer::new);

        // Urokodaki armor
        registerArmor(NichirinItemRegistry.UROKODAKI_HEADPIECE.get(), UrokodakiUniformRenderer::new);
        registerArmor(NichirinItemRegistry.UROKODAKI_CAPE.get(), UrokodakiCapeRenderer::new);
        registerArmor(NichirinItemRegistry.UROKODAKI_LEGGINGS.get(), UrokodakiUniformRenderer::new);
        registerArmor(NichirinItemRegistry.UROKODAKI_BOOTS.get(), UrokodakiUniformRenderer::new);

        // Inosuke armor
        registerArmor(NichirinItemRegistry.BOAR_HEAD.get(), InosukeUniformRenderer::new);
        registerArmor(NichirinItemRegistry.INOSUKE_LEGGINGS.get(), InosukeUniformRenderer::new);
        registerArmor(NichirinItemRegistry.INOSUKE_BOOTS.get(), InosukeUniformRenderer::new);

    }

    private static void registerArmor(Item item, Supplier<AzArmorRenderer> rendererSupplier) {
        AzArmorRendererRegistry.register(item, rendererSupplier);
        AzIdentityRegistry.register(item);
    }
}
