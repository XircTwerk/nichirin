package com.xirc.nichirin.client.renderer.armor;

import mod.azure.azurelib.model.AzBone;
import net.minecraft.world.entity.EquipmentSlot;

public class ShinobuCapeRenderer extends NichirinArmorRenderer {

    public ShinobuCapeRenderer() {
        super("shinobu_cape");
    }

    @Override
    protected void applyBoneTransformations() {
        // Match body transformation for cape to follow player
        AzBone bodyBone = getBone("Body");
        if (bodyBone != null && currentBaseModel != null) {
            applyBodyTransform(bodyBone);
        }
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        // Set all bones invisible first
        setAllVisible(false);

        // Only show cape when in chest slot
        if (slot == EquipmentSlot.CHEST) {
            setBoneVisible(getBone("Body"), true);
        }
    }
}