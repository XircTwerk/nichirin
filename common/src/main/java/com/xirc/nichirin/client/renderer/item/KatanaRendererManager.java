package com.xirc.nichirin.client.renderer.item;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import mod.azure.azurelib.animation.cache.AzIdentityRegistry;
import mod.azure.azurelib.render.item.AzItemRendererRegistry;
import net.minecraft.world.item.Item;

public class KatanaRendererManager {

    public static void registerAll() {
        registerKatana(NichirinItemRegistry.SABITO_KATANA.get(), "katana_sabito", "sabito_katana");
        registerKatana(NichirinItemRegistry.MIST_KATANA.get(), "muichiro_katana", "mist_katana");
    }

    private static void registerKatana(Item item, String geoName, String textureName) {
        AzItemRendererRegistry.register(item, () -> KatanaItemRenderer.create(geoName, textureName));
        AzIdentityRegistry.register(item);
    }
}
