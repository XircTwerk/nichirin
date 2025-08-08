package com.xirc.nichirin.client.renderer.armor;

import com.xirc.nichirin.mixin.client.PlayerModelAccessor;
import mod.azure.azurelib.animatable.GeoItem;
import mod.azure.azurelib.cache.object.GeoBone;
import mod.azure.azurelib.model.GeoModel;
import mod.azure.azurelib.renderer.GeoArmorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class NichirinArmorRenderer<T extends Item & GeoItem> extends GeoArmorRenderer<T> {

    public NichirinArmorRenderer(final GeoModel<T> model) {
        super(model);
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
    public GeoBone getRightArmBone() {
        return this.model.getBone("rightArm").orElse(super.getRightArmBone());
    }

    @Nullable
    @Override
    public GeoBone getLeftArmBone() {
        return this.model.getBone("leftArm").orElse(super.getLeftArmBone());
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

        if (!(entity instanceof AbstractClientPlayer player)) return;
        EntityRenderer<? super AbstractClientPlayer> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);

        if (!(renderer instanceof PlayerRenderer playerRenderer)) return;
        PlayerModel<AbstractClientPlayer> playerModel = playerRenderer.getModel();

        if (!((PlayerModelAccessor) playerModel).isSlim())
            return;

        GeoBone leftArm = getLeftArmBone();
        GeoBone rightArm = getRightArmBone();

        if (leftArm != null && rightArm != null) {
            leftArm.setScaleX(0.75f);
            rightArm.setScaleX(0.75f);
        }

        // Also scale cape arms if they exist
        GeoBone capeLeft = this.model.getBone("capeLeft").orElse(null);
        GeoBone capeRight = this.model.getBone("capeRight").orElse(null);

        if (capeLeft != null) capeLeft.setScaleX(0.75f);
        if (capeRight != null) capeRight.setScaleX(0.75f);
    }
}