package com.xirc.nichirin.client.renderer.entity.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xirc.nichirin.common.entity.projectile.ThrownKatanaEntity;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class ThrownKatanaRenderer extends EntityRenderer<ThrownKatanaEntity> {

    public ThrownKatanaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownKatanaEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(ThrownKatanaEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        ItemStack stack = entity.getThrownItem();
        if (stack.isEmpty()) stack = new ItemStack(NichirinItemRegistry.KATANA.get());

        poseStack.pushPose();

        // Align with travel direction (taken from old AzureLib renderer)
        float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F - xRot));

        // Spin while in flight, freeze when stuck
        if (!entity.isStuck()) {
            float spinAngle = (entity.tickCount + partialTick) * 36.0F;
            poseStack.mulPose(Axis.YP.rotationDegrees(spinAngle));
        }
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.NONE,
                packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, entity.level(), entity.getId()
        );

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}
