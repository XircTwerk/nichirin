package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.model.NichirinArmorModel;
import com.xirc.nichirin.common.item.armor.NichirinArmorItem;
import com.xirc.nichirin.common.item.katana.SoundKatana;
import mod.azure.azurelib.cache.object.GeoBone;
import mod.azure.azurelib.util.RenderUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class TengenAccessoriesRenderer extends NichirinArmorRenderer<NichirinArmorItem> {
    public TengenAccessoriesRenderer() {
        super(new NichirinArmorModel<>("tengen_accessories"));
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

        // Get sword bones for visibility control
        GeoBone sword1 = this.model.getBone("Sword").orElse(null);
        GeoBone sword2 = this.model.getBone("Sword2").orElse(null);

        // Check if player is dual-wielding sound katanas to hide swords
        boolean hideSwords = false;
        if (this.currentEntity instanceof Player player) {
            hideSwords = SoundKatana.isPlayerDualWieldingSoundKatanas(player);
        }

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

        // Hide swords if player is dual-wielding sound katanas
        if (hideSwords) {
            if (sword1 != null) {
                sword1.setScaleX(0f);
                sword1.setScaleY(0f);
                sword1.setScaleZ(0f);
            }
            if (sword2 != null) {
                sword2.setScaleX(0f);
                sword2.setScaleY(0f);
                sword2.setScaleZ(0f);
            }
        } else {
            // Restore normal sword scaling
            if (sword1 != null) {
                sword1.setScaleX(1f);
                sword1.setScaleY(1f);
                sword1.setScaleZ(1f);
            }
            if (sword2 != null) {
                sword2.setScaleX(1f);
                sword2.setScaleY(1f);
                sword2.setScaleZ(1f);
            }
        }

        // Apply scaling based on player model type
        if (this.currentEntity instanceof AbstractClientPlayer player) {
            boolean isSlim = isSlimPlayer(player);

            if (isSlim) {
                if (capeLeft != null) {
                    capeLeft.setScaleX(0.8f);
                    capeLeft.setScaleY(1.15f);
                    capeLeft.setScaleZ(1f);
                }
                if (capeRight != null) {
                    capeRight.setScaleX(0.8f);
                    capeRight.setScaleY(1.15f);
                    capeRight.setScaleZ(1f);
                }
            } else {
                if (capeLeft != null) {
                    capeLeft.setScaleX(1f);
                    capeLeft.setScaleY(1.15f);
                    capeLeft.setScaleZ(1f);
                }
                if (capeRight != null) {
                    capeRight.setScaleX(1f);
                    capeRight.setScaleY(1.15f);
                    capeRight.setScaleZ(1f);
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

            // Always make sword bones visible by default - scaling handles the hiding
            setBoneVisible(this.model.getBone("Sword").orElse(null), true);
            setBoneVisible(this.model.getBone("Sword2").orElse(null), true);
        }
    }
}