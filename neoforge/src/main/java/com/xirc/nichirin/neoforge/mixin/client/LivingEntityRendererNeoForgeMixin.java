package com.xirc.nichirin.neoforge.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public class LivingEntityRendererNeoForgeMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> {

    @Inject(
            method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void nichirin$skipNeoForgeArmorWhenInvisible(PoseStack poseStack, MultiBufferSource buffer,
                                                         T entity, EquipmentSlot slot, int packedLight,
                                                         A model, float limbSwing, float limbSwingAmount,
                                                         float partialTick, float ageInTicks,
                                                         float netHeadYaw, float headPitch, CallbackInfo ci) {
        if (entity.isInvisible()) {
            ci.cancel();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (entity == minecraft.player
                && entity instanceof AbstractClientPlayer
                && (FirstPersonMode.isFirstPersonPass()
                || minecraft.options.getCameraType() == CameraType.FIRST_PERSON)) {
            ci.cancel();
        }
    }
}
