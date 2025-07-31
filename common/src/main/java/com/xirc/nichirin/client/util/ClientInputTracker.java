package com.xirc.nichirin.client.util;

import com.xirc.nichirin.common.network.MovementInputSyncPacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Tracks and syncs input states to server
 */
@Environment(EnvType.CLIENT)
public class ClientInputTracker {

    private static boolean lastForward = false;
    private static boolean lastBackward = false;
    private static boolean lastLeft = false;
    private static boolean lastRight = false;
    private static boolean lastJump = false;

    /**
     * Check input states and sync to server if changed
     * Call this from your client tick handler
     */
    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;

        if (player == null || client.options == null) {
            return;
        }

        // Read current input states
        boolean forward = client.options.keyUp.isDown();
        boolean backward = client.options.keyDown.isDown();
        boolean left = client.options.keyLeft.isDown();
        boolean right = client.options.keyRight.isDown();
        boolean jump = client.options.keyJump.isDown();

        // Check if any input changed
        if (forward != lastForward || backward != lastBackward ||
                left != lastLeft || right != lastRight || jump != lastJump) {

            // Send updated input state to server
            MovementInputSyncPacket packet = new MovementInputSyncPacket(forward, backward, left, right, jump);
            NichirinPacketRegistry.sendToServer(packet);

            // Update last known state
            lastForward = forward;
            lastBackward = backward;
            lastLeft = left;
            lastRight = right;
            lastJump = jump;
        }
    }
}