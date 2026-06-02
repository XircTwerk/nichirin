package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.renderer.armor.core.NichirinCapeArmorBoneProvider;
import mod.azure.azurelib.common.model.AzBone;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

public class TengenAccessoriesRenderer extends NichirinArmorRenderer {

    public TengenAccessoriesRenderer() {
        super("tengen_accessories", "tengen_accessories", new NichirinCapeArmorBoneProvider("Cape"));
    }

    @Override
    protected void applyBoneTransformations() {
        if (currentBaseModel == null) return;
        // Cape body is handled by the bone provider.
        // CapeLeft/CapeRight are arm-covering pieces — follow arm rotation+position so they swing.
        matchArmBone(currentBaseModel.leftArm, getBone("CapeLeft"), true);
        matchArmBone(currentBaseModel.rightArm, getBone("CapeRight"), false);
        AzBone capeLeft  = getBone("CapeLeft");
        AzBone capeRight = getBone("CapeRight");
        if (capeLeft  != null) { capeLeft.setScaleX(1.1f);  capeLeft.setScaleZ(1.2f); }
        if (capeRight != null) { capeRight.setScaleX(1.1f); capeRight.setScaleZ(1.2f); }
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        if (slot != EquipmentSlot.CHEST) {
            setAllVisible(false);
            return;
        }

        setAllVisible(true);

        // Hide back swords that are currently being held
        if (currentEntity instanceof AbstractClientPlayer player) {
            boolean hasMainHandSword = isSword(player.getMainHandItem());
            boolean hasOffHandSword = isSword(player.getOffhandItem());

            if (hasMainHandSword) {
                // Right back sword drawn — hide Sword tree
                AzBone sword = getBone("Sword");
                if (sword != null) setAllBonesRecursive(sword, false);
            }
            if (hasOffHandSword) {
                // Left back sword drawn — hide Sword2 tree
                AzBone sword2 = getBone("Sword2");
                if (sword2 != null) setAllBonesRecursive(sword2, false);
            }
        }
    }

    private boolean isSword(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SwordItem;
    }
}