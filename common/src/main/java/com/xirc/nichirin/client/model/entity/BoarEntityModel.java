package com.xirc.nichirin.client.model.entity;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.common.entity.BoarEntity;
import mod.azure.azurelib.model.GeoModel;
import net.minecraft.resources.ResourceLocation;

/**
 * The {@link GeoModel} for {@link BoarEntity}.
 * @see com.xirc.nichirin.client.renderer.entity.BoarEntityRenderer BoarEntityRenderer
 */
public class BoarEntityModel extends GeoModel<BoarEntity> {

    @Override
    public ResourceLocation getModelResource(final BoarEntity animatable) {
        return new ResourceLocation(BreathOfNichirin.MOD_ID, "geo/boar.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(final BoarEntity animatable) {
        return new ResourceLocation(BreathOfNichirin.MOD_ID, "textures/entity/boar.png");
    }

    @Override
    public ResourceLocation getAnimationResource(final BoarEntity animatable) {
        return new ResourceLocation(BreathOfNichirin.MOD_ID, "animations/boar.animation.json");
    }
}