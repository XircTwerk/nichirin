package com.xirc.nichirin.client.renderer.entity.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.renderer.entity.BaseAZNichirinEntityRenderer;
import com.xirc.nichirin.common.entity.projectile.ThrownKatanaEntity;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ThrownKatanaRenderer extends BaseAZNichirinEntityRenderer<ThrownKatanaEntity> {

    private static final ResourceLocation GEO = new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/katana_inosuke.geo.json");
    private static final ResourceLocation TEX = new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/item/inosuke_katana.png");

    public ThrownKatanaRenderer(EntityRendererProvider.Context context) {
        super(AzEntityRendererConfig.<ThrownKatanaEntity>builder(GEO, TEX)
                .setAnimatorProvider(() -> null)
                .build(), context, TEX);
    }

    @Override
    public void render(ThrownKatanaEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        // Align the geo's +Y blade axis with the entity's travel direction.
        // YP(yRot-90) handles horizontal direction; ZP(90-xRot) combines the
        // 90° ground-style tilt (blade from +Y to travel plane) with pitch correction.
        float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        float xRot = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.mulPose(Axis.YP.rotationDegrees(yRot - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F - xRot));

        // End-over-end tumble; freeze when embedded in a block
        if (!entity.isStuck()) {
            float spinAngle = (entity.tickCount + partialTick) * 36.0F;
            poseStack.mulPose(Axis.YP.rotationDegrees(spinAngle));
        }
        // Flip blade to face travel direction (model is oriented backwards)
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }
}
