package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.renderer.armor.core.NichirinCapeArmorBoneProvider;
import net.minecraft.world.entity.EquipmentSlot;

public class SabitoCapeRenderer extends NichirinArmorRenderer {

    public SabitoCapeRenderer() {
        super("sabito_cape", "sabito_cape", new NichirinCapeArmorBoneProvider("Cape"), "sabito_cape");
    }

    @Override
    protected void applyBoneTransformations() {
        if (currentBaseModel == null) return;
        // Cape body is handled by the bone provider.
        // capeRight/capeLeft are children of Cape in the geo hierarchy, so their rotation is
        // applied in addition to Cape's (body) rotation. Setting arm rotation here makes them
        // swing with the arms while still following the body through the parent hierarchy.
        matchArmBone(currentBaseModel.rightArm, getBone("capeRight"), false);
        matchArmBone(currentBaseModel.leftArm, getBone("capeLeft"), true);
        scaleArmBones(getBone("capeLeft"), getBone("capeRight"));
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        if (slot == EquipmentSlot.CHEST) {
            setAllVisible(true);
        } else {
            setAllVisible(false);
        }
    }
}