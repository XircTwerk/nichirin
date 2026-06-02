package com.xirc.nichirin.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.xirc.nichirin.common.system.sheathing.SheathingManager;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public class ItemInHandLayerMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void nichirin$skipHeldItemsWhenInvisible(PoseStack poseStack, MultiBufferSource buffer,
                                                      int packedLight, T entity,
                                                      float limbSwing, float limbSwingAmount,
                                                      float partialTick, float ageInTicks,
                                                      float netHeadYaw, float headPitch,
                                                      CallbackInfo ci) {
        if (entity.isInvisible()) {
            ci.cancel();
            return;
        }
        if (entity instanceof Player player && SheathingManager.isSelectedKatanaSheathed(player)) {
            ci.cancel();
        }
    }
}