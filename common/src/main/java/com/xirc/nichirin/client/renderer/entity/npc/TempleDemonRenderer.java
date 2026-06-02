package com.xirc.nichirin.client.renderer.entity.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.renderer.entity.BaseAZNichirinEntityRenderer;
import com.xirc.nichirin.client.renderer.entity.animator.TempleDemonAnimator;
import com.xirc.nichirin.common.entity.npc.TempleDemonEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class TempleDemonRenderer extends BaseAZNichirinEntityRenderer<TempleDemonEntity> {

    private static final ResourceLocation GEO = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "geo/temple_demon.geo.json");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/entity/npc/temple_demon.png");

    public TempleDemonRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<TempleDemonEntity>builder(GEO, TEX)
                        .setAnimatorProvider(TempleDemonAnimator::new)
                        .build(),
                context,
                TEX
        );
    }

    @Override
    public void render(TempleDemonEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();

        float scale = entity.getRenderScale();
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }
}