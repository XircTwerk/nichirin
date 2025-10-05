package com.xirc.nichirin.client.renderer.entity.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.model.entity.npc.TempleDemonModel;
import com.xirc.nichirin.common.entity.npc.TempleDemonEntity;
import mod.azure.azurelib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * GeoEntityRenderer for TempleDemon
 * Note: Since we're using the moveset's animation system through performDemonAttack,
 * we don't need to manually manage animations here - they're handled by the attack system.
 */
public class TempleDemonRenderer extends GeoEntityRenderer<TempleDemonEntity> {

    public TempleDemonRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TempleDemonModel());
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

        // Call parent render method - AzureLib handles the GeoModel animations
        // The entity's animation controller (registered in registerControllers) determines what plays
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(TempleDemonEntity animatable) {
        // You can make this dynamic based on entity.getDemonType() if you have variants
        return new ResourceLocation("nichirin", "textures/entity/npc/temple_demon.png");
    }
}