package com.xirc.nichirin.common.util;

import com.xirc.nichirin.BreathOfNichirin;
import com.xirc.nichirin.client.animation.NichirinAnimations;
import com.xirc.nichirin.client.handler.AttackWheelHandler;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.item.katana.Katana;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

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
        // Block-capable = holding a katana in either hand, or empty main hand with CQC equipped.
        // Offhand katana counts so you can guard while the main hand is busy.
        if (!canBlock(player)) {
            // No longer able to block but was blocking — stop.
            if (isCurrentlyBlocking) {
                sendBlockStop();
                isCurrentlyBlocking = false;
                blockRetryTicks = 0;
            }
            return;
        }

        // Right mouse (Use Item) is now the block button.
        boolean blockKeyPressed = Minecraft.getInstance().options.keyUse.isDown()
                && Minecraft.getInstance().screen == null;
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

        // A block + left-click special replaces the held block animation while it plays. The server's
        // hasActiveAttacks check doesn't cover self-ticking breathing/demon specials, so re-assert the
        // block hold here on the local player when its animation controller goes idle while guarding.
        boolean animPlaying = player instanceof AbstractClientPlayer clientPlayer
                && NichirinAnimations.isAnimationPlaying(clientPlayer);
        if (serverAcceptedBlock && blockKeyPressed && wasAnimPlayingLastTick && !animPlaying) {
            NichirinAnimations.playAnimation(player, blockAnimation(player));
        }
        wasAnimPlayingLastTick = animPlaying;
    }

    private static boolean wasAnimPlayingLastTick = false;

    private static boolean canBlock(Player player) {
        // The Gyomei ball-and-chain can't guard (for now) — RMB is its attack, not a block.
        if (com.xirc.nichirin.client.gyomei.ClientGyomeiWeaponManager.isEnabled()) return false;
        if (player.getMainHandItem().getItem() instanceof Katana) return true;
        if (player.getOffhandItem().getItem() instanceof Katana) return true;
        return player.getMainHandItem().isEmpty()
                && MovesetHelper.hasFightingMoveset(player);
    }

    private static String blockAnimation(Player player) {
        if (player.getMainHandItem().getItem() instanceof Katana
                && player.getOffhandItem().getItem() instanceof Katana) return "dual_block";
        if (player.getMainHandItem().getItem() instanceof Katana) return "sword.block";
        if (player.getOffhandItem().getItem() instanceof Katana) return "sword.block";
        if (!player.getMainHandItem().isEmpty()) return "sword.block";
        if (!MovesetHelper.hasFightingMoveset(player)) return "sword.block";
        return PlayerDataProvider.getData(player)
                .getCqcPresetData().getStanceAnimation();
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
            BreathOfNichirin.LOGGER.warn("Could not check wheel blocking state", e);
        }

        // Check multiplayer input handler
        try {
            if (MultiplayerInputHandler.shouldBlockInputsClient()) {
                return true;
            }
        } catch (Exception e) {
            BreathOfNichirin.LOGGER.warn("Could not check multiplayer input blocking", e);
        }

        return false;
    }

    // Utility method to check current blocking state
    public static boolean isCurrentlyBlocking() {
        return isCurrentlyBlocking;
    }
}
