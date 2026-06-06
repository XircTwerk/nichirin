package com.xirc.nichirin.client.renderer.entity.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.npc.DemonNPCEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class DemonBloodBarRenderer {

    private static final ResourceLocation BLOOD_FULL = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/gui/blood_full.png");
    private static final ResourceLocation BLOOD_HALF = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/gui/blood_half.png");
    private static final ResourceLocation BLOOD_EMPTY = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/gui/blood_empty.png");

    private static final int SEGMENTS = 10;
    private static final int ICON_SIZE = 9;
    private static final int ICON_STEP = 8;

    private DemonBloodBarRenderer() {
    }

    public static void render(DemonNPCEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.isInvisible()) return;

        int maxBlood = Math.max(1, entity.getMaxBloodPoints());
        double actualBlood = Math.max(0.0D, Math.min(entity.getBloodPoints(), maxBlood));
        double scaledBlood = actualBlood / maxBlood * SEGMENTS;

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.45D, 0.0D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        int width = (SEGMENTS - 1) * ICON_STEP + ICON_SIZE;
        int startX = -width / 2;

        for (int i = 0; i < SEGMENTS; i++) {
            int segment = SEGMENTS - i;
            ResourceLocation texture;
            if (scaledBlood >= segment) {
                texture = BLOOD_FULL;
            } else if (scaledBlood >= segment - 0.5D) {
                texture = BLOOD_HALF;
            } else {
                texture = BLOOD_EMPTY;
            }
            drawIcon(poseStack, bufferSource, texture, startX + i * ICON_STEP, 0, packedLight);
        }

        poseStack.popPose();
    }

    private static void drawIcon(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture,
                                 int x, int y, int packedLight) {
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        float x0 = x;
        float y0 = y;
        float x1 = x + ICON_SIZE;
        float y1 = y + ICON_SIZE;

        buffer.addVertex(matrix, x0, y1, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setLight(packedLight);
        buffer.addVertex(matrix, x1, y1, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setLight(packedLight);
        buffer.addVertex(matrix, x1, y0, 0.0F).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setLight(packedLight);
        buffer.addVertex(matrix, x0, y0, 0.0F).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setLight(packedLight);
    }
}
