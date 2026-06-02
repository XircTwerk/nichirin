package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.renderer.armor.core.NichirinCapeArmorBoneProvider;
import net.minecraft.world.entity.EquipmentSlot;

public class UrokodakiCapeRenderer extends NichirinArmorRenderer {

    public UrokodakiCapeRenderer() {
        super("urokodaki_cape", "urokodaki_cape", new NichirinCapeArmorBoneProvider("Cape"), "kimono");
    }

    @Override
    protected void applyBoneTransformations() {
        if (currentBaseModel == null) return;
        // In urokodaki_cape.geo.json the arm bone names are physically swapped:
        //   geo "rightArm" has pivot [5,22,0]  → physically the LEFT arm position
        //   geo "leftArm"  has pivot [-5,22,0] → physically the RIGHT arm position
        // So we map vanilla rightArm → geo "leftArm" and vanilla leftArm → geo "rightArm".
        // isLeft=false for geo "leftArm" because it's physically at the right arm (x+5 offset).
        // isLeft=true  for geo "rightArm" because it's physically at the left arm (x-5 offset).
        matchArmBone(currentBaseModel.rightArm, getBone("leftArm"), false);
        matchArmBone(currentBaseModel.leftArm,  getBone("rightArm"), true);
        scaleArmBones(getBone("leftArm"), getBone("rightArm"));
        matchKimonoBone(getBone("Cape"));
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