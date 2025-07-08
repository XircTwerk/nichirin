package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.gui.AttackWheelGui;
import com.xirc.nichirin.client.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.common.network.BreathingMovePacket;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * Handles the attack wheel input and display
 * Toggle system: Press once to open, press again to execute and close
 */
public class AttackWheelHandler {

    private static boolean wasKeyDown = false;
    private static AttackWheelGui currentWheel = null;
    private static boolean wheelOpen = false;

    public static void register() {
        // Register tick handler
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.player == null) return;

            boolean isKeyDown = NichirinKeybindRegistry.ATTACK_WHEEL_KEY.isDown();

            // Key just pressed
            if (isKeyDown && !wasKeyDown) {
                System.out.println("DEBUG: Attack wheel key pressed. Wheel open: " + wheelOpen);
                if (!wheelOpen) {
                    // First press - open wheel
                    openWheel();
                } else {
                    // Second press - execute and close
                    executeAndCloseWheel();
                }
            }

            wasKeyDown = isKeyDown;
        });
    }

    /**
     * Opens the attack wheel
     */
    private static void openWheel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // Check if player is holding a SimpleKatana
        ItemStack mainHand = mc.player.getMainHandItem();
        if (!(mainHand.getItem() instanceof SimpleKatana)) {
            // Player must be holding a katana to use breathing techniques
            System.out.println("DEBUG: Not holding SimpleKatana");
            return;
        }

        // Check if player has a breathing style selected
        if (!BreathingStyleHelper.hasMoveset(mc.player)) {
            // Could show a message here that they need to select a breathing style
            System.out.println("DEBUG: No breathing style selected");
            return;
        }

        // Don't open if another screen is already open
        if (mc.screen != null) {
            System.out.println("DEBUG: Another screen is already open");
            return;
        }

        System.out.println("DEBUG: Opening attack wheel");
        currentWheel = new AttackWheelGui();
        currentWheel.activate();
        mc.setScreen(currentWheel);
        wheelOpen = true;
    }

    /**
     * Executes the selected move and closes the wheel
     */
    public static void executeAndCloseWheel() {
        if (currentWheel == null || !wheelOpen) return;

        // Get selected move before closing
        int selectedMove = currentWheel.deactivate();

        // Close the GUI
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(null);
        // Grab mouse again for looking around
        mc.mouseHandler.grabMouse();
        wheelOpen = false;

        // Execute the move if one was selected
        if (selectedMove != -1) {
            executeMove(selectedMove);
        }

        // Clear the wheel reference
        currentWheel = null;
    }

    /**
     * Force closes the wheel (e.g., when ESC is pressed)
     */
    public static void forceCloseWheel() {
        if (currentWheel != null) {
            currentWheel.deactivate();
            currentWheel = null;
        }
        wheelOpen = false;
    }

    /**
     * Checks if the wheel is currently open
     */
    public static boolean isWheelOpen() {
        return wheelOpen;
    }

    /**
     * Sends packet to server to execute the selected move
     */
    private static void executeMove(int moveIndex) {
        // Create and send the breathing move packet
        BreathingMovePacket packet = new BreathingMovePacket(moveIndex, true); // true = pressed/execute

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.toBytes(buf);

        NetworkManager.sendToServer(NichirinPacketRegistry.BREATHING_MOVE_ID, buf);
    }
}