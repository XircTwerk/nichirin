package com.xirc.nichirin.client.model.entity;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.ThunderBallEntity;
import mod.azure.azurelib.model.GeoModel;
import net.minecraft.resources.ResourceLocation;

/**
 * The {@link GeoModel} for {@link ThunderBallEntity}.
 * @see com.xirc.nichirin.client.renderer.entity.ThunderBallRenderer ThunderBallRenderer
 */
public class ThunderBallModel extends GeoModel<ThunderBallEntity> {

    @Override
    public ResourceLocation getModelResource(final ThunderBallEntity animatable) {
        return new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/thunder_ball.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(final ThunderBallEntity animatable) {
        return new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/entity/thunder_ball.png");
    }

    @Override
    public ResourceLocation getAnimationResource(final ThunderBallEntity animatable) {
        return new ResourceLocation(BreathOfNichirin.MOD_ID, "animations/thunder_ball.animation.json");
    }
}