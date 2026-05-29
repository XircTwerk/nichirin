package com.xirc.nichirin.client.renderer.item;

import com.xirc.nichirin.BreathOfNichirin;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.item.AzItemRenderer;
import mod.azure.azurelib.render.item.AzItemRendererConfig;
import mod.azure.azurelib.render.item.AzItemRendererPipeline;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class KatanaItemRenderer extends AzItemRenderer {

    private static final String SHEATH_BONE = "Sheath";

    public KatanaItemRenderer(ResourceLocation geoModel, ResourceLocation texture) {
        super(AzItemRendererConfig.builder(geoModel, texture).build());
    }

    @Override
    protected AzItemRendererPipeline createPipeline(AzItemRendererConfig config) {
        return new AzItemRendererPipeline(config, this) {
            @Override
            public void preRender(AzRendererPipelineContext<UUID, ItemStack> context, boolean isReRender) {
                super.preRender(context, isReRender);
                hideOrShowSheath(context);
            }
        };
    }

    private void hideOrShowSheath(AzRendererPipelineContext<UUID, ItemStack> context) {
        var model = context.bakedModel();
        if (model == null) return;
        Optional<AzBone> sheathBone = model.getBone(SHEATH_BONE);
        if (sheathBone.isEmpty()) return;

        ItemStack stack = context.animatable();
        boolean sheathed = stack != null && stack.hasTag()
                && stack.getTag().getBoolean("nichirin_sheathed_render");
        sheathBone.get().setHidden(!sheathed);
    }

    public static KatanaItemRenderer create(String geoName, String textureName) {
        return new KatanaItemRenderer(
                new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/" + geoName + ".geo.json"),
                new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/item/" + textureName + ".png")
        );
    }
}
