package com.xirc.nichirin.client.renderer.entity.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.renderer.entity.BaseAZNichirinEntityRenderer;
import com.xirc.nichirin.common.entity.npc.DemonNPCEntity;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Base renderer for Demon NPCs using AzureLib animations.
 * Individual demon types should extend this or create their own renderers.
 */
public class DemonNPCRenderer extends BaseAZNichirinEntityRenderer<DemonNPCEntity> {

    private static final ResourceLocation DEFAULT_GEO = new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/demon_npc.geo.json");
    private static final ResourceLocation DEFAULT_TEXTURE = new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/entity/npc/demon_npc.png");

    public DemonNPCRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<DemonNPCEntity>builder(DEFAULT_GEO, DEFAULT_TEXTURE)
                        .setAnimatorProvider(() -> null) // Override in subclasses
                        .build(),
                context,
                DEFAULT_TEXTURE
        );
    }

    @Override
    public void render(DemonNPCEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();

        float scale = entity.getRenderScale();
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(DemonNPCEntity entity) {
        // Just use the demon type directly as the texture name
        // No "demon_" prefix - exact match to texture file name
        String demonType = entity.getDemonType();
        if (demonType != null && !demonType.isEmpty()) {
            return new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/entity/npc/" + demonType + ".png");
        }
        return DEFAULT_TEXTURE;
    }
}