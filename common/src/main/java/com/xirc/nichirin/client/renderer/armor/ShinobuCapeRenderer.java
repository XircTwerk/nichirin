package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.renderer.armor.core.NichirinCapeArmorBoneProvider;
import net.minecraft.world.entity.EquipmentSlot;

public class ShinobuCapeRenderer extends NichirinArmorRenderer {

    public ShinobuCapeRenderer() {
        super("shinobu_cape", "shinobu_cape", new NichirinCapeArmorBoneProvider("Cape"), "shinobu_cape");
    }

    @Override
    protected void applyBoneTransformations() {
        if (currentBaseModel == null) return;
        // Cape body is handled by the bone provider.
        // capeLeft/capeRight are arm-covering pieces — they must follow arm rotation+position,
        // not body rotation, so they swing correctly when the player walks.
        matchArmBone(currentBaseModel.leftArm, getBone("capeLeft"), true);
        matchArmBone(currentBaseModel.rightArm, getBone("capeRight"), false);
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