package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.gui.AttackWheelOverlay;
import com.xirc.nichirin.client.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.common.network.BreathingMovePacket;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.data.BreathingStyleHelper;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * Handles the attack wheel input and display using HUD overlay
 */
public class AttackWheelHandler {

    private static boolean wasKeyDown = false;
    private static AttackWheelOverlay currentWheel = null;
    @Getter
    private static boolean wheelOpen = false;

    public static void register() {
        System.out.println("DEBUG: AttackWheelHandler.register() called!");

        // Create the overlay instance
        currentWheel = new AttackWheelOverlay();

        // Register tick handler
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.player == null) return;

            boolean isKeyDown = NichirinKeybindRegistry.ATTACK_WHEEL_KEY.isDown();

            // Key just pressed (not held)
            if (isKeyDown && !wasKeyDown) {
                System.out.println("DEBUG: Attack wheel key pressed. Wheel open: " + wheelOpen);
                if (!wheelOpen) {
                    openWheel();
                } else {
                    forceCloseWheel();
                }
            }

            wasKeyDown = isKeyDown;
        });

        // Register HUD render event
        ClientGuiEvent.RENDER_HUD.register((guiGraphics, partialTick) -> {
            if (currentWheel != null && currentWheel.isActive()) {
                currentWheel.render(guiGraphics);
            }
        });

        // Register mouse click handler
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (wheelOpen && client.options.keyAttack.isDown()) {
                // Left click while wheel is open
                if (currentWheel != null && currentWheel.isMouseOverWheel()) {
                    executeAndCloseWheel();
                }
            }
        });
    }

    private static void openWheel() {
        System.out.println("DEBUG: openWheel() called!");
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            System.out.println("DEBUG: Player is null");
            return;
        }

        // Check if player is holding a SimpleKatana
        ItemStack mainHand = mc.player.getMainHandItem();
        if (!(mainHand.getItem() instanceof SimpleKatana)) {
            System.out.println("DEBUG: Not holding SimpleKatana - item: " + mainHand.getItem().getClass().getSimpleName());
            return;
        }

        // Check if player has a breathing style selected
        if (!BreathingStyleHelper.hasMoveset(mc.player)) {
            System.out.println("DEBUG: No breathing style selected");
            return;
        }

        // Don't open if a screen is open (like inventory)
        if (mc.screen != null) {
            System.out.println("DEBUG: Another screen is already open: " + mc.screen.getClass().getSimpleName());
            return;
        }

        System.out.println("DEBUG: Activating wheel...");
        if (currentWheel != null) {
            currentWheel.activate();
            wheelOpen = true;
            System.out.println("DEBUG: Wheel activated successfully!");
        } else {
            System.out.println("DEBUG: currentWheel is null!");
        }
    }

    public static void executeAndCloseWheel() {
        if (currentWheel == null || !wheelOpen) return;

        int selectedMove = currentWheel.deactivate();
        wheelOpen = false;

        if (selectedMove != -1) {
            executeMove(selectedMove);
        }
    }

    public static void forceCloseWheel() {
        System.out.println("DEBUG: forceCloseWheel() called");
        if (currentWheel != null) {
            currentWheel.deactivate();
        }
        wheelOpen = false;
    }

    private static void executeMove(int moveIndex) {
        BreathingMovePacket packet = new BreathingMovePacket(moveIndex, true);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.toBytes(buf);
        NetworkManager.sendToServer(NichirinPacketRegistry.BREATHING_MOVE_ID, buf);
    }
}