package com.xirc.nichirin.client.renderer.item;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.item.gun.GenyaDB;
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

/**
 * Item renderer for the gun. Identical to {@link KatanaItemRenderer} except it hides the
 * muzzle-flash / effect bones, which are only meant to appear while firing — otherwise they
 * render on the held/GUI item as huge offset quads.
 */
public class GunItemRenderer extends AzItemRenderer {

    // Effect bones in genya_db.geo.json (large flash/pressure quads).
    private static final String[] EFFECT_BONES = {"fireL", "fireR", "pressure"};
    // Demon-only flesh bone — shown only when a demon holds the gun.
    private static final String FLESH_BONE = "flesh";

    public GunItemRenderer(ResourceLocation geoModel, ResourceLocation texture) {
        super(AzItemRendererConfig.builder(geoModel, texture)
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
                hideEffectBones(context);
            }
        };
    }

    private void hideEffectBones(AzRendererPipelineContext<UUID, ItemStack> context) {
        var model = context.bakedModel();
        if (model == null) return;
        for (String boneName : EFFECT_BONES) {
            Optional<AzBone> bone = model.getBone(boneName);
            bone.ifPresent(b -> b.setHidden(true));
        }

        ItemStack stack = context.animatable();
        boolean demon = stack != null && ItemStackData.get(stack).getBoolean(GenyaDB.DEMON_RENDER_KEY);
        model.getBone(FLESH_BONE).ifPresent(b -> b.setHidden(!demon));
    }

    public static GunItemRenderer create(String geoName, String textureName) {
        return new GunItemRenderer(
                ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "geo/" + geoName + ".geo.json"),
                ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/item/" + textureName + ".png")
        );
    }
}
