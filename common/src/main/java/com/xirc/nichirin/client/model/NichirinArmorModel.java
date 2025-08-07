package com.xirc.nichirin.client.model;

import com.xirc.nichirin.BreathOfNichirin;
import mod.azure.azurelib.core.animatable.GeoAnimatable;
import mod.azure.azurelib.model.GeoModel;
import net.minecraft.resources.ResourceLocation;

public class NichirinArmorModel<T extends GeoAnimatable> extends GeoModel<T> {
    protected final String modelName;
    protected final String textureName;

    public NichirinArmorModel(final String name) {
        this(name, name);
    }

    public NichirinArmorModel(final String modelName, final String textureName) {
        this.modelName = modelName;
        this.textureName = textureName;
    }

    @Override
    public ResourceLocation getModelResource(final T object) {
        return BreathOfNichirin.id("geo/" + modelName + ".geo.json"); //ex: geo + tanjiro_scarf + .geo.json
    }

    @Override
    public ResourceLocation getTextureResource(final T object) {
        return BreathOfNichirin.id("textures/armor/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(final T animatable) {
        return BreathOfNichirin.id("animations/" + modelName + ".animation.json");
    }
}
