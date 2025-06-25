package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.common.system.slayerabilities.PlayerDoubleJump;
import com.xirc.nichirin.common.network.DoubleJumpPacket;
import com.xirc.nichirin.common.registry.NichirinPacketRegistry;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side mixin to detect jump input for double jumping
 */
@Mixin(LocalPlayer.class)
public class ClientPlayerDoubleJumpMixin {

    @Unique
    private boolean nichirin$wasJumping = false;
    @Unique
    private int nichirin$jumpCooldown = 0;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onClientTick(CallbackInfo ci) {
        LocalPlayer player = (LocalPlayer) (Object) this;

        // Update player state in the double jump system FIRST
        PlayerDoubleJump.tickPlayer(player);

        // Reduce jump cooldown
        if (nichirin$jumpCooldown > 0) {
            nichirin$jumpCooldown--;
        }

        // Detect jump key press (rising edge detection)
        boolean jumpPressed = player.input.jumping && !nichirin$wasJumping;

        // Only process jump if not on ground and cooldown is ready
        if (jumpPressed && !player.onGround() && nichirin$jumpCooldown == 0) {

            // Only process jump if not on ground and cooldown is ready
            if (jumpPressed && !player.onGround() && nichirin$jumpCooldown == 0) {
                // Check if we can double jump
                if (PlayerDoubleJump.canDoubleJump(player)) {
                    // Set cooldown
                    nichirin$jumpCooldown = 5;

                    // Perform double jump
                    PlayerDoubleJump.tryDoubleJump(player);

                    // Send packet to server
                    if (player.level().isClientSide) {
                        NichirinPacketRegistry.sendToServer(new DoubleJumpPacket());
                    }
                }
            }

            // Update state tracking
            nichirin$wasJumping = player.input.jumping;
        }
    }
}