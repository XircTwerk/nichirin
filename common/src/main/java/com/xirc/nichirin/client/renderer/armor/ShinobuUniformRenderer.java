package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.model.NichirinArmorModel;
import com.xirc.nichirin.common.item.armor.NichirinArmorItem;
import mod.azure.azurelib.cache.object.GeoBone;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

public class ShinobuUniformRenderer extends NichirinArmorRenderer<NichirinArmorItem> {
    public ShinobuUniformRenderer() {
        super(new NichirinArmorModel<>("shinobu_uniform"));
    }

    @Override
    public GeoBone getHeadBone() {
        return this.model.getBone("Head").orElse(super.getHeadBone());
    }

    @Nullable
    @Override
    public GeoBone getBodyBone() {
        return this.model.getBone("chestplate").orElse(super.getBodyBone());
    }

    @Nullable
    @Override
    public GeoBone getLeftArmBone() {
        return this.model.getBone("leftArm").orElse(super.getLeftArmBone());
    }

    @Nullable
    @Override
    public GeoBone getRightArmBone() {
        return this.model.getBone("rightArm").orElse(super.getRightArmBone());
    }

    @Nullable
    @Override
    public GeoBone getRightLegBone() {
        return this.model.getBone("rightLeg").orElse(super.getRightLegBone());
    }

    @Nullable
    @Override
    public GeoBone getLeftLegBone() {
        return this.model.getBone("leftLeg").orElse(super.getLeftLegBone());
    }

    @Nullable
    @Override
    public GeoBone getRightBootBone() {
        return this.model.getBone("rightBoot").orElse(super.getRightBootBone());
    }

    @Nullable
    @Override
    public GeoBone getLeftBootBone() {
        return this.model.getBone("leftBoot").orElse(super.getLeftBootBone());
    }

    @Override
    protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
        super.applyBaseTransformations(baseModel);

        // Scale down base uniform arms to prevent clipping through cape
        GeoBone baseLeftArm = this.model.getBone("leftArm").orElse(null);
        GeoBone baseRightArm = this.model.getBone("rightArm").orElse(null);
        GeoBone chestplate = this.model.getBone("chestplate").orElse(null);

        if (baseLeftArm != null) baseLeftArm.setScaleX(0.95f);  // Scale down slightly
        if (baseRightArm != null) baseRightArm.setScaleX(0.95f); // Scale down slightly
        if (chestplate != null) chestplate.setScaleX(0.98f);     // Scale down body slightly
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
            case LEGS -> {
                setBoneVisible(this.model.getBone("chestplate").orElse(null), true);
                setBoneVisible(this.model.getBone("leftArm").orElse(null), true);
                setBoneVisible(this.model.getBone("rightArm").orElse(null), true);
                setBoneVisible(this.model.getBone("leftLeg").orElse(null), true);
                setBoneVisible(this.model.getBone("rightLeg").orElse(null), true);
            }
            case FEET -> {
                setBoneVisible(this.model.getBone("leftBoot").orElse(null), true);
                setBoneVisible(this.model.getBone("rightBoot").orElse(null), true);
            }
        }
    }
}