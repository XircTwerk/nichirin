package com.xirc.nichirin.client.model;

import com.xirc.nichirin.BreathOfNichirin;
import mod.azure.azurelib.core.animatable.GeoAnimatable;
import mod.azure.azurelib.model.GeoModel;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

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
        return BreathOfNichirin.id("geo/" + modelName + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(final T object) {
        return BreathOfNichirin.id("textures/armor/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(final T animatable) {
        ResourceLocation animationFile = BreathOfNichirin.id("animations/" + modelName + ".animation.json");

        // Check if animation file exists
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(animationFile);
            return animationFile; // File exists, return it
        } catch (Exception e) {
            // Animation file doesn't exist, create a dummy/empty animation file resource location
            // AzureLib requires a valid ResourceLocation, so we'll return a fallback
            return BreathOfNichirin.id("animations/empty.animation.json");
        }
    }
}