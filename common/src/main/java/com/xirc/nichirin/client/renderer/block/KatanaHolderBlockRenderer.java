package com.xirc.nichirin.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.xirc.nichirin.common.blocks.KatanaHolderBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class KatanaHolderBlockRenderer implements BlockEntityRenderer<KatanaHolderBlock.KatanaHolderBlockEntity> {

    public KatanaHolderBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(KatanaHolderBlock.KatanaHolderBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        renderBlockModel(blockEntity, poseStack, bufferSource, packedLight, packedOverlay);

        if (blockEntity.shouldRenderKatana()) {
            renderKatana(poseStack, blockEntity.getStoredKatana(), blockEntity.getFacing(),
                    blockEntity.isRotated(), bufferSource, packedLight);
        }
    }

    private void renderBlockModel(KatanaHolderBlock.KatanaHolderBlockEntity blockEntity, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        Minecraft mc = Minecraft.getInstance();
        ModelResourceLocation modelLocation = new ModelResourceLocation(
                ResourceLocation.fromNamespaceAndPath("nichirin", "katana_holder_block"), "facing=up,rotated=false");
        BakedModel model = mc.getModelManager().getModel(modelLocation);

        if (model == null || model == mc.getModelManager().getMissingModel()) {
            return;
        }

        Direction facing = blockEntity.getFacing();
        boolean isRotated = blockEntity.isRotated();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        // Apply rotations based on facing and rotation state
        if (isRotated) {
            switch (facing) {
                case UP -> {
                    poseStack.mulPose(Axis.XP.rotationDegrees(0));
                    poseStack.mulPose(Axis.YP.rotationDegrees(0));
                }
                case DOWN -> {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(180));
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
            switch (facing) {
                case UP -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                case DOWN -> {
                    // Flip the model upside down so it mounts on the ceiling properly
                    poseStack.mulPose(Axis.ZP.rotationDegrees(180));
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                }
                case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
                case WEST -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
            }
        }

        poseStack.translate(-0.5, -0.5, -0.5);

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.solid());
        RandomSource random = RandomSource.create(42L);

        for (Direction dir : Direction.values()) {
            List<BakedQuad> quads = model.getQuads(blockEntity.getBlockState(), dir, random);
            for (BakedQuad quad : quads) {
                buffer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, 1.0f, packedLight, packedOverlay);
            }
        }

        List<BakedQuad> generalQuads = model.getQuads(blockEntity.getBlockState(), null, random);
        for (BakedQuad quad : generalQuads) {
            buffer.putBulkData(poseStack.last(), quad, 1.0f, 1.0f, 1.0f, 1.0f, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    private void renderKatana(PoseStack poseStack, ItemStack katana, Direction facing, boolean isRotated,
                              MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        if (isRotated) {
            switch (facing) {
                case UP   -> { /* identity — matches block model rotated-UP */ }
                case DOWN -> poseStack.mulPose(Axis.ZP.rotationDegrees(180));
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
            switch (facing) {
                case UP    -> poseStack.mulPose(Axis.YP.rotationDegrees(90));
                case DOWN  -> {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(180));
                    poseStack.mulPose(Axis.YP.rotationDegrees(-90));
                }
                case NORTH -> poseStack.mulPose(Axis.XP.rotationDegrees(-90));
                case SOUTH -> poseStack.mulPose(Axis.XP.rotationDegrees(90));
                case WEST  -> poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                case EAST  -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
            }
        }

        poseStack.translate(0.12, -0.1, 0.028);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45));

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        itemRenderer.renderStatic(katana, ItemDisplayContext.FIXED, packedLight,
                OverlayTexture.NO_OVERLAY, poseStack, bufferSource, null, 0);

        poseStack.popPose();
    }
}