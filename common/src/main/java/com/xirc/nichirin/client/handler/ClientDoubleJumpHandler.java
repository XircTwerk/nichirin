package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.common.system.slayerabilities.PlayerDoubleJump;
import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.common.network.DoubleJumpPacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public class ClientDoubleJumpHandler {

    private static boolean wasJumping = false;
    private static int jumpCooldown = 0;

    public static void register() {
        ClientTickEvent.CLIENT_POST.register(ClientDoubleJumpHandler::onClientTick);
    }

    private static void onClientTick(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        if (player == null) return;

        // Reduce jump cooldown
        if (jumpCooldown > 0) {
            jumpCooldown--;
        }

        // Current jump state
        boolean isJumping = player.input.jumping;

        // Detect jump key press (rising edge detection)
        boolean jumpPressed = isJumping && !wasJumping;

        // CRITICAL: Only process if NOT on ground, cooldown ready, and jump was pressed
        if (jumpPressed && jumpCooldown == 0) {

            // STRICT CHECK: Must not be on ground
            if (player.onGround()) {
                // Do nothing
            } else if (!StaminaManager.hasStamina(player, PlayerDoubleJump.getStaminaCost())) {
                // Do nothing
            } else if (PlayerDoubleJump.canDoubleJump(player)) {

                // Set cooldown to prevent spam
                jumpCooldown = 10;

                // Send packet to server
                NichirinPacketRegistry.sendToServer(new DoubleJumpPacket());
            }
        }

        // Update state tracking for next tick
        wasJumping = isJumping;
    }
}