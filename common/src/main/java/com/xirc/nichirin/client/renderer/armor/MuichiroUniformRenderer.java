package com.xirc.nichirin.client.renderer.armor;

import net.minecraft.world.entity.EquipmentSlot;

public class MuichiroUniformRenderer extends NichirinArmorRenderer {

    public MuichiroUniformRenderer() {
        super("muichiro_uniform");
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        setAllVisible(false);
        switch (slot) {
            case HEAD -> {
                setBoneVisible(getBone("Head"), true);
                setBoneVisible(getBone("Back_hair_upper"), true);
                setBoneVisible(getBone("Back hair lower"), true);
                setBoneVisible(getBone("Left front hair"), true);
                setBoneVisible(getBone("Right front hair"), true);
            }
            case CHEST -> {
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
            default -> {}
        }
    }
}
