package com.xirc.nichirin.client.renderer.item;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xirc.nichirin.BreathOfNichirin;
import mod.azure.azurelib.common.render.item.AzItemRenderer;
import mod.azure.azurelib.common.render.item.AzItemRendererConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

/**
 * Renders the ChainBallAxe axe as its 3D geo when held/dropped/in-frame (the inherited {@code renderByItem}),
 * but draws a flat 2D sprite as the INVENTORY ICON ({@code renderByGui}). AzureLib routes the {@code GUI}
 * display context to {@code renderByGui} and every other context to {@code renderByItem}, so overriding
 * only the GUI path gives a plain 2D icon while keeping the conventional in-hand 3D axe.
 *
 * <p>The quad is drawn in the item's local 0..1 block space: by the time {@code renderByGui} runs, vanilla
 * has already applied the model's {@code gui} display transform and a {@code translate(-0.5)} that centers
 * the item in the slot. For the sprite to fill the slot upright, the model's {@code gui} transform must be
 * identity (see {@code models/item/chain_ball_axe.json}).</p>
 */
public class ChainBallAxeItemRenderer extends AzItemRenderer {

    private final ResourceLocation icon;

    public ChainBallAxeItemRenderer(ResourceLocation geoModel, ResourceLocation texture, ResourceLocation icon) {
        super(AzItemRendererConfig.builder(geoModel, texture).build());
        this.icon = icon;
    }

    @Override
    public void renderByGui(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                            MultiBufferSource source, int packedLight) {
        Lighting.setupForFlatItems();
        MultiBufferSource.BufferSource buffers = source instanceof MultiBufferSource.BufferSource bs
                ? bs : Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(icon));
        Matrix4f mat = poseStack.last().pose();
        int light = LightTexture.FULL_BRIGHT;
        // Flat quad, 0..1 in X/Y (vanilla's translate(-0.5) centers it). +Y is the top of the sprite = v0.
        vtx(vc, mat, 0f, 1f, 0.5f, 0f, 0f, light); // top-left
        vtx(vc, mat, 1f, 1f, 0.5f, 1f, 0f, light); // top-right
        vtx(vc, mat, 1f, 0f, 0.5f, 1f, 1f, light); // bottom-right
        vtx(vc, mat, 0f, 0f, 0.5f, 0f, 1f, light); // bottom-left
        buffers.endBatch();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
    }

    private static void vtx(VertexConsumer vc, Matrix4f mat, float x, float y, float z,
                            float u, float v, int light) {
        vc.addVertex(mat, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0f, 0f, 1f);
    }

    public static ChainBallAxeItemRenderer create(String geoName, String textureName, String iconName) {
        return new ChainBallAxeItemRenderer(
                ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "geo/" + geoName + ".geo.json"),
                ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/item/" + textureName + ".png"),
                ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/item/" + iconName + ".png"));
    }
}
