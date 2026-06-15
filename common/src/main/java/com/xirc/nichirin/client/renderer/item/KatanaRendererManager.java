package com.xirc.nichirin.client.renderer.item;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import mod.azure.azurelib.common.animation.cache.AzIdentityRegistry;
import mod.azure.azurelib.common.render.item.AzItemRendererRegistry;
import net.minecraft.world.item.Item;

public class KatanaRendererManager {

    public static void registerAll() {
        registerKatana(NichirinItemRegistry.SABITO_KATANA.get(), "sabito_katana", "sabito_katana");
        registerKatana(NichirinItemRegistry.MIST_KATANA.get(), "muichiro_katana", "mist_katana");
        registerKatana(NichirinItemRegistry.FLAME_KATANA.get(), "flame_katana", "flame_katana");
        registerKatana(NichirinItemRegistry.INSECT_KATANA.get(), "insect_katana", "insect_katana");
        registerKatana(NichirinItemRegistry.GIYU_KATANA.get(), "giyu_katana", "giyu_katana");
        registerGun(NichirinItemRegistry.GENYA_DB.get(), "genya_db", "genya_db/genya_db");
    }

    private static void registerGun(Item item, String geoName, String textureName) {
        AzItemRendererRegistry.register(item, () -> GunItemRenderer.create(geoName, textureName));
        AzIdentityRegistry.register(item);
    }

    private static void registerKatana(Item item, String geoName, String textureName) {
        AzItemRendererRegistry.register(item, () -> KatanaItemRenderer.create(geoName, textureName));
        AzIdentityRegistry.register(item);
    }
}