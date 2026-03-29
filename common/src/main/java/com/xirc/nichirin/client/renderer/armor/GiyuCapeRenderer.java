package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.renderer.armor.core.NichirinCapeArmorBoneProvider;
import net.minecraft.world.entity.EquipmentSlot;

public class GiyuCapeRenderer extends NichirinArmorRenderer {

    public GiyuCapeRenderer() {
        super("giyu_cape", "giyu_cape", new NichirinCapeArmorBoneProvider("Cape"), "giyu_cape");
    }

    @Override
    protected void applyBoneTransformations() {
        if (currentBaseModel == null) return;
        // Cape body is handled by the bone provider.
        // capeRight/capeLeft are separate arm bones — follow arm rotation+position so they swing.
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
