package com.xirc.nichirin.client.renderer.item;

import com.xirc.nichirin.registry.NichirinItemRegistry;
import mod.azure.azurelib.common.animation.cache.AzIdentityRegistry;
import mod.azure.azurelib.common.render.item.AzItemRendererRegistry;
import net.minecraft.world.item.Item;

public class KatanaRendererManager {

    public static void registerAll() {
        registerKatana(NichirinItemRegistry.KATANA.get(), "katana", "katana");
        registerKatana(NichirinItemRegistry.SABITO_KATANA.get(), "sabito_katana", "sabito_katana");
        registerKatana(NichirinItemRegistry.MIST_KATANA.get(), "muichiro_katana", "mist_katana");
        registerKatana(NichirinItemRegistry.THUNDER_KATANA.get(), "thunder_katana", "thunder_katana");
        registerKatana(NichirinItemRegistry.UROKODAKI_KATANA.get(), "urokodaki_katana", "urokodaki_katana");
        registerKatana(NichirinItemRegistry.FLAME_KATANA.get(), "flame_katana", "flame_katana");
        registerKatana(NichirinItemRegistry.INSECT_KATANA.get(), "insect_katana", "insect_katana");
        registerKatana(NichirinItemRegistry.GIYU_KATANA.get(), "giyu_katana", "giyu_katana");
        registerGun(NichirinItemRegistry.GENYA_DB.get(), "genya_db", "genya_db/genya_db");
        // The axe is a conventional held item (MC attaches it to the hand with proper bob/swing) — 3D geo
        // in hand/world, but a flat 2D sprite as the inventory icon. The sim renders the chain + flail.
        registerChainBallAxe(NichirinItemRegistry.CHAIN_BALL_AXE_WEAPON.get(), "chain_ball_axe_axe", "chain_ball_axe_axe", "chain_ball_axe_icon");
    }

    private static void registerChainBallAxe(Item item, String geoName, String textureName, String iconName) {
        AzItemRendererRegistry.register(item, () -> ChainBallAxeItemRenderer.create(geoName, textureName, iconName));
        AzIdentityRegistry.register(item);
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