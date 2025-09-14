package com.xirc.nichirin.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.model.entity.BoarEntityModel;
import com.xirc.nichirin.common.entity.BoarEntity;
import mod.azure.azurelib.cache.object.BakedGeoModel;
import mod.azure.azurelib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The {@link GeoEntityRenderer} for {@link BoarEntity}.
 * @see BoarEntityModel
 */
public class BoarEntityRenderer extends GeoEntityRenderer<BoarEntity> {

    public BoarEntityRenderer(final EntityRendererProvider.Context context) {
        super(context, new BoarEntityModel());
    }

    @Override
    public void render(BoarEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        poseStack.pushPose();

        // Make boar 1.25x bigger, but babies should be reasonable size
        if (entity.isBaby()) {
            poseStack.scale(0.8f, 0.8f, 0.8f); // Bigger baby size (was 0.625f)
        } else {
            poseStack.scale(1.25f, 1.25f, 1.25f);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }

    @Override
    public RenderType getRenderType(final BoarEntity animatable, final ResourceLocation texture,
                                    final @Nullable MultiBufferSource bufferSource, final float partialTick) {
        // Use cutout render type for normal rendering
        return RenderType.entityCutout(this.getTextureLocation(animatable));
    }

    @Override
    protected float getDeathMaxRotation(BoarEntity animatable) {
        // Standard death rotation
        return 90.0f;
    }

    @Override
    public boolean shouldRender(BoarEntity livingEntity,
                                net.minecraft.client.renderer.culling.Frustum camera,
                                double camX, double camY, double camZ) {
        // Standard rendering distance check
        return super.shouldRender(livingEntity, camera, camX, camY, camZ);
    }
}