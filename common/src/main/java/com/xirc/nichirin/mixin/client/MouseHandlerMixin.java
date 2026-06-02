package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.client.player.LocalPlayer;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void invertMouseMovement(LocalPlayer player, double deltaX, double deltaY) {
        // Check if player has disoriented effect
        if (Minecraft.getInstance().player != null) {
            MobEffectInstance disorientedEffect = Minecraft.getInstance().player.getEffect(NichirinEffectRegistry.disoriented());

            if (disorientedEffect != null) {
                int amplifier = disorientedEffect.getAmplifier();

                // Tier 2 (amplifier 1) or Tier 3 (amplifier 2) - invert mouse movement
                if (amplifier == 1 || amplifier == 2) {
                    // Invert both mouse movements
                    player.turn(-deltaX, -deltaY);
                } else {
                    // Normal movement for Tier 1 (amplifier 0)
                    player.turn(deltaX, deltaY);
                }
            } else {
                // Normal movement when no effect
                player.turn(deltaX, deltaY);
            }
        } else {
            // Fallback normal movement
            player.turn(deltaX, deltaY);
        }
    }
}