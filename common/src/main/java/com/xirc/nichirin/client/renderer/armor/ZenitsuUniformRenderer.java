package com.xirc.nichirin.client.renderer.armor;

import mod.azure.azurelib.model.AzBone;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;

public class ZenitsuUniformRenderer extends NichirinArmorRenderer {

    public ZenitsuUniformRenderer() {
        super("zenitsu_uniform");
    }

    @Override
    protected void applyBoneTransformations() {
        if (!(currentEntity instanceof AbstractClientPlayer player)) {
            return;
        }

        boolean isSlim = isSlimPlayer(player);

        // Match body rotations
        if (currentBaseModel != null) {
            matchRotation(currentBaseModel.body, getBone("Body"));
            matchRotation(currentBaseModel.leftArm, getBone(isSlim ? "LeftArmSlim" : "LeftArm"));
            matchRotation(currentBaseModel.rightArm, getBone(isSlim ? "RightArmSlim" : "RightArm"));
            matchRotation(currentBaseModel.leftLeg, getBone("LeftLeg"));
            matchRotation(currentBaseModel.rightLeg, getBone("RightLeg"));
        }
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        if (!(currentEntity instanceof AbstractClientPlayer player)) {
            setAllVisible(false);
            return;
        }

        boolean isSlim = isSlimPlayer(player);

        // Set all bones invisible first
        setAllVisible(false);

        // Show only bones for the current slot
        switch (slot) {
            case CHEST -> {
                setBoneVisible(getBone("Body"), true);
                setBoneVisible(getBone(isSlim ? "LeftArmSlim" : "LeftArm"), true);
                setBoneVisible(getBone(isSlim ? "RightArmSlim" : "RightArm"), true);
            }
            case LEGS -> {
                setBoneVisible(getBone("LeftLeg"), true);
                setBoneVisible(getBone("RightLeg"), true);
            }
            case FEET -> {
                setBoneVisible(getBone("LeftBoot"), true);
                setBoneVisible(getBone("RightBoot"), true);
            }
        }
    }
}