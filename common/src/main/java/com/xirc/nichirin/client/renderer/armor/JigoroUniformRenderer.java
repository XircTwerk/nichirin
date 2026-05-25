package com.xirc.nichirin.client.renderer.armor;

import net.minecraft.world.entity.EquipmentSlot;

public class JigoroUniformRenderer extends NichirinArmorRenderer {

    public JigoroUniformRenderer() {
        super("jigoro_uniform");
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        setAllVisible(false);
        switch (slot) {
            case HEAD -> {
                setBoneVisible(getBone("Head"), true);
                setBoneVisible(getBone("Hair thingies"), true);
                setBoneVisible(getBone("Mustashe"), true);
                setBoneVisible(getBone("Eyebrows"), true);
            }
            case CHEST -> {
                setBoneVisible(getBone("leftArm"), true);
                setBoneVisible(getBone("rightArm"), true);
                setBoneVisible(getBone("Cape"), true);
            }
            case LEGS -> {
                setBoneVisible(getBone("chestplate"), true);
                setBoneVisible(getBone("leftLeg"), true);
                setBoneVisible(getBone("rightLeg"), true);
            }
            case FEET -> {
                setBoneVisible(getBone("leftBoot"), true);
                setBoneVisible(getBone("rightLeg"), true);
            }
        }
    }
}
