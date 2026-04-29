package com.xirc.nichirin.client.renderer.item;

import com.xirc.nichirin.BreathOfNichirin;
import mod.azure.azurelib.render.item.AzItemRenderer;
import mod.azure.azurelib.render.item.AzItemRendererConfig;
import net.minecraft.resources.ResourceLocation;

public class BeastKatanaItemRenderer extends AzItemRenderer {

    private static final ResourceLocation GEO = new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/katana_inosuke.geo.json");
    private static final ResourceLocation TEX = new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/item/inosuke_katana.png");

    public BeastKatanaItemRenderer() {
        super(AzItemRendererConfig.builder(GEO, TEX).build());
    }
}
