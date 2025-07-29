package com.xirc.nichirin.common.util;

import com.xirc.nichirin.client.registry.NichirinKeybindRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.xirc.nichirin.common.item.katana.SimpleKatana;

/**
 * Handles blocking input with V key
 */
public class BlockingInputHandler {

    // Packet IDs
    private static final ResourceLocation BLOCK_START_ID = new ResourceLocation("nichirin", "block_start");
    private static final ResourceLocation BLOCK_STOP_ID = new ResourceLocation("nichirin", "block_stop");
    private static final ResourceLocation PARRY_ID = new ResourceLocation("nichirin", "parry");

    private static boolean isCurrentlyBlocking = false;
    // Remove parry-related variables since parrying is now automatic

    public static void register() {
        // Only register on client side
        if (isClientSide()) {
            registerClientEvents();
        }
    }

    private static void registerClientEvents() {
        // Use client tick to check key states
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            if (minecraft.player == null) return;

            handleBlockingInput(minecraft.player);
        });
    }

    private static void handleBlockingInput(Player player) {
        // Check if player has a katana equipped
        ItemStack mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof SimpleKatana)) {
            // If not holding katana but was blocking, stop blocking
            if (isCurrentlyBlocking) {
                sendBlockStop();
                isCurrentlyBlocking = false;
            }
            return;
        }

        boolean blockKeyPressed = NichirinKeybindRegistry.BLOCK_KEY.isDown();

        // Simple state management - don't check for other input blocks when handling blocking
        // Handle key press
        if (blockKeyPressed && !isCurrentlyBlocking) {
            // Key just pressed - start blocking
            sendBlockStart();
            isCurrentlyBlocking = true;
            System.out.println("DEBUG: Block key pressed - starting block with 1-second parry window");
        } else if (!blockKeyPressed && isCurrentlyBlocking) {
            // Key just released - stop blocking
            sendBlockStop();
            isCurrentlyBlocking = false;
            System.out.println("DEBUG: Block key released - stopping block");
        }
    }

    private static void sendBlockStart() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        NetworkManager.sendToServer(BLOCK_START_ID, buf);
    }

    private static void sendBlockStop() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        NetworkManager.sendToServer(BLOCK_STOP_ID, buf);
    }

    /**
     * Check if inputs should be blocked (same logic as KatanaInputHandler)
     */
    private static boolean isInputBlocked() {
        // Check wheel state first
        try {
            if (com.xirc.nichirin.client.handler.AttackWheelHandler.shouldBlockAttackInputs()) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("WARNING: Could not check wheel blocking state: " + e.getMessage());
        }

        // Check multiplayer input handler
        try {
            if (MultiplayerInputHandler.shouldBlockInputsClient()) {
                return true;
            }
        } catch (Exception e) {
            System.out.println("WARNING: Could not check multiplayer input blocking: " + e.getMessage());
        }

        return false;
    }

    private static boolean isClientSide() {
        try {
            Class.forName("net.minecraft.client.Minecraft");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}