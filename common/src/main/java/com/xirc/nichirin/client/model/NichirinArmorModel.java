package com.xirc.nichirin.client.model;

import com.xirc.nichirin.BreathOfNichirin;
import mod.azure.azurelib.core.animatable.GeoAnimatable;
import mod.azure.azurelib.model.GeoModel;
import net.minecraft.resources.ResourceLocation;

/**
 * Base model class for Nichirin armor pieces using AzureLib
 * Handles resource location generation for models, textures, and animations
 */
public class NichirinArmorModel<T extends GeoAnimatable> extends GeoModel<T> {

    private final ArmorResourcePaths resourcePaths;

    public NichirinArmorModel(String armorName) {
        this.resourcePaths = new ArmorResourcePaths(armorName, armorName);
    }

    public NichirinArmorModel(String modelPath, String texturePath) {
        this.resourcePaths = new ArmorResourcePaths(modelPath, texturePath);
    }

    @Override
    public ResourceLocation getModelResource(T armorPiece) {
        return resourcePaths.getModelLocation();
    }

    @Override
    public ResourceLocation getTextureResource(T armorPiece) {
        return resourcePaths.getTextureLocation();
    }

    @Override
    public ResourceLocation getAnimationResource(T armorPiece) {
        return resourcePaths.getAnimationLocation();
    }

    /**
     * Inner class to manage armor resource paths
     */
    private static class ArmorResourcePaths {
        private final String modelIdentifier;
        private final String textureIdentifier;

        ArmorResourcePaths(String modelId, String textureId) {
            this.modelIdentifier = modelId;
            this.textureIdentifier = textureId;
        }

        ResourceLocation getModelLocation() {
            return BreathOfNichirin.id("geo/" + modelIdentifier + ".geo.json");
        }

        ResourceLocation getTextureLocation() {
            return BreathOfNichirin.id("textures/armor/" + textureIdentifier + ".png");
        }

        ResourceLocation getAnimationLocation() {
            return BreathOfNichirin.id("animations/" + modelIdentifier + ".animation.json");
        }
    }
}