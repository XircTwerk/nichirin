package com.xirc.nichirin.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xirc.nichirin.BreathOfNichirin;
import mod.azure.azurelib.render.item.AzItemRenderer;
import mod.azure.azurelib.render.item.AzItemRendererConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class BeastKatanaItemRenderer extends AzItemRenderer {

    private static final ResourceLocation GEO = new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/katana_inosuke.geo.json");
    private static final ResourceLocation TEX = new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/item/inosuke_katana.png");

    public BeastKatanaItemRenderer() {
        super(AzItemRendererConfig.builder(GEO, TEX).build());
    }

    @Override
    public void renderByGui(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                            MultiBufferSource source, int packedLight) {
        poseStack.pushPose();
        // AzItemRendererPipeline.preRender adds translate(0.5, 0.51, 0.5) internally.
        // Cancel it so our explicit GUI transforms align with the other katanas.
        poseStack.translate(-0.5, -0.51, -0.5);
        poseStack.mulPose(Axis.XP.rotationDegrees(90));
        poseStack.mulPose(Axis.YP.rotationDegrees(45));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
        poseStack.translate(-6.75f / 16f, -4.5f / 16f, 0);
        poseStack.scale(0.8f, 0.8f, 0.8f);
        super.renderByGui(stack, transformType, poseStack, source, packedLight);
        poseStack.popPose();
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                             MultiBufferSource source, int packedLight) {
        poseStack.pushPose();
        switch (transformType) {
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND,
                 THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
                    poseStack.translate(0, -0.5, 0);
            case GROUND -> {
                poseStack.mulPose(Axis.ZP.rotationDegrees(90));
                poseStack.scale(0.75f, 0.75f, 0.75f);
            }
            case FIXED -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(90));
                poseStack.mulPose(Axis.YP.rotationDegrees(45));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90));
                poseStack.translate(-7.75f / 16f, -4.5f / 16f, 0);
                poseStack.scale(0.9f, 0.9f, 0.9f);
            }
            default -> {}
        }
        super.renderByItem(stack, transformType, poseStack, source, packedLight);
        poseStack.popPose();
    }
}
