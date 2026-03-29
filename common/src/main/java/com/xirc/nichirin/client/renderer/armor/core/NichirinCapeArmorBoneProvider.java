package com.xirc.nichirin.client.renderer.armor.core;

import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.armor.bone.AzArmorBoneProvider;
import org.jetbrains.annotations.Nullable;

/**
 * Bone provider for Nichirin cape/accessories armor.
 * Maps the root cape bone to the body slot so the pipeline applies body rotation
 * and position. Optionally maps arm bones for capes that have arm parts (e.g. Urokodaki).
 */
public class NichirinCapeArmorBoneProvider implements AzArmorBoneProvider {

    private final String bodyBoneName;

    public NichirinCapeArmorBoneProvider(String bodyBoneName) {
        this.bodyBoneName = bodyBoneName;
    }

    @Override
    public @Nullable AzBone getHeadBone(AzBakedModel model) {
        return null;
    }

    @Override
    public @Nullable AzBone getBodyBone(AzBakedModel model) {
        return model.getBone(bodyBoneName).orElse(null);
    }

    @Override
    public @Nullable AzBone getRightArmBone(AzBakedModel model) {
        return null;
    }

    @Override
    public @Nullable AzBone getLeftArmBone(AzBakedModel model) {
        return null;
    }

    @Override
    public @Nullable AzBone getRightLegBone(AzBakedModel model) {
        return null;
    }

    @Override
    public @Nullable AzBone getLeftLegBone(AzBakedModel model) {
        return null;
    }

    @Override
    public @Nullable AzBone getRightBootBone(AzBakedModel model) {
        return null;
    }

    @Override
    public @Nullable AzBone getLeftBootBone(AzBakedModel model) {
        return null;
    }

    @Override
    public @Nullable AzBone getWaistBone(AzBakedModel model) {
        return null;
    }
}
