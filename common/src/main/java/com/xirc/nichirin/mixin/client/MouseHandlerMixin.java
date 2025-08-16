package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void invertMouseMovement(net.minecraft.client.player.LocalPlayer player, double deltaX, double deltaY) {
        // Check if player has disoriented effect
        if (Minecraft.getInstance().player != null &&
                Minecraft.getInstance().player.hasEffect(NichirinEffectRegistry.DISORIENTED.get())) {

            // Invert both mouse movements
            player.turn(-deltaX, -deltaY);
        } else {
            // Normal movement
            player.turn(deltaX, deltaY);
        }
    }
}