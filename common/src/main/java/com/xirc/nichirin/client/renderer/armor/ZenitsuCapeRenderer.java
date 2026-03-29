package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.renderer.armor.core.NichirinCapeArmorBoneProvider;
import net.minecraft.world.entity.EquipmentSlot;

public class ZenitsuCapeRenderer extends NichirinArmorRenderer {

    public ZenitsuCapeRenderer() {
        super("zenitsu_cape", "zenitsu_cape", new NichirinCapeArmorBoneProvider("Cape"), "zenitsu_cape");
    }

    @Override
    protected void applyBoneTransformations() {
        if (currentBaseModel == null) return;
        // Cape body is handled by the bone provider.
        // CapeRight/CapeLeft are arm-covering pieces — follow arm rotation+position so they swing.
        // CapeRight pivot [-5,22,0] = right arm; CapeLeft pivot [5,22,0] = left arm.
        matchArmBone(currentBaseModel.rightArm, getBone("CapeRight"), false);
        matchArmBone(currentBaseModel.leftArm, getBone("CapeLeft"), true);
        scaleArmBones(getBone("CapeLeft"), getBone("CapeRight"));
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
