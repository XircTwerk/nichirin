package com.xirc.nichirin.client.renderer.armor;

import net.minecraft.world.entity.EquipmentSlot;

public class JigoroUniformRenderer extends NichirinArmorRenderer {

    public JigoroUniformRenderer() {
        // Animate the kimono skirt (Lowerpartsfront/Lowerpartsback) with the shared kimono animation.
        // Those bones are only visible on the chest/cape piece, so other slots are unaffected.
        super("jigoro_uniform", "jigoro_uniform",
                com.xirc.nichirin.client.renderer.armor.core.NichirinArmorBoneProvider.INSTANCE, "kimono");
    }

    @Override
    protected void applyBoneTransformations() {
        super.applyBoneTransformations();
        // "Cape" is a top-level geo bone (sibling of chestplate) that holds the kimono skirt
        // (Lowerpartsfront/Lowerpartsback). The uniform bone provider doesn't map it, so the base
        // transform never moves it — it would stay at the geo origin and not follow the player.
        // Bind it to the body the same way the cape provider does for the chest piece.
        if (currentBaseModel != null) {
            matchBodyBone(currentBaseModel.body, getBone("Cape"));
        }
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
                // chestplate is the torso bone — must be visible (and transformed) with the chest piece
                setBoneVisible(getBone("chestplate"), true);
                setBoneVisible(getBone("leftArm"), true);
                setBoneVisible(getBone("rightArm"), true);
                // Cape contains the kimono skirt (Lowerpartsfront/Lowerpartsback). setAllVisible(false)
                // above hid the children too — re-enable the whole Cape subtree recursively or
                // the skirt geometry stays invisible even though the parent bone is shown.
                setAllBonesRecursive(getBone("Cape"), true);
            }
            case LEGS -> {
                setBoneVisible(getBone("leftLeg"), true);
                setBoneVisible(getBone("rightLeg"), true);
            }
            case FEET -> {
                // Model only authors a single leftBoot bone (no rightBoot in jigoro_uniform.geo).
                setBoneVisible(getBone("leftBoot"), true);
            }
        }
    }
}
