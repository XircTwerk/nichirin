package com.xirc.nichirin.client.renderer.armor;

import mod.azure.azurelib.model.AzBone;
import net.minecraft.world.entity.EquipmentSlot;

public class ZenitsuCapeRenderer extends NichirinArmorRenderer {

    public ZenitsuCapeRenderer() {
        super("zenitsu_cape");
    }

    @Override
    protected void applyBoneTransformations() {
        // Match body rotation for cape to follow player
        if (currentBaseModel != null) {
            matchRotation(currentBaseModel.body, getBone("Body"));
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