package com.xirc.nichirin.client.renderer.armor;

import net.minecraft.world.entity.EquipmentSlot;

public class SabitoUniformRenderer extends NichirinArmorRenderer {

    public SabitoUniformRenderer() {
        super("sabito_uniform");
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        setAllVisible(false);
        switch (slot) {
            case HEAD -> setBoneVisible(getBone("Head"), true);
            case CHEST -> {
                setBoneVisible(getBone("chestplate"), true);
                setBoneVisible(getBone("leftArm"), true);
                setBoneVisible(getBone("rightArm"), true);
            }
            case LEGS -> {
                setBoneVisible(getBone("chestplate"), true);
                setBoneVisible(getBone("leftArm"), true);
                setBoneVisible(getBone("rightArm"), true);
                setBoneVisible(getBone("leftLeg"), true);
                setBoneVisible(getBone("rightLeg"), true);
            }
            case FEET -> {
                setBoneVisible(getBone("leftBoot"), true);
                setBoneVisible(getBone("rightBoot"), true);
            }
        }
    }
}
