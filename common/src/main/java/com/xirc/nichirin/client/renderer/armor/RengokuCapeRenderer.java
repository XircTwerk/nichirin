package com.xirc.nichirin.client.renderer.armor;

import mod.azure.azurelib.model.AzBone;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;

public class RengokuCapeRenderer extends NichirinArmorRenderer {

    public RengokuCapeRenderer() {
        super("rengoku_cape");
    }

    @Override
    protected void applyBoneTransformations() {
        if (currentEntity instanceof ArmorStand armorStand) {
            applyArmorStandTransformations(armorStand);
        } else {
            // Match body transformation for cape to follow player
            AzBone bodyBone = getBone("Body");
            if (bodyBone != null && currentBaseModel != null) {
                applyBodyTransform(bodyBone);
            }
        }
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot slot) {
        // Set all bones invisible first
        setAllVisible(false);

        // Only show cape when in chest slot
        if (slot == EquipmentSlot.CHEST) {
            setBoneVisible(getBone("Body"), true);
        }
    }

    /**
     * Apply special transformations for armor stands
     */
    private void applyArmorStandTransformations(ArmorStand armorStand) {
        AzBone bodyBone = getBone("Body");
        if (bodyBone == null) {
            return;
        }

        // Get armor stand's body rotation
        net.minecraft.core.Rotations bodyRotation = armorStand.getBodyPose();

        // Convert degrees to radians and apply
        bodyBone.setRotX((float) Math.toRadians(bodyRotation.getX()));
        bodyBone.setRotY((float) Math.toRadians(bodyRotation.getY()));
        bodyBone.setRotZ((float) Math.toRadians(bodyRotation.getZ()));

        // Apply small adjustments for better cape positioning on stands
        if (armorStand.isSmall()) {
            // Small armor stands need scaled transformations
            bodyBone.setPosY(bodyBone.getPosY() + 4.0f);
        }
    }
}