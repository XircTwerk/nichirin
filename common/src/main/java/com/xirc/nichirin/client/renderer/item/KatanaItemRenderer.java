package com.xirc.nichirin.client.renderer.item;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.util.ItemStackData;
import com.mojang.blaze3d.platform.Lighting;
import mod.azure.azurelib.common.model.AzBone;
import mod.azure.azurelib.common.render.AzRendererPipelineContext;
import mod.azure.azurelib.common.render.item.AzItemRenderer;
import mod.azure.azurelib.common.render.item.AzItemRendererConfig;
import mod.azure.azurelib.common.render.item.AzItemRendererPipeline;
import mod.azure.azurelib.common.render.item.AzItemRendererPipelineContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.UUID;

public class KatanaItemRenderer extends AzItemRenderer {

    private static final String SHEATH_BONE = "Sheath";

    public KatanaItemRenderer(ResourceLocation geoModel, ResourceLocation texture) {
        super(AzItemRendererConfig.builder(geoModel, texture)
                .setModelRenderer((pipeline, layers) ->
                        new KatanaItemModelRenderer((AzItemRendererPipeline) pipeline, layers))
                .build());
    }

    @Override
    protected AzItemRendererPipeline createPipeline(AzItemRendererConfig config) {
        return new AzItemRendererPipeline(config, this) {
            @Override
            public void preRender(AzRendererPipelineContext<UUID, ItemStack> context, boolean isReRender) {
                var itemContext = (AzItemRendererPipelineContext) context;
                if (itemContext.getTransformType() == ItemDisplayContext.GUI) {
                    context.poseStack().translate(0, 0, 0.1);
                }
                super.preRender(context, isReRender);
                if (itemContext.getTransformType() == ItemDisplayContext.GUI) {
                    Lighting.setupFor3DItems();
                }
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
        boolean sheathed = stack != null && ItemStackData.get(stack).getBoolean("nichirin_sheathed_render");
        sheathBone.get().setHidden(!sheathed);
    }

    public static KatanaItemRenderer create(String geoName, String textureName) {
        return new KatanaItemRenderer(
                ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "geo/" + geoName + ".geo.json"),
                ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/item/" + textureName + ".png")
        );
    }
}
