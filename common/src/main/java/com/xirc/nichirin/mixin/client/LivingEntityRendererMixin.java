package com.xirc.nichirin.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.client.animation.NichirinAnimations;
import com.zigythebird.playeranimcore.api.firstPerson.FirstPersonMode;
import net.minecraft.client.Minecraft;
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

/**
 * Cancels armor rendering on invisible entities so MobEffects.INVISIBILITY
 * truly hides all visuals (vanilla only fades the body model; layers always render).
 */
@Mixin(HumanoidArmorLayer.class)
public class LivingEntityRendererMixin<T extends LivingEntity, M extends HumanoidModel<T>, A extends HumanoidModel<T>> {

    @Inject(method = "renderArmorPiece", at = @At("HEAD"), cancellable = true)
    private void nichirin$skipArmorWhenInvisible(PoseStack poseStack, MultiBufferSource buffer,
                                                  T entity, EquipmentSlot slot, int packedLight,
                                                  A model, CallbackInfo ci) {
        if (entity.isInvisible()) {
            ci.cancel();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (entity == minecraft.player
                && entity instanceof AbstractClientPlayer player
                && NichirinAnimations.isAnimationPlaying(player)
                && FirstPersonMode.isFirstPersonPass()) {
            ci.cancel();
        }
    }
}