package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.gui.AttackWheelGui;
import com.xirc.nichirin.client.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.common.network.BreathingMovePacket;
import com.xirc.nichirin.common.util.enums.MoveClass;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Handles the attack wheel input and display
 */
public class AttackWheelHandler {

    private static boolean wasKeyDown = false;
    private static AttackWheelGui currentWheel = null;

    public static void register() {
        // Register tick handler
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.player == null) return;

            boolean isKeyDown = NichirinKeybindRegistry.ATTACK_WHEEL_KEY.isDown();

            // Key pressed - open wheel
            if (isKeyDown && !wasKeyDown) {
                openWheel();
            }
            // Key released - execute move and close wheel
            else if (!isKeyDown && wasKeyDown) {
                closeWheel();
            }

            wasKeyDown = isKeyDown;
        });
    }

    /**
     * Opens the attack wheel
     */
    private static void openWheel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        currentWheel = new AttackWheelGui();
        mc.setScreen(currentWheel);
    }

    /**
     * Closes the wheel and executes selected move
     */
    private static void closeWheel() {
        if (currentWheel == null) return;

        // Get selected move
        MoveClass selectedMove = currentWheel.getSelectedMove();

        // Close the GUI
        Minecraft.getInstance().setScreen(null);
        currentWheel = null;

        // Execute the move if one was selected
        if (selectedMove != null) {
            executeMove(selectedMove);
        }
    }

    /**
     * Sends packet to server to execute the selected move
     */
    private static void executeMove(MoveClass moveClass) {
        // Create and send the breathing move packet
        BreathingMovePacket packet = new BreathingMovePacket(moveClass, true); // true = pressed/execute

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.toBytes(buf);

        NetworkManager.sendToServer(NichirinPacketRegistry.BREATHING_MOVE_ID, buf);
    }
}