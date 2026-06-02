package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.common.item.katana.SimpleKatana;
import com.xirc.nichirin.common.network.c2s.BreathingMovePacket;
import com.xirc.nichirin.common.network.c2s.DemonMovePacket;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Handles multiplayer input for katanas, breathing moves, and demon arts
 * Interaction blocking is handled by InputHandler
 */
public class MultiplayerInputHandler {

    // Network packet IDs
    private static final ResourceLocation ATTACK_WHEEL_STATE_PACKET = ResourceLocation.fromNamespaceAndPath("nichirin", "attack_wheel_state");
    private static final ResourceLocation KATANA_INPUT_PACKET = ResourceLocation.fromNamespaceAndPath("nichirin", "katana_input");
    private static final ResourceLocation DEMON_INPUT_PACKET = ResourceLocation.fromNamespaceAndPath("nichirin", "demon_input");

    // Client-side state
    private static volatile boolean clientWheelOpen = false;
    private static volatile long clientWheelCloseTime = 0;

    public static class PlayerInputState {
        public boolean attackWheelOpen = false;
        public long wheelCloseTime = 0;
        public boolean inputBlocked = false;
        public long blockUntilTime = 0;

        public boolean shouldBlockInput(long currentTime) {
            if (attackWheelOpen) return true;
            if (inputBlocked) {
                if (currentTime < blockUntilTime) return true;
                inputBlocked = false;
                blockUntilTime = 0;
            }
            if (wheelCloseTime > 0) {
                if (currentTime - wheelCloseTime < 20) return true;
                wheelCloseTime = 0;
            }
            return false;
        }

        public void clearBlock() {
            attackWheelOpen = false;
            wheelCloseTime = 0;
            inputBlocked = false;
            blockUntilTime = 0;
        }
    }

    public enum InputType {
        LEFT_CLICK, RIGHT_CLICK, RIGHT_CLICK_CROUCH
    }

    /**
     * CLIENT: Set attack wheel state
     */
    public static void setAttackWheelOpen(boolean open, Player player) {
        if (player.level().isClientSide) {
            clientWheelOpen = open;
            if (!open) {
                clientWheelCloseTime = System.currentTimeMillis();
            }

            try {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeBoolean(open);
                NetworkManager.sendToServer(ATTACK_WHEEL_STATE_PACKET, NetworkBufferUtils.client(buf));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * CLIENT: Check if inputs should be blocked
     */
    public static boolean shouldBlockInputsClient() {
        if (clientWheelOpen) {
            return true;
        }

        if (clientWheelCloseTime > 0) {
            long elapsed = System.currentTimeMillis() - clientWheelCloseTime;
            if (elapsed < 100) {
                return true;
            }
        }

        return false;
    }

    /**
     * CLIENT: Send input to server (interaction checking removed - handled in ClientInputHandler)
     */
    public static void sendInput(InputType inputType, Player player) {
        if (player.level().isClientSide) {
            if (shouldBlockInputsClient()) {
                return;
            }

            boolean holdingKatana = player.getMainHandItem().getItem() instanceof SimpleKatana;

            if (holdingKatana) {
                try {
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                    buf.writeEnum(inputType);
                    NetworkManager.sendToServer(KATANA_INPUT_PACKET, NetworkBufferUtils.client(buf));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                sendDemonInput(inputType, player);
            }
        }
    }

    public static void sendDemonInput(InputType inputType, Player player) {
        if (!player.level().isClientSide || !MovesetHelper.hasDemonMoveset(player)) return;

        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(inputType.name());
            NetworkManager.sendToServer(DEMON_INPUT_PACKET, NetworkBufferUtils.client(buf));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * CLIENT: Send katana input (legacy compatibility)
     */
    public static void sendKatanaInput(InputType inputType, Player player) {
        sendInput(inputType, player);
    }

    /**
     * CLIENT: Send breathing move
     */
    public static void sendBreathingMove(int moveIndex, Player player) {
        if (player.level().isClientSide) {
            try {
                var packet = new BreathingMovePacket(moveIndex, true);
                NichirinPacketRegistry.sendToServer(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * CLIENT: Send demon move
     */
    public static void sendDemonMove(int moveIndex, Player player) {
        if (player.level().isClientSide) {
            try {
                var packet = new DemonMovePacket(moveIndex, true);
                NichirinPacketRegistry.sendToServer(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * SERVER: Check if player inputs should be blocked
     */
    public static boolean shouldBlockInputsServer(Player player) {
        return NichirinPacketRegistry.shouldBlockInputsServer(player);
    }

    /**
     * SERVER: Block inputs after move execution
     */
    public static void blockInputsAfterMoveExecution(Player player) {
        if (!player.level().isClientSide) {
            PlayerInputState state = NichirinPacketRegistry.getOrCreatePlayerState(player);
            state.inputBlocked = true;
            state.blockUntilTime = player.level().getGameTime() + 40;
        }
    }

    public static void clearInputBlock(Player player) {
        if (!player.level().isClientSide) {
            NichirinPacketRegistry.clearInputBlock(player);
        }
    }

    /**
     * @deprecated Use blockInputsAfterMoveExecution instead
     */
    @Deprecated
    public static void blockInputsAfterBreathingMove(Player player) {
        blockInputsAfterMoveExecution(player);
    }

    /**
     * Clean up player data
     */
    public static void cleanupPlayer(Player player) {
        NichirinPacketRegistry.cleanupPlayer(player);
    }

    public static void tick() {
        // No longer needed
    }
}