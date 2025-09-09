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

        // Block rendering with manual rotations
        Direction facing = animatable.getFacing();
        boolean isRotated = animatable.isRotated();
        poseStack.pushPose();

        poseStack.translate(0.5, 0.5, 0.5);

        if (isRotated) {
            // Rotated block orientations
            switch (facing) {
                case UP -> {
                    poseStack.translate(0.5625, -0.5, 0);
                    poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                }
                case DOWN -> {
                    poseStack.translate(-0.435, 0.5, 0);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                }
                case NORTH -> {
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                }
                case SOUTH -> {
                    poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                }
                case WEST -> {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                }
                case EAST -> {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                }
            }
        } else {
            // Normal block orientations
            switch (facing) {
                case UP -> {
                    poseStack.translate(0, -0.5, 0.5625);
                    poseStack.mulPose(Axis.XP.rotationDegrees(90));
                    poseStack.mulPose(Axis.YP.rotationDegrees(180));
                }
                case DOWN -> {
                    poseStack.translate(0, 0.5, -0.435);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                }
                case NORTH -> {
                    poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                }
                case SOUTH -> {
                    poseStack.mulPose(Axis.XP.rotationDegrees(90));
                }
                case WEST -> {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                }
                case EAST -> {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
                }
            }
        }

        poseStack.translate(-0.5, -0.5, -0.5);

        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        poseStack.popPose();

        // Katana rendering with manual rotations
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

        if (isRotated) {
            // Rotated katana positions
            switch (facing) {
                case UP -> {
                    poseStack.translate(0.03, -0.075, -0.15);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-45));
                    poseStack.mulPose(Axis.YP.rotationDegrees(90));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(0));
                }
                case DOWN -> {
                    poseStack.translate(-0.03, 0.075, 0.03);
                    poseStack.mulPose(Axis.XP.rotationDegrees(135));
                    poseStack.mulPose(Axis.YP.rotationDegrees(270));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                }
                case NORTH -> {
                    poseStack.translate(-0.025, -0.15, 0.05);
                    poseStack.mulPose(Axis.XP.rotationDegrees(-315));
                    poseStack.mulPose(Axis.YP.rotationDegrees(90));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-180));
                }
                case SOUTH -> {
                    poseStack.translate(0.025, -0.15, -0.05);
                    poseStack.mulPose(Axis.XP.rotationDegrees(315));
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(180));
                }
                case WEST -> {
                    poseStack.translate(0.025, -0.15, -0.05);
                    poseStack.mulPose(Axis.XP.rotationDegrees(225));
                    poseStack.mulPose(Axis.YP.rotationDegrees(-45));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(180));
                }
                case EAST -> {
                    poseStack.translate(0.025, -0.15, -0.05);
                    poseStack.mulPose(Axis.XP.rotationDegrees(315));
                    poseStack.mulPose(Axis.YP.rotationDegrees(90));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(-180));
                }
            }
        } else {
            // Normal katana positions (your original code)
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