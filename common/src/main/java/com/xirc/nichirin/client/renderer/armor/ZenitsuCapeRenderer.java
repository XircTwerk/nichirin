package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.model.NichirinArmorModel;
import com.xirc.nichirin.common.item.armor.NichirinArmorItem;
import mod.azure.azurelib.cache.object.GeoBone;
import mod.azure.azurelib.util.RenderUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ZenitsuCapeRenderer extends NichirinArmorRenderer<NichirinArmorItem> {
    public ZenitsuCapeRenderer() {
        super(new NichirinArmorModel<>("zenitsu_cape"));
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
        return this.model.getBone("CapeLeft").orElse(super.getLeftArmBone());
    }

    @Nullable
    @Override
    public GeoBone getRightArmBone() {
        return this.model.getBone("CapeRight").orElse(super.getRightArmBone());
    }

    @Override
    public void prepForRender(@Nullable Entity entity, ItemStack stack, @Nullable EquipmentSlot slot, @Nullable HumanoidModel<?> baseModel) {
        super.prepForRender(entity, stack, slot, baseModel);
    }

    @Override
    protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
        // FIRST: Apply base transformations
        super.applyBaseTransformations(baseModel);

        GeoBone cape = this.model.getBone("Cape").orElse(null);
        GeoBone capeLeft = this.model.getBone("CapeLeft").orElse(null);
        GeoBone capeRight = this.model.getBone("CapeRight").orElse(null);

        // Apply positioning for all entities
        if (cape != null) {
            ModelPart bodyPart = baseModel.body;
            RenderUtils.matchModelPartRot(bodyPart, cape);
            cape.updatePosition(bodyPart.x, -bodyPart.y + 0.1875f, bodyPart.z);
        }

        if (capeLeft != null) {
            ModelPart leftArmPart = baseModel.leftArm;
            RenderUtils.matchModelPartRot(leftArmPart, capeLeft);
            capeLeft.updatePosition(leftArmPart.x - 5f, 2f - leftArmPart.y + 0.1875f, leftArmPart.z);
        }

        if (capeRight != null) {
            ModelPart rightArmPart = baseModel.rightArm;
            RenderUtils.matchModelPartRot(rightArmPart, capeRight);
            capeRight.updatePosition(rightArmPart.x + 5f, 2f - rightArmPart.y + 0.1875f, rightArmPart.z);
        }

        // Apply scaling based on player model type
        if (this.currentEntity instanceof AbstractClientPlayer player) {
            boolean isSlim = isSlimPlayer(player);

            if (isSlim) {
                if (capeLeft != null) {
                    capeLeft.setScaleX(1.35f);
                    capeLeft.setScaleY(1.15f);
                    capeLeft.setScaleZ(1.15f);
                }
                if (capeRight != null) {
                    capeRight.setScaleX(1.35f);
                    capeRight.setScaleY(1.15f);
                    capeRight.setScaleZ(1.15f);
                }
            } else {
                if (capeLeft != null) {
                    capeLeft.setScaleX(1.5f);
                    capeLeft.setScaleY(1.15f);
                    capeLeft.setScaleZ(1.3f);
                }
                if (capeRight != null) {
                    capeRight.setScaleX(1.5f);
                    capeRight.setScaleY(1.15f);
                    capeRight.setScaleZ(1.3f);
                }
            }
        }
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        setAllVisible(false);

        if (currentSlot == EquipmentSlot.CHEST) {
            setBoneVisible(this.model.getBone("Cape").orElse(null), true);
            setBoneVisible(this.model.getBone("CapeLeft").orElse(null), true);
            setBoneVisible(this.model.getBone("CapeRight").orElse(null), true);
            setBoneVisible(this.model.getBone("Lower parts").orElse(null), true);
        }
    }
}