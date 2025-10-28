package com.xirc.nichirin.client.renderer.armor;

import mod.azure.azurelib.model.AzBone;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

public class TengenAccessoriesRenderer extends NichirinArmorRenderer {

    public TengenAccessoriesRenderer() {
        super("tengen_accessories");
    }

    @Override
    protected void applyBoneTransformations() {
        if (!(currentEntity instanceof AbstractClientPlayer player)) {
            return;
        }

        boolean isSlim = isSlimPlayer(player);

        if (currentBaseModel == null) return;

        // Match body transformation
        AzBone bodyBone = getBone("Body");
        if (bodyBone != null) {
            applyBodyTransform(bodyBone);
        }

        // Match arm transformations
        AzBone leftArm = getBone(isSlim ? "LeftArmSlim" : "LeftArm");
        AzBone rightArm = getBone(isSlim ? "RightArmSlim" : "RightArm");

        applyArmTransform(leftArm, currentBaseModel.leftArm, true);
        applyArmTransform(rightArm, currentBaseModel.rightArm, false);
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

        // Only show accessories in chest slot
        if (slot != EquipmentSlot.CHEST) {
            return;
        }

        // Show body-based accessories
        setBoneVisible(getBone("Body"), true);
        setBoneVisible(getBone(isSlim ? "LeftArmSlim" : "LeftArm"), true);
        setBoneVisible(getBone(isSlim ? "RightArmSlim" : "RightArm"), true);

        // Check if player is holding swords to show/hide back swords
        boolean hasMainHandSword = isSword(player.getMainHandItem());
        boolean hasOffHandSword = isSword(player.getOffhandItem());

        // Get back sword bones
        AzBone leftBackSword = getBone("LeftBackSword");
        AzBone rightBackSword = getBone("RightBackSword");

        // Hide back swords if they're being held
        if (hasMainHandSword && hasOffHandSword) {
            // Both swords in hands - hide both back swords
            setBoneVisible(leftBackSword, false);
            setBoneVisible(rightBackSword, false);
        } else if (hasMainHandSword) {
            // One sword in main hand - hide right back sword
            setBoneVisible(rightBackSword, false);
            setBoneVisible(leftBackSword, true);
        } else if (hasOffHandSword) {
            // One sword in off hand - hide left back sword
            setBoneVisible(leftBackSword, false);
            setBoneVisible(rightBackSword, true);
        } else {
            // No swords in hands - show both back swords
            setBoneVisible(leftBackSword, true);
            setBoneVisible(rightBackSword, true);
        }
    }

    /**
     * Check if an item is a sword
     */
    private boolean isSword(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SwordItem;
    }
}