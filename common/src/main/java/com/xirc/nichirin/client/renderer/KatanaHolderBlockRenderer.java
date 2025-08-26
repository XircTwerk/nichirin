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

        // Apply rotation based on facing direction for the block model
        Direction facing = animatable.getFacing();
        poseStack.pushPose();

        // Center the model for rotation
        poseStack.translate(0.5, 0.5, 0.5);

        // Apply rotation based on facing direction
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

        poseStack.translate(-0.5, -0.5, -0.5);

        // Call super to render the block model
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);

        poseStack.popPose();

        // Render katana using dirty flag system
        if (!isReRender && animatable.getLevel() != null && animatable.getLevel().isClientSide()) {
            // Check if we should render using the dirty flag system
            boolean shouldRender = animatable.shouldRenderKatana();
            ItemStack katana = animatable.getStoredKatana();

            System.out.println("RENDER at " + animatable.getBlockPos() + ":");
            System.out.println("  KATANA: " + (katana.isEmpty() ? "EMPTY" : katana.getDisplayName().getString()));
            System.out.println("  SHOULD RENDER: " + shouldRender);

            if (shouldRender) {
                System.out.println("  DECISION: RENDERING KATANA");
                renderKatana(poseStack, katana, facing, bufferSource, packedLight);
            } else {
                System.out.println("  DECISION: NOT RENDERING (dirty flag or empty)");
                // Render nothing - katana disappears
            }
        }
    }

    private void renderKatana(PoseStack poseStack, ItemStack katana, Direction facing,
                              MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Center to block
        poseStack.translate(0.5, 0.5, 0.5);

        // Position and orient katana based on facing direction
        switch (facing) {
            case UP -> {
                poseStack.translate(0.05, -0.075, 0.03);
                poseStack.mulPose(Axis.XP.rotationDegrees(0));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-45));
            }
            case DOWN -> {
                poseStack.translate(-0.05, 0.075, 0.03);
                poseStack.mulPose(Axis.XP.rotationDegrees(180));
                poseStack.mulPose(Axis.ZP.rotationDegrees(45));
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }
            case NORTH -> {
                poseStack.translate(-0.05, 0.025, 0.125);
                poseStack.mulPose(Axis.XP.rotationDegrees(-270));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-225));
                poseStack.mulPose(Axis.YP.rotationDegrees(0));
            }
            case SOUTH -> {
                poseStack.translate(0.05, 0.025, -0.125);
                poseStack.mulPose(Axis.XP.rotationDegrees(270));
                poseStack.mulPose(Axis.ZP.rotationDegrees(225));
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
            }
            case WEST -> {
                poseStack.translate(0.1, 0.025, 0.05);
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(45));
            }
            case EAST -> {
                poseStack.translate(-0.1, 0.025, -0.05);
                poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-45));
                poseStack.mulPose(Axis.YP.rotationDegrees(-180));
            }
        }

        // Normal scale - no tricks needed with dirty flag system
        float scale = 0.8f;
        poseStack.scale(scale, scale, scale);

        // Render the katana item
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        itemRenderer.renderStatic(katana, ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, bufferSource,
                null, 0);

        poseStack.popPose();
    }
}