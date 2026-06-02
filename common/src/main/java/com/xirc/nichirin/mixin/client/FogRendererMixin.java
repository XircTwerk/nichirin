package com.xirc.nichirin.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.xirc.nichirin.client.handler.BloodMoonClientState;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tints the sky fog a deep blood-red during a blood moon.
 * FogRenderer.setupColor is the last place the horizon/sky fog colour is set each frame,
 * so overriding it here guarantees the colour sticks.
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {

    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void nichirin$bloodMoonFogColor(
            Camera camera, float partialTick, ClientLevel level,
            int renderDistance, float darknessScale, CallbackInfo ci) {
        if (BloodMoonClientState.isActive()) {
            // Deep crimson horizon fog — bleeds into the sky gradient around the moon
            RenderSystem.setShaderFogColor(0.35F, 0.02F, 0.02F, 1.0F);
        }
    }
}