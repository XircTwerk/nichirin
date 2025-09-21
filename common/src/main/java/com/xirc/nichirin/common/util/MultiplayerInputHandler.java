package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.network.c2s.BreathingMovePacket;
import com.xirc.nichirin.common.network.c2s.DemonMovePacket;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DEBUG VERSION: Handles all multiplayer input for katanas, breathing moves, and demon arts
 * Enhanced with extensive logging to debug multiplayer issues
 */
public class MultiplayerInputHandler {

    // Network packet IDs - UPDATED to match NichirinPacketRegistry
    private static final ResourceLocation ATTACK_WHEEL_STATE_PACKET = new ResourceLocation("nichirin", "attack_wheel_state");
    private static final ResourceLocation KATANA_INPUT_PACKET = new ResourceLocation("nichirin", "katana_input");
    private static final ResourceLocation DEMON_INPUT_PACKET = new ResourceLocation("nichirin", "demon_input");

    // Client-side state (for UI only, not authoritative)
    private static volatile boolean clientWheelOpen = false;
    private static volatile long clientWheelCloseTime = 0;

    /**
     * Player input state tracked server-side (now handled in NichirinPacketRegistry)
     */
    public static class PlayerInputState {
        public boolean attackWheelOpen = false;
        public long wheelCloseTime = 0;
        public boolean inputBlocked = false;
        public long blockUntilTime = 0;

        public boolean shouldBlockInput(long currentTime) {
            if (attackWheelOpen) return true;
            if (inputBlocked && currentTime < blockUntilTime) return true;
            if (wheelCloseTime > 0 && currentTime - wheelCloseTime < 20) return true; // 1 second grace period (20 ticks)
            return false;
        }
    }

    /**
     * Input types for packet communication
     */
    public enum InputType {
        LEFT_CLICK, RIGHT_CLICK, RIGHT_CLICK_CROUCH
    }

    /**
     * CLIENT: Set attack wheel state
     */
    public static void setAttackWheelOpen(boolean open, Player player) {

        if (player.level().isClientSide) {
            // Update client state immediately for responsive UI
            clientWheelOpen = open;
            if (!open) {
                clientWheelCloseTime = System.currentTimeMillis();
            }

            try {
                // Send state to server
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeBoolean(open);
                NetworkManager.sendToServer(ATTACK_WHEEL_STATE_PACKET, buf);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * CLIENT: Check if inputs should be blocked
     */
    public static boolean shouldBlockInputsClient() {
        // Simple rule: Block ALL inputs when wheel is open, period.
        if (clientWheelOpen) {
            // Only log when first blocked, not every check
            return true;
        }

        // Brief grace period after wheel closes to prevent click leakage
        if (clientWheelCloseTime > 0) {
            long elapsed = System.currentTimeMillis() - clientWheelCloseTime;
            if (elapsed < 500) { // Reduced to 500ms
                // Only log when first blocked, not every check
                return true;
            }
        }

        return false;
    }

    /**
     * FIXED: CLIENT: Send katana/demon input to server
     */
    public static void sendInput(InputType inputType, Player player) {
        if (player.level().isClientSide) {
            // Check client-side block first (immediate feedback)
            if (shouldBlockInputsClient()) {
                return;
            }

            // PRIORITY: Check held item first, then movesets
            boolean holdingKatana = player.getMainHandItem().getItem() instanceof com.xirc.nichirin.common.item.katana.SimpleKatana;

            if (holdingKatana) {
                // Holding katana = ALWAYS use katana input (breathing techniques)
                boolean hasBreathing = MovesetHelper.hasBreathingMoveset(player);
                String breathingId = MovesetHelper.getBreathingMovesetId(player);

                try {
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                    buf.writeEnum(inputType);
                    NetworkManager.sendToServer(KATANA_INPUT_PACKET, buf);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                // Not holding katana = check for demon moveset
                boolean hasDemon = MovesetHelper.hasDemonMoveset(player);
                String demonId = MovesetHelper.getDemonMovesetId(player);

                if (hasDemon) {
                    try {
                        // Send demon input packet
                        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                        buf.writeUtf(inputType.name());
                        NetworkManager.sendToServer(DEMON_INPUT_PACKET, buf);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // FIXED: No demon moveset and no katana = do nothing silently
                    return;
                }
            }
        }
    }

    /**
     * CLIENT: Send katana input to server (legacy method)
     */
    public static void sendKatanaInput(InputType inputType, Player player) {
        sendInput(inputType, player); // Redirect to unified method
    }

    /**
     * CLIENT: Send breathing move to server (use existing system)
     */
    public static void sendBreathingMove(int moveIndex, Player player) {
        if (player.level().isClientSide) {
            try {
                // Use the existing BreathingMovePacket system that already works
                var packet = new BreathingMovePacket(moveIndex, true);
                com.xirc.nichirin.registry.NichirinPacketRegistry.sendToServer(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * CLIENT: Send demon move to server (use same packet system as breathing)
     */
    public static void sendDemonMove(int moveIndex, Player player) {

        if (player.level().isClientSide) {
            try {
                // Use the DemonMovePacket system (similar to BreathingMovePacket)
                var packet = new DemonMovePacket(moveIndex, true);
                com.xirc.nichirin.registry.NichirinPacketRegistry.sendToServer(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * SERVER: Check if player inputs should be blocked
     * Now delegated to NichirinPacketRegistry
     */
    public static boolean shouldBlockInputsServer(Player player) {
        boolean blocked = com.xirc.nichirin.registry.NichirinPacketRegistry.shouldBlockInputsServer(player);
        if (blocked) {
        }
        return blocked;
    }

    /**
     * SERVER: Block inputs after move execution (breathing or demon)
     * Now delegated to NichirinPacketRegistry
     */
    public static void blockInputsAfterMoveExecution(Player player) {
        if (!player.level().isClientSide) {
            PlayerInputState state = com.xirc.nichirin.registry.NichirinPacketRegistry.getOrCreatePlayerState(player);
            state.inputBlocked = true;
            state.blockUntilTime = player.level().getGameTime() + 40; // 2 seconds (40 ticks)
        }
    }

    /**
     * Legacy method for backwards compatibility
     * @deprecated Use blockInputsAfterMoveExecution instead
     */
    @Deprecated
    public static void blockInputsAfterBreathingMove(Player player) {
        blockInputsAfterMoveExecution(player);
    }

    /**
     * Clean up when player leaves server
     */
    public static void cleanupPlayer(Player player) {
        com.xirc.nichirin.registry.NichirinPacketRegistry.cleanupPlayer(player);
    }

    /**
     * Periodic cleanup of old states (no longer needed, handled by server)
     */
    public static void tick() {
        // No longer needed - server handles cleanup
    }
}