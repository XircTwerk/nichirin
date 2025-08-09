package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.client.model.NichirinArmorModel;
import com.xirc.nichirin.common.item.armor.NichirinArmorItem;
import com.xirc.nichirin.mixin.client.PlayerModelAccessor;
import mod.azure.azurelib.cache.object.GeoBone;
import mod.azure.azurelib.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
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
    public void prepForRender(@Nullable Entity entity, ItemStack stack, @Nullable EquipmentSlot slot, @Nullable HumanoidModel<?> baseModel) {
        super.prepForRender(entity, stack, slot, baseModel);

        if (entity instanceof AbstractClientPlayer player) {
            EntityRenderer<? super AbstractClientPlayer> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();
                boolean isSlim = ((PlayerModelAccessor) playerModel).isSlim();

                GeoBone baseLeftArm = this.model.getBone("leftArm").orElse(null);
                GeoBone baseRightArm = this.model.getBone("rightArm").orElse(null);

                if (isSlim) {
                    if (baseLeftArm != null) {
                        baseLeftArm.setScaleX(1.35f);
                        baseLeftArm.setScaleZ(1.1f);
                    }
                    if (baseRightArm != null) {
                        baseRightArm.setScaleX(1.35f);
                        baseRightArm.setScaleZ(1.1f);
                    }
                } else {
                    if (baseLeftArm != null) {
                        baseLeftArm.setScaleX(1.55f);
                        baseLeftArm.setScaleY(1.05f);
                        baseLeftArm.setScaleZ(1.2f);
                    }
                    if (baseRightArm != null) {
                        baseRightArm.setScaleX(1.55f);
                        baseRightArm.setScaleY(1.05f);
                        baseRightArm.setScaleZ(1.2f);
                    }
                }
            }
        }
    }

    @Override
    protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
        super.applyBaseTransformations(baseModel);

        if (this.currentEntity instanceof AbstractClientPlayer player) {
            EntityRenderer<? super AbstractClientPlayer> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();
                boolean isSlim = ((PlayerModelAccessor) playerModel).isSlim();

                GeoBone chestplate = this.model.getBone("chestplate").orElse(null);
                GeoBone baseLeftArm = this.model.getBone("leftArm").orElse(null);
                GeoBone baseRightArm = this.model.getBone("rightArm").orElse(null);

                if (chestplate != null) chestplate.setScaleX(0.98f);

                if (isSlim) {
                    if (baseLeftArm != null) {
                        ModelPart leftArmPart = baseModel.leftArm;
                        RenderUtils.matchModelPartRot(leftArmPart, baseLeftArm);
                        baseLeftArm.updatePosition(leftArmPart.x - 5f, 2.25f - leftArmPart.y, leftArmPart.z + 0.1f);
                    }

                    if (baseRightArm != null) {
                        ModelPart rightArmPart = baseModel.rightArm;
                        RenderUtils.matchModelPartRot(rightArmPart, baseRightArm);
                        baseRightArm.updatePosition(rightArmPart.x + 5f, 2.25f - rightArmPart.y, rightArmPart.z + 0.1f);
                    }
                } else {
                    if (baseLeftArm != null) {
                        ModelPart leftArmPart = baseModel.leftArm;
                        RenderUtils.matchModelPartRot(leftArmPart, baseLeftArm);
                        baseLeftArm.updatePosition(leftArmPart.x - 5f, 2.25f - leftArmPart.y, leftArmPart.z + 0.2f);
                    }

                    if (baseRightArm != null) {
                        ModelPart rightArmPart = baseModel.rightArm;
                        RenderUtils.matchModelPartRot(rightArmPart, baseRightArm);
                        baseRightArm.updatePosition(rightArmPart.x + 5f, 2.25f - rightArmPart.y, rightArmPart.z + 0.2f);
                    }
                }
            }
        }
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