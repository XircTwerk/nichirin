package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.model.NichirinArmorModel;
import com.xirc.nichirin.common.item.armor.NichirinArmorItem;
import mod.azure.azurelib.cache.object.GeoBone;
import mod.azure.azurelib.util.RenderUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;

public class ShinobuUniformRenderer extends NichirinArmorRenderer<NichirinArmorItem> {
    public ShinobuUniformRenderer() {
        super(new NichirinArmorModel<>("shinobu_kimono"));
    }

    // Don't let the parent handle cape arms - they're not standard bones
    @Override
    public GeoBone getLeftArmBone() {
        // Only return the base uniform left arm, not cape
        return this.model.getBone("leftArm").orElse(super.getLeftArmBone());
    }

    @Override
    public GeoBone getRightArmBone() {
        // Only return the base uniform right arm, not cape
        return this.model.getBone("rightArm").orElse(super.getRightArmBone());
    }

    @Override
    protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
        super.applyBaseTransformations(baseModel);

        // Manually apply transformations to cape arms since parent doesn't handle them
        GeoBone capeLeft = this.model.getBone("capeLeft").orElse(null);
        GeoBone capeRight = this.model.getBone("capeRight").orElse(null);

        if (capeLeft != null) {
            ModelPart leftArmPart = baseModel.leftArm;
            // Copy rotation AND position exactly like parent does for arms
            RenderUtils.matchModelPartRot(leftArmPart, capeLeft);
            capeLeft.updatePosition(leftArmPart.x - 5f, 2f - leftArmPart.y, leftArmPart.z);
            capeLeft.setScaleX(1.15f);
        }

        if (capeRight != null) {
            ModelPart rightArmPart = baseModel.rightArm;
            // Copy rotation AND position exactly like parent does for arms
            RenderUtils.matchModelPartRot(rightArmPart, capeRight);
            capeRight.updatePosition(rightArmPart.x + 5f, 2f - rightArmPart.y, rightArmPart.z);
            capeRight.setScaleX(1.15f);
        }

        // Scale base uniform arms
        GeoBone baseLeftArm = this.model.getBone("leftArm").orElse(null);
        GeoBone baseRightArm = this.model.getBone("rightArm").orElse(null);

        if (baseLeftArm != null) baseLeftArm.setScaleX(1.05f);
        if (baseRightArm != null) baseRightArm.setScaleX(1.05f);
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        setAllVisible(false);

        switch (currentSlot) {
            case HEAD -> {
                setBoneVisible(this.model.getBone("Head").orElse(null), true);
                setBoneVisible(this.model.getBone("Butterfly").orElse(null), true);
                setBoneVisible(this.model.getBone("Butterfly2").orElse(null), true);
            }
            case CHEST -> {
                setBoneVisible(this.model.getBone("Cape").orElse(null), true);
                setBoneVisible(this.model.getBone("capeLeft").orElse(null), true);
                setBoneVisible(this.model.getBone("capeRight").orElse(null), true);
                setBoneVisible(this.model.getBone("Lower parts").orElse(null), true);
            }
            case LEGS -> {
                setBoneVisible(this.body, true);
                setBoneVisible(this.leftArm, true);
                setBoneVisible(this.rightArm, true);
                setBoneVisible(this.leftLeg, true);
                setBoneVisible(this.rightLeg, true);
            }
            case FEET -> {
                setBoneVisible(this.leftBoot, true);
                setBoneVisible(this.rightBoot, true);
            }
        }
    }
}