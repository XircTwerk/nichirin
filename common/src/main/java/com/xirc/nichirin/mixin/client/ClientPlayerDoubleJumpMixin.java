package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.common.system.slayerabilities.PlayerDoubleJump;
import com.xirc.nichirin.common.util.StaminaManager;
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

        // DO NOT TICK PLAYER STATE ON CLIENT - Let server handle it
        // PlayerDoubleJump.tickPlayer(player); // REMOVED!

        // Reduce jump cooldown
        if (nichirin$jumpCooldown > 0) {
            nichirin$jumpCooldown--;
        }

        // Current jump state
        boolean isJumping = player.input.jumping;

        // Detect jump key press (rising edge detection)
        boolean jumpPressed = isJumping && !nichirin$wasJumping;

        // CRITICAL: Only process if NOT on ground, cooldown ready, and jump was pressed
        if (jumpPressed && nichirin$jumpCooldown == 0) {
            System.out.println("=== CLIENT JUMP PRESS DETECTED ===");
            System.out.println("Player on ground: " + player.onGround());
            System.out.println("Current stamina: " + StaminaManager.getStamina(player) + "/" + StaminaManager.getMaxStamina(player));
            System.out.println("Stamina cost: " + PlayerDoubleJump.getStaminaCost());

            // STRICT CHECK: Must not be on ground
            if (player.onGround()) {
                System.out.println("CLIENT: Jump pressed but player is on ground - ignoring");
            } else if (!StaminaManager.hasStamina(player, PlayerDoubleJump.getStaminaCost())) {
                System.out.println("CLIENT: Not enough stamina for double jump - need " + PlayerDoubleJump.getStaminaCost() + ", have " + StaminaManager.getStamina(player));
            } else if (PlayerDoubleJump.canDoubleJump(player)) {
                System.out.println("CLIENT: Can double jump - sending packet to server");

                // Set cooldown to prevent spam
                nichirin$jumpCooldown = 10;

                // DO NOT PERFORM DOUBLE JUMP ON CLIENT
                // Just send packet to server and let server handle everything
                NichirinPacketRegistry.sendToServer(new DoubleJumpPacket());

                // Optionally: Add immediate visual feedback without modifying state
                // PlayerDoubleJump.playClientSideEffectsOnly(player);
            } else {
                System.out.println("CLIENT: Cannot double jump - already used or other restriction");
            }
        }

        // Update state tracking for next tick
        nichirin$wasJumping = isJumping;
    }
}