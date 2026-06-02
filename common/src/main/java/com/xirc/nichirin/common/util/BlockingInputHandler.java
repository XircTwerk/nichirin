package com.xirc.nichirin.common.util;

import com.xirc.nichirin.client.handler.AttackWheelHandler;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinKeybindRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Handles blocking input with V key
 */
public class BlockingInputHandler {

    // Packet IDs
    private static final ResourceLocation BLOCK_START_ID = ResourceLocation.fromNamespaceAndPath("nichirin", "block_start");
    private static final ResourceLocation BLOCK_STOP_ID = ResourceLocation.fromNamespaceAndPath("nichirin", "block_stop");
    private static final ResourceLocation PARRY_ID = ResourceLocation.fromNamespaceAndPath("nichirin", "parry");

    private static boolean isCurrentlyBlocking = false;
    private static int blockRetryTicks = 0;
    private static final int BLOCK_START_RETRY_TICKS = 5;

    public static void register() {
        // Only register on client side
        if (Platform.getEnvironment() == Env.CLIENT) {
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
                blockRetryTicks = 0;
            }
            return;
        }

        boolean blockKeyPressed = NichirinKeybindRegistry.BLOCK_KEY.isDown();
        boolean serverAcceptedBlock = player.hasEffect(NichirinEffectRegistry.blocking());

        // Handle key press
        if (blockKeyPressed && !isCurrentlyBlocking) {
            // Key just pressed - start blocking
            sendBlockStart();
            isCurrentlyBlocking = true;
            blockRetryTicks = BLOCK_START_RETRY_TICKS;
        } else if (blockKeyPressed && isCurrentlyBlocking && !serverAcceptedBlock) {
            if (blockRetryTicks-- <= 0 && !player.hasEffect(NichirinEffectRegistry.stunned())) {
                sendBlockStart();
                blockRetryTicks = BLOCK_START_RETRY_TICKS;
            }
        } else if (!blockKeyPressed && isCurrentlyBlocking) {
            // Key just released - stop blocking
            sendBlockStop();
            isCurrentlyBlocking = false;
            blockRetryTicks = 0;
        } else if (serverAcceptedBlock) {
            blockRetryTicks = 0;
        }
    }

    private static void sendBlockStart() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        NetworkManager.sendToServer(BLOCK_START_ID, NetworkBufferUtils.client(buf));
    }

    private static void sendBlockStop() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        NetworkManager.sendToServer(BLOCK_STOP_ID, NetworkBufferUtils.client(buf));
    }

    /**
     * Check if inputs should be blocked (same logic as KatanaInputHandler)
     */
    private static boolean isInputBlocked() {
        Minecraft mc = Minecraft.getInstance();

        // Check if player has blocking effect - BLOCK ALL INPUTS
        if (mc.player != null && mc.player.hasEffect(NichirinEffectRegistry.blocking())) {
            return true;
        }

        // Check if player is stunned - BLOCK ALL INPUTS
        if (mc.player != null && mc.player.hasEffect(NichirinEffectRegistry.stunned())) {
            return true;
        }

        // Check wheel state first
        try {
            if (AttackWheelHandler.shouldBlockAttackInputs()) {
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

    // Utility method to check current blocking state
    public static boolean isCurrentlyBlocking() {
        return isCurrentlyBlocking;
    }
}