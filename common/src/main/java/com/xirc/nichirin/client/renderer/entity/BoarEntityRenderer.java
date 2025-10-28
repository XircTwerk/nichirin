package com.xirc.nichirin.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.animator.entity.BoarEntityAnimator;
import com.xirc.nichirin.common.entity.BoarEntity;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * The renderer for {@link BoarEntity}.
 */
public class BoarEntityRenderer extends AzEntityRenderer<BoarEntity> {

    private static final ResourceLocation GEO = new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/boar.geo.json");
    private static final ResourceLocation TEX = new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/entity/boar.png");

    public BoarEntityRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<BoarEntity>builder(GEO, TEX)
                        .setAnimatorProvider(BoarEntityAnimator::new)
                        .build(),
                context
        );
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BoarEntity entity) {
        return TEX;
    }

    @Override
    public void render(BoarEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        if (entity.isBaby()) {
            poseStack.scale(0.8f, 0.8f, 0.8f);
        } else {
            poseStack.scale(1.25f, 1.25f, 1.25f);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}