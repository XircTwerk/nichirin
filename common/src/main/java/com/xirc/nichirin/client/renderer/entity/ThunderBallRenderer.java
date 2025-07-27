package com.xirc.nichirin.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.model.entity.ThunderBallModel;
import com.xirc.nichirin.common.entity.ThunderBallEntity;
import mod.azure.azurelib.cache.object.BakedGeoModel;
import mod.azure.azurelib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The {@link GeoEntityRenderer} for {@link ThunderBallEntity}.
 * @see ThunderBallModel
 */
public class ThunderBallRenderer<T> extends GeoEntityRenderer<ThunderBallEntity> {

    protected float scaleWidth = 2;
    protected float scaleHeight = 2;

    public ThunderBallRenderer(final EntityRendererProvider.Context context) {
        super(context, new ThunderBallModel());
    }

    public ThunderBallRenderer<T> withScale(float scaleWidth, float scaleHeight) {
        this.scaleWidth = scaleWidth;
        this.scaleHeight = scaleHeight;

        return this;
    }

    @Override
    public void render(ThunderBallEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        // Scale the model to 2 times bigger
        poseStack.pushPose();
        poseStack.scale(2.0f, 2.0f, 2.0f);

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);

        poseStack.popPose();
    }

    @Override
    public RenderType getRenderType(final ThunderBallEntity animatable, final ResourceLocation texture,
                                    final @Nullable MultiBufferSource bufferSource, final float partialTick) {
        // Use translucent render type for glowing effect
        return RenderType.entityTranslucent(this.getTextureLocation(animatable));
    }

    @Override
    protected float getDeathMaxRotation(ThunderBallEntity animatable) {
        // No death rotation for projectiles
        return 0.0f;
    }

    @Override
    public boolean shouldRender(ThunderBallEntity livingEntity,
                                net.minecraft.client.renderer.culling.Frustum camera,
                                double camX, double camY, double camZ) {
        // Always render if within reasonable distance
        return super.shouldRender(livingEntity, camera, camX, camY, camZ);
    }
}