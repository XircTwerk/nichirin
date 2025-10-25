package com.xirc.nichirin.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.animator.entity.ThunderBallAnimator;
import com.xirc.nichirin.common.entity.ThunderBallEntity;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * The renderer for {@link ThunderBallEntity}.
 */
public class ThunderBallRenderer extends AzEntityRenderer<ThunderBallEntity> {

    private static final ResourceLocation GEO = new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/thunder_ball.geo.json");
    private static final ResourceLocation TEX = new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/entity/thunder_ball.png");

    public ThunderBallRenderer(final EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<ThunderBallEntity>builder(GEO, TEX)
                        .setAnimatorProvider(ThunderBallAnimator::new)
                        .build(),
                context
        );
    }

    @Override
    public ResourceLocation getTextureLocation(ThunderBallEntity entity) {
        return TEX;
    }

    @Override
    public void render(ThunderBallEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Scale the model to 2 times bigger
        poseStack.pushPose();
        poseStack.scale(2.0f, 2.0f, 2.0f);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }
}