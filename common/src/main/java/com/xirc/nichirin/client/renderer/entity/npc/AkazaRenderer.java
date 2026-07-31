package com.xirc.nichirin.client.renderer.entity.npc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.renderer.entity.BaseAZNichirinEntityRenderer;
import com.xirc.nichirin.client.renderer.entity.animator.AkazaAnimator;
import com.xirc.nichirin.client.renderer.entity.layer.AkazaEmissiveLayer;
import com.xirc.nichirin.common.entity.npc.AkazaEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class AkazaRenderer extends BaseAZNichirinEntityRenderer<AkazaEntity> {

    private static final ResourceLocation GEO = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "geo/akaza_npc.geo.json");
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(BreathOfNichirin.MOD_ID, "textures/entity/npc/akaza/akaza_npc.png");

    public AkazaRenderer(EntityRendererProvider.Context context) {
        super(
                AzEntityRendererConfig.<AkazaEntity>builder(GEO, TEX)
                        .setAnimatorProvider(AkazaAnimator::new)
                        .addRenderLayer(new AkazaEmissiveLayer())
                        .build(),
                context,
                TEX
        );
    }

    @Override
    public void render(AkazaEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        float scale = entity.getRenderScale();
        if (scale != 1.0f) {
            poseStack.scale(scale, scale, scale);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        DemonBloodBarRenderer.render(entity, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }
}
