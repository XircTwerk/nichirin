package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.common.system.abilities.PlayerDoubleJump;
import com.xirc.nichirin.common.system.abilities.PlayerWallJump;
import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.common.network.c2s.DoubleJumpPacket;
import com.xirc.nichirin.common.network.c2s.WallJumpPacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

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

        // Wall jump: pressing jump mid-air right next to a block bounces off it instead of double
        // jumping. Checked outside the double-jump cooldown gate — it has no cooldown, doesn't
        // consume the double jump, and the rising-edge key detection is the only rate limit.
        if (jumpPressed && !player.onGround()
                && PlayerWallJump.canWallJump(player)
                && !PlayerWallJump.findAwayFromWall(player).equals(Vec3.ZERO)) {
            NichirinPacketRegistry.sendToServer(new WallJumpPacket());
        } else if (jumpPressed && jumpCooldown == 0) {

            if (player.onGround()) {
                // Do nothing
            } else if (!StaminaManager.hasStamina(player, PlayerDoubleJump.getStaminaCost())) {
                // Do nothing
            } else if (PlayerDoubleJump.canDoubleJump(player)) {

                // Set cooldown to prevent spam
                jumpCooldown = 10;

                boolean forward = player.input.up;
                boolean backward = player.input.down;
                boolean left = player.input.left;
                boolean right = player.input.right;

                // Send packet to server WITH movement input
                DoubleJumpPacket packet = new DoubleJumpPacket(forward, backward, left, right);
                NichirinPacketRegistry.sendToServer(packet);
            }
        }

        // Update state tracking for next tick
        wasJumping = isJumping;
    }
}