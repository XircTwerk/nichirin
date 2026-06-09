package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.client.afterimage.AfterimageRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererAfterimageMixin<T extends Entity> {
    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void nichirin$hideAfterimageName(T entity, CallbackInfoReturnable<Boolean> cir) {
        if (AfterimageRenderState.isRendering()) {
            cir.setReturnValue(false);
        }
    }
}
