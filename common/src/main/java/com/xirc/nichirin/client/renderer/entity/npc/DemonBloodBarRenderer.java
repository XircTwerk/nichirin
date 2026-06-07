package com.xirc.nichirin.client.renderer.entity.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.npc.DemonNPCEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public final class DemonBloodBarRenderer {

    private static final ResourceLocation BLOOD_FULL = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/gui/blood_full.png");
    private static final ResourceLocation BLOOD_HALF = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/gui/blood_half.png");
    private static final ResourceLocation BLOOD_EMPTY = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/gui/blood_empty.png");

    private static final int SEGMENTS = 10;
    private static final int ICON_SIZE = 9;
    private static final int ICON_STEP = 8;
    private static final int REGEN_FLASH_TICKS = 12;

    private static final Map<Integer, Integer> LAST_BLOOD = new HashMap<>();
    private static final Map<Integer, Integer> REGEN_FLASH_UNTIL = new HashMap<>();

    private DemonBloodBarRenderer() {
    }

    public static void render(DemonNPCEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (entity.isInvisible()) return;

        int maxBlood = Math.max(1, entity.getMaxBloodPoints());
        int blood = Math.max(0, Math.min(entity.getBloodPoints(), maxBlood));
        double actualBlood = blood;
        double scaledBlood = actualBlood / maxBlood * SEGMENTS;
        int id = entity.getId();
        int lastBlood = LAST_BLOOD.getOrDefault(id, blood);
        if (blood > lastBlood) {
            REGEN_FLASH_UNTIL.put(id, entity.tickCount + REGEN_FLASH_TICKS);
        }
        LAST_BLOOD.put(id, blood);
        boolean regenFlash = REGEN_FLASH_UNTIL.getOrDefault(id, 0) > entity.tickCount;

        poseStack.pushPose();
        poseStack.translate(0.0D, entity.getBbHeight() + 0.45D, 0.0D);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        int width = (SEGMENTS - 1) * ICON_STEP + ICON_SIZE;
        int startX = -width / 2;

        for (int i = 0; i < SEGMENTS; i++) {
            int segment = i + 1;
            ResourceLocation texture;
            if (scaledBlood >= segment) {
                texture = BLOOD_FULL;
            } else if (scaledBlood >= segment - 0.5D) {
                texture = BLOOD_HALF;
            } else {
                texture = BLOOD_EMPTY;
            }
            boolean filled = scaledBlood >= segment - 0.5D;
            drawIcon(poseStack, bufferSource, texture, startX + i * ICON_STEP, 0, packedLight, regenFlash && filled);
        }

        poseStack.popPose();
    }

    private static void drawIcon(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation texture,
                                 int x, int y, int packedLight, boolean regenFlash) {
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.entityTranslucent(texture));
        float growth = regenFlash ? 1.25F : 1.0F;
        float extra = (ICON_SIZE * growth - ICON_SIZE) * 0.5F;
        float x0 = x - extra;
        float y0 = y - extra;
        float x1 = x + ICON_SIZE + extra;
        float y1 = y + ICON_SIZE + extra;
        int green = regenFlash ? 70 : 255;
        int blue = regenFlash ? 70 : 255;

        buffer.addVertex(matrix, x0, y1, 0.0F).setColor(255, green, blue, 255).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0F, 0.0F, 1.0F);
        buffer.addVertex(matrix, x1, y1, 0.0F).setColor(255, green, blue, 255).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0F, 0.0F, 1.0F);
        buffer.addVertex(matrix, x1, y0, 0.0F).setColor(255, green, blue, 255).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0F, 0.0F, 1.0F);
        buffer.addVertex(matrix, x0, y0, 0.0F).setColor(255, green, blue, 255).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(packedLight).setNormal(0.0F, 0.0F, 1.0F);
    }
}
