package com.xirc.nichirin.client.renderer.armor.core;

import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.armor.bone.AzArmorBoneProvider;
import org.jetbrains.annotations.Nullable;

/**
 * Bone provider for Nichirin uniform armor.
 * Maps our geo bone names to the standard AzureLib armor slots so the pipeline
 * can apply correct rotation, position, and scale from the vanilla humanoid model.
 */
public class NichirinArmorBoneProvider implements AzArmorBoneProvider {

    public static final NichirinArmorBoneProvider INSTANCE = new NichirinArmorBoneProvider();

    @Override
    public @Nullable AzBone getHeadBone(AzBakedModel model) {
        return model.getBone("Head").orElse(null);
    }

    @Override
    public @Nullable AzBone getBodyBone(AzBakedModel model) {
        // Return null so the pipeline never hides/shows the chestplate bone;
        // we control its visibility in NichirinArmorRenderer and apply body rotation manually.
        return null;
    }

    // Geo bone naming matches Minecraft's HumanoidModel convention: "rightArm" = player's right arm.
    // Direct 1-to-1 mapping; no swap needed.

    @Override
    public @Nullable AzBone getRightArmBone(AzBakedModel model) {
        return model.getBone("rightArm").orElse(null);
    }

    @Override
    public @Nullable AzBone getLeftArmBone(AzBakedModel model) {
        return model.getBone("leftArm").orElse(null);
    }

    @Override
    public @Nullable AzBone getRightLegBone(AzBakedModel model) {
        return model.getBone("rightLeg").orElse(null);
    }

    @Override
    public @Nullable AzBone getLeftLegBone(AzBakedModel model) {
        return model.getBone("leftLeg").orElse(null);
    }

    @Override
    public @Nullable AzBone getRightBootBone(AzBakedModel model) {
        return model.getBone("rightBoot").orElse(null);
    }

    @Override
    public @Nullable AzBone getLeftBootBone(AzBakedModel model) {
        return model.getBone("leftBoot").orElse(null);
    }

    @Override
    public @Nullable AzBone getWaistBone(AzBakedModel model) {
        return null;
    }
}
