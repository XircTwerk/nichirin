package com.xirc.nichirin.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xirc.nichirin.client.model.KatanaHolderBlockModel;
import com.xirc.nichirin.common.blocks.KatanaHolderBlock;
import mod.azure.azurelib.cache.object.BakedGeoModel;
import mod.azure.azurelib.renderer.GeoBlockRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class KatanaHolderBlockRenderer extends GeoBlockRenderer<KatanaHolderBlock.KatanaHolderBlockEntity> {

    public KatanaHolderBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new KatanaHolderBlockModel());
    }

    @Override
    public void actuallyRender(PoseStack poseStack, KatanaHolderBlock.KatanaHolderBlockEntity animatable,
                               BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource,
                               VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {

        // YOUR ORIGINAL BLOCK RENDERING + simple 90 degree rotation
        Direction facing = animatable.getFacing();
        boolean isRotated = animatable.isRotated();
        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.5);

        switch (facing) {
            case UP -> {
                poseStack.translate(0, -0.5, 0.5625);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                // UP: rotate around Z-axis, keep same position when rotated
                if (isRotated) {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                }
            }
            case DOWN -> {
                poseStack.translate(0, 0.5, -0.435);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                // DOWN: rotate around Z-axis, keep same position when rotated
                if (isRotated) {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                }
            }
            case NORTH -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                // Walls: Y-axis rotation
                if (isRotated) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                }
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                // Walls: Y-axis rotation
                if (isRotated) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                }
            }
            case WEST -> {
                poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                // Walls: Y-axis rotation
                if (isRotated) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                }
            }
            case EAST -> {
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
                // Walls: Y-axis rotation
                if (isRotated) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                }
            }
        }

        poseStack.translate(-0.5, -0.5, -0.5);

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        poseStack.popPose();

        // YOUR ORIGINAL KATANA RENDERING
        if (!isReRender && animatable.getLevel() != null && animatable.getLevel().isClientSide()) {
            boolean shouldRender = animatable.shouldRenderKatana();
            ItemStack katana = animatable.getStoredKatana();

            if (shouldRender) {
                renderKatana(poseStack, katana, facing, isRotated, bufferSource, packedLight);
            }
        }
    }

    private void renderKatana(PoseStack poseStack, ItemStack katana, Direction facing, boolean isRotated,
                              MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.5);

        // YOUR ORIGINAL KATANA POSITIONING
        switch (facing) {
            case UP -> {
                poseStack.translate(0.15, -0.075, 0.03);
                poseStack.mulPose(Axis.XP.rotationDegrees(0));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-45));
            }
            case DOWN -> {
                poseStack.translate(-0.15, 0.075, 0.03);
                poseStack.mulPose(Axis.XP.rotationDegrees(180));
                poseStack.mulPose(Axis.ZP.rotationDegrees(45));
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }
            case NORTH -> {
                poseStack.translate(-0.15, 0.025, 0.125);
                poseStack.mulPose(Axis.XP.rotationDegrees(-270));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-225));
                poseStack.mulPose(Axis.YP.rotationDegrees(0));
            }
            case SOUTH -> {
                poseStack.translate(0.15, 0.025, -0.125);
                poseStack.mulPose(Axis.XP.rotationDegrees(270));
                poseStack.mulPose(Axis.ZP.rotationDegrees(225));
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }
            case WEST -> {
                poseStack.translate(0.1, 0.025, 0.15);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(45));
            }
            case EAST -> {
                poseStack.translate(-0.1, 0.025, -0.15);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-45));
                poseStack.mulPose(Axis.YP.rotationDegrees(-180));
            }
        }

        // Add 90 degree rotation if rotated
        if (isRotated) {
            poseStack.mulPose(Axis.YP.rotationDegrees(90));
        }

        float scale = 1f;
        poseStack.scale(scale, scale, scale);

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        itemRenderer.renderStatic(katana, ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, bufferSource,
                null, 0);

        poseStack.popPose();
    }
}