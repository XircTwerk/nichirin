package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void invertControlsWhenDisoriented(boolean slowDown, float friction, CallbackInfo ci) {
        // Cast this to Input since KeyboardInput extends Input
        Input input = (Input) (Object) this;

        // Get the current player
        if (Minecraft.getInstance().player != null) {
            MobEffectInstance disorientedEffect = Minecraft.getInstance().player.getEffect(NichirinEffectRegistry.disoriented());

            if (disorientedEffect != null) {
                int amplifier = disorientedEffect.getAmplifier();

                // Tier 1 (amplifier 0) or Tier 3 (amplifier 2) - invert movement
                if (amplifier == 0 || amplifier == 2) {
                    // Store the original input values before modifying
                    float originalForwardImpulse = input.forwardImpulse;
                    float originalLeftImpulse = input.leftImpulse;
                    boolean originalUp = input.up;
                    boolean originalDown = input.down;
                    boolean originalLeft = input.left;
                    boolean originalRight = input.right;

                    // Invert the movement impulses (float values)
                    input.forwardImpulse = -originalForwardImpulse;  // Forward becomes backward
                    input.leftImpulse = -originalLeftImpulse;        // Left becomes right

                    // Invert the movement booleans (key press states)
                    input.up = originalDown;      // W key becomes S key behavior
                    input.down = originalUp;      // S key becomes W key behavior
                    input.left = originalRight;   // A key becomes D key behavior
                    input.right = originalLeft;   // D key becomes A key behavior
                }
                // Tier 2 (amplifier 1) only affects mouse, so no movement inversion here
            }
        }
    }
}