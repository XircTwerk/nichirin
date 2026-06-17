package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.common.item.gun.GenyaDB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class GunEquipAnimMixin {

    @Shadow private float mainHandHeight;
    @Shadow private float oMainHandHeight;

    @Inject(method = "tick", at = @At("TAIL"))
    private void nichirin$keepGunAtRest(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.player.getMainHandItem().getItem() instanceof GenyaDB) {
            oMainHandHeight = 1.0f;
            mainHandHeight = 1.0f;
        }
    }
}
