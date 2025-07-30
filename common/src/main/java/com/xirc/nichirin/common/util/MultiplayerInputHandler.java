package com.xirc.nichirin.common.util;

import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles all multiplayer input for katanas and breathing moves
 */
public class MultiplayerInputHandler {

    // Network packet IDs
    private static final ResourceLocation ATTACK_WHEEL_STATE_PACKET = new ResourceLocation("nichirin", "attack_wheel_state");
    private static final ResourceLocation KATANA_INPUT_PACKET = new ResourceLocation("nichirin", "katana_input");

    // Server-side state tracking (AUTHORITATIVE)
    private static final Map<UUID, PlayerInputState> serverPlayerStates = new ConcurrentHashMap<>();

    // Client-side state (for UI only, not authoritative)
    private static volatile boolean clientWheelOpen = false;
    private static volatile long clientWheelCloseTime = 0;

    /**
     * Player input state tracked server-side
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
     * Initialize the system
     */
    public static void initialize() {
        registerServerPacketHandlers();
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

            // Send state to server
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeBoolean(open);
            NetworkManager.sendToServer(ATTACK_WHEEL_STATE_PACKET, buf);
        }
    }

    /**
     * CLIENT: Check if inputs should be blocked
     */
    public static boolean shouldBlockInputsClient() {
        // Simple rule: Block ALL inputs when wheel is open, period.
        if (clientWheelOpen) {
            return true;
        }

        // Brief grace period after wheel closes to prevent click leakage
        if (clientWheelCloseTime > 0) {
            long elapsed = System.currentTimeMillis() - clientWheelCloseTime;
            if (elapsed < 500) { // Reduced to 500ms
                return true;
            }
        }

        return false;
    }

    /**
     * CLIENT: Send katana input to server
     */
    public static void sendKatanaInput(InputType inputType, Player player) {
        if (player.level().isClientSide) {
            // Check client-side block first (immediate feedback)
            if (shouldBlockInputsClient()) {
                return;
            }

            // Send to server for authoritative handling
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeEnum(inputType);
            NetworkManager.sendToServer(KATANA_INPUT_PACKET, buf);
        }
    }

    /**
     * CLIENT: Send breathing move to server (use existing system)
     */
    public static void sendBreathingMove(int moveIndex, Player player) {
        if (player.level().isClientSide) {
            // Use the existing BreathingMovePacket system that already works
            var packet = new com.xirc.nichirin.common.network.BreathingMovePacket(moveIndex, true);
            com.xirc.nichirin.registry.NichirinPacketRegistry.sendToServer(packet);
        }
    }

    /**
     * SERVER: Check if player inputs should be blocked
     */
    public static boolean shouldBlockInputsServer(Player player) {
        if (player.level().isClientSide) return false;

        if (player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.STUNNED.get())) {
            return true;
        }

        // Check if player has blocking effect
        if (player.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.BLOCKING.get())) {
            return true;
        }

        PlayerInputState state = serverPlayerStates.get(player.getUUID());
        if (state == null) return false;

        long currentTime = player.level().getGameTime();
        return state.shouldBlockInput(currentTime);
    }

    /**
     * SERVER: Block inputs after breathing move execution
     */
    public static void blockInputsAfterBreathingMove(Player player) {
        if (!player.level().isClientSide) {
            PlayerInputState state = getOrCreatePlayerState(player);
            state.inputBlocked = true;
            state.blockUntilTime = player.level().getGameTime() + 40; // 2 seconds (40 ticks)
        }
    }

    /**
     * SERVER: Get or create player state
     */
    private static PlayerInputState getOrCreatePlayerState(Player player) {
        return serverPlayerStates.computeIfAbsent(player.getUUID(), uuid -> new PlayerInputState());
    }

    /**
     * Register server-side packet handlers
     */
    private static void registerServerPacketHandlers() {
        // Attack wheel state updates
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ATTACK_WHEEL_STATE_PACKET, (buf, context) -> {
            boolean wheelOpen = buf.readBoolean();
            ServerPlayer player = (ServerPlayer) context.getPlayer();

            context.queue(() -> {
                PlayerInputState state = getOrCreatePlayerState(player);
                state.attackWheelOpen = wheelOpen;

                if (!wheelOpen) {
                    // When wheel closes, brief block to prevent click leakage
                    state.wheelCloseTime = player.level().getGameTime();
                    state.inputBlocked = true;
                    state.blockUntilTime = player.level().getGameTime() + 10; // 0.5 second block
                }
            });
        });

        // Katana input handling
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, KATANA_INPUT_PACKET, (buf, context) -> {
            InputType inputType = buf.readEnum(InputType.class);
            ServerPlayer player = (ServerPlayer) context.getPlayer();

            context.queue(() -> {
                // AUTHORITATIVE CHECK: Block if needed
                if (shouldBlockInputsServer(player)) {
                    return;
                }

                // Execute the katana input
                executeKatanaInput(player, inputType);
            });
        });

        // Breathing move handling - REMOVED (use existing BreathingMovePacket system)
    }

    /**
     * SERVER SIDE: Execute katana input (called after validation)
     */
    private static void executeKatanaInput(ServerPlayer player, InputType inputType) {
        // Get the katana and execute the appropriate action
        var mainHand = player.getMainHandItem();
        if (!(mainHand.getItem() instanceof com.xirc.nichirin.common.item.katana.SimpleKatana katana)) {
            return;
        }

        switch (inputType) {
            case LEFT_CLICK -> {
                katana.performAttack(player);
            }
            case RIGHT_CLICK -> {
                katana.use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            case RIGHT_CLICK_CROUCH -> {
                // Temporarily set crouch state
                boolean wasCrouching = player.isShiftKeyDown();
                player.setShiftKeyDown(true);
                katana.use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
                player.setShiftKeyDown(wasCrouching);
            }
        }
    }

    /**
     * Clean up when player leaves server
     */
    public static void cleanupPlayer(Player player) {
        serverPlayerStates.remove(player.getUUID());
    }

    /**
     * Periodic cleanup of old states
     */
    public static void tick() {
        // Remove states for players who haven't been active
        long currentTime = System.currentTimeMillis();
        serverPlayerStates.entrySet().removeIf(entry -> {
            PlayerInputState state = entry.getValue();
            // Remove if inputs haven't been blocked for more than 30 seconds
            return !state.inputBlocked &&
                    state.wheelCloseTime > 0 &&
                    currentTime - state.wheelCloseTime > 30000;
        });
    }
}