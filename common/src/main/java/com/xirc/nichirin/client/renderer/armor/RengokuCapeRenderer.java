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
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class RengokuCapeRenderer extends NichirinArmorRenderer<NichirinArmorItem> {
    public RengokuCapeRenderer() {
        super(new NichirinArmorModel<>("rengoku_cape"));
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
    public void prepForRender(@Nullable Entity entity, ItemStack stack, @Nullable EquipmentSlot slot, @Nullable HumanoidModel<?> baseModel) {
        super.prepForRender(entity, stack, slot, baseModel);

        GeoBone cape = this.model.getBone("Cape").orElse(null);
        GeoBone capeLeft = this.model.getBone("capeLeft").orElse(null);
        GeoBone capeRight = this.model.getBone("capeRight").orElse(null);

        if (entity instanceof ArmorStand) {
            if (cape != null) {
                cape.setScaleX(1.4f);
                cape.setScaleY(1.4f);
                cape.setScaleZ(1.6f);
            }
            if (capeLeft != null) {
                capeLeft.setScaleX(1.7f);
                capeLeft.setScaleY(1.1f);
                capeLeft.setScaleZ(1.2f);
            }
            if (capeRight != null) {
                capeRight.setScaleX(1.7f);
                capeRight.setScaleY(1.1f);
                capeRight.setScaleZ(1.2f);
            }
        } else if (entity instanceof AbstractClientPlayer player) {
            EntityRenderer<? super AbstractClientPlayer> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
            if (renderer instanceof PlayerRenderer playerRenderer) {
                PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();
                boolean isSlim = ((PlayerModelAccessor) playerModel).isSlim();

                if (isSlim) {
                    if (cape != null) {
                        cape.setScaleX(1.15f);
                        cape.setScaleY(1.425f);
                        cape.setScaleZ(1.8f);
                    }
                    if (capeLeft != null) {
                        capeLeft.setScaleX(1.3f);
                        capeLeft.setScaleY(1.2f);
                        capeLeft.setScaleZ(1.4f);
                    }
                    if (capeRight != null) {
                        capeRight.setScaleX(1.35f);
                        capeRight.setScaleY(1.2f);
                        capeRight.setScaleZ(1.4f);
                    }
                } else {
                    if (cape != null) {
                        cape.setScaleX(1.3f);
                        cape.setScaleY(1.425f);
                        cape.setScaleZ(1.8f);
                    }
                    if (capeLeft != null) {
                        capeLeft.setScaleX(1.55f);
                        capeLeft.setScaleY(1.2f);
                        capeLeft.setScaleZ(1.4f);
                    }
                    if (capeRight != null) {
                        capeRight.setScaleX(1.55f);
                        capeRight.setScaleY(1.2f);
                        capeRight.setScaleZ(1.4f);
                    }
                }
            }
        }
    }

    @Override
    protected void applyBaseTransformations(HumanoidModel<?> baseModel) {
        super.applyBaseTransformations(baseModel);

        GeoBone cape = this.model.getBone("Cape").orElse(null);
        GeoBone capeLeft = this.model.getBone("capeLeft").orElse(null);
        GeoBone capeRight = this.model.getBone("capeRight").orElse(null);

        if (this.currentEntity instanceof ArmorStand) {
            if (cape != null) {
                ModelPart bodyPart = baseModel.body;
                RenderUtils.matchModelPartRot(bodyPart, cape);
                cape.updatePosition(bodyPart.x, -bodyPart.y + 1.5f, bodyPart.z - 1f);
            }

            if (capeLeft != null) {
                ModelPart leftArmPart = baseModel.leftArm;
                RenderUtils.matchModelPartRot(leftArmPart, capeLeft);
                capeLeft.updatePosition(leftArmPart.x - 5.7f, 2.32f - leftArmPart.y, leftArmPart.z);
            }

            if (capeRight != null) {
                ModelPart rightArmPart = baseModel.rightArm;
                RenderUtils.matchModelPartRot(rightArmPart, capeRight);
                capeRight.updatePosition(rightArmPart.x + 5.7f, 2.32f - rightArmPart.y, rightArmPart.z);
            }
        } else {
            if (cape != null) {
                ModelPart bodyPart = baseModel.body;
                RenderUtils.matchModelPartRot(bodyPart, cape);
                cape.updatePosition(bodyPart.x, -bodyPart.y + 1.125f, bodyPart.z - 1f);
            }

            if (capeLeft != null) {
                ModelPart leftArmPart = baseModel.leftArm;
                RenderUtils.matchModelPartRot(leftArmPart, capeLeft);
                capeLeft.updatePosition(leftArmPart.x - 5f, 2f - leftArmPart.y, leftArmPart.z);
            }

            if (capeRight != null) {
                ModelPart rightArmPart = baseModel.rightArm;
                RenderUtils.matchModelPartRot(rightArmPart, capeRight);
                capeRight.updatePosition(rightArmPart.x + 5f, 2f - rightArmPart.y, rightArmPart.z);
            }
        }
    }

    @Override
    protected void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        setAllVisible(false);

        if (currentSlot == EquipmentSlot.CHEST) {
            setBoneVisible(this.model.getBone("Cape").orElse(null), true);
            setBoneVisible(this.model.getBone("CapeMiddle").orElse(null), true);
            setBoneVisible(this.model.getBone("CapeLower").orElse(null), true);
            setBoneVisible(this.model.getBone("capeLeft").orElse(null), true);
            setBoneVisible(this.model.getBone("capeRight").orElse(null), true);
        }
    }
}