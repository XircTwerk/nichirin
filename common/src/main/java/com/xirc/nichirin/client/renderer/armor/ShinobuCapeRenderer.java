package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.model.NichirinArmorModel;
import com.xirc.nichirin.common.item.armor.NichirinArmorItem;
import mod.azure.azurelib.cache.object.GeoBone;
import mod.azure.azurelib.util.RenderUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

public class ShinobuCapeRenderer extends NichirinArmorRenderer<NichirinArmorItem> {
    public ShinobuCapeRenderer() {
        super(new NichirinArmorModel<>("shinobu_cape"));
    }

    @Override
    public GeoBone getHeadBone() {
        return this.model.getBone("Head").orElse(super.getHeadBone());
    }

    @Nullable
    @Override
    public GeoBone getBodyBone() {
        return this.model.getBone("Cape").orElse(super.getBodyBone());
    }

    @Nullable
    @Override
    public GeoBone getLeftArmBone() {
        return this.model.getBone("capeLeft").orElse(super.getLeftArmBone());
    }

    @Nullable
    @Override
    public GeoBone getRightArmBone() {
        return this.model.getBone("capeRight").orElse(super.getRightArmBone());
    }

    @Override
    protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
        super.applyBaseTransformations(baseModel);

        // Handle cape arms scaling and positioning
        GeoBone capeLeft = this.model.getBone("capeLeft").orElse(null);
        GeoBone capeRight = this.model.getBone("capeRight").orElse(null);
        GeoBone cape = this.model.getBone("Cape").orElse(null);

        // Scale cape arms slightly larger as outer layer
        if (capeLeft != null) capeLeft.setScaleX(1.05f);
        if (capeRight != null) capeRight.setScaleX(1.05f);

        // Make sure cape body follows player body
        if (cape != null) {
            ModelPart bodyPart = baseModel.body;
            RenderUtils.matchModelPartRot(bodyPart, cape);
            cape.updatePosition(bodyPart.x, -bodyPart.y, bodyPart.z - 0.1f); // Slightly forward
        }

        // Position cape arms slightly outward to prevent clipping
        if (capeLeft != null) {
            ModelPart leftArmPart = baseModel.leftArm;
            RenderUtils.matchModelPartRot(leftArmPart, capeLeft);
            capeLeft.updatePosition(leftArmPart.x - 5.2f, 2f - leftArmPart.y, leftArmPart.z - 0.1f);
        }

        if (capeRight != null) {
            ModelPart rightArmPart = baseModel.rightArm;
            RenderUtils.matchModelPartRot(rightArmPart, capeRight);
            capeRight.updatePosition(rightArmPart.x + 5.2f, 2f - rightArmPart.y, rightArmPart.z - 0.1f);
        }
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        setAllVisible(false);

        if (currentSlot == EquipmentSlot.CHEST) {
            setBoneVisible(this.model.getBone("Cape").orElse(null), true);
            setBoneVisible(this.model.getBone("capeLeft").orElse(null), true);
            setBoneVisible(this.model.getBone("capeRight").orElse(null), true);
            setBoneVisible(this.model.getBone("Lower parts").orElse(null), true);
        }
    }
}