package com.xirc.nichirin.client.renderer.entity.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.npc.TempleDemonEntity;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * AzEntityRenderer for TempleDemon without animations (static pose)
 */
public class TempleDemonRenderer extends AzEntityRenderer<TempleDemonEntity> {

    private static final ResourceLocation GEO = new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/temple_demon.geo.json");
    private static final ResourceLocation TEX = new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/entity/npc/temple_demon.png");

    public TempleDemonRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<TempleDemonEntity>builder(GEO, TEX)
                        .setAnimatorProvider(() -> null) // No animations
                        .build(),
                context
        );
    }

    @Override
    public void render(TempleDemonEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();

        // Apply custom scaling
        float scale = entity.getRenderScale();
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }

        // Call parent render
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(TempleDemonEntity entity) {
        return TEX;
    }
}