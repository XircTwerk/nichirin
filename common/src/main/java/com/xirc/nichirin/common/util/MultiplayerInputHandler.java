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
 * Handles all multiplayer input for katanas, breathing moves, and demon arts
 * Enhanced to support demon abilities without katana requirement
 */
public class MultiplayerInputHandler {

    // Network packet IDs
    private static final ResourceLocation ATTACK_WHEEL_STATE_PACKET = new ResourceLocation("nichirin", "attack_wheel_state");
    private static final ResourceLocation KATANA_INPUT_PACKET = new ResourceLocation("nichirin", "katana_input");
    private static final ResourceLocation DEMON_INPUT_PACKET = new ResourceLocation("nichirin", "demon_input");

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
     * CLIENT: Send katana/demon input to server
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
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeEnum(inputType);
                NetworkManager.sendToServer(KATANA_INPUT_PACKET, buf);
            } else {
                // Not holding katana = check for demon moveset
                boolean hasDemon = MovesetHelper.hasDemonMoveset(player);

                if (hasDemon) {
                    // Send demon input packet
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                    buf.writeUtf(inputType.name());
                    NetworkManager.sendToServer(DEMON_INPUT_PACKET, buf);
                } else {
                    // No demon moveset and no katana = send katana packet anyway (will fail server-side)
                    // But only for right-click inputs, not left-click
                    if (inputType != InputType.LEFT_CLICK) {
                        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                        buf.writeEnum(inputType);
                        NetworkManager.sendToServer(KATANA_INPUT_PACKET, buf);
                    }
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
            // Use the existing BreathingMovePacket system that already works
            var packet = new BreathingMovePacket(moveIndex, true);
            com.xirc.nichirin.registry.NichirinPacketRegistry.sendToServer(packet);
        }
    }

    /**
     * CLIENT: Send demon move to server (use same packet system as breathing)
     */
    public static void sendDemonMove(int moveIndex, Player player) {
        if (player.level().isClientSide) {
            // Use the DemonMovePacket system (similar to BreathingMovePacket)
            var packet = new DemonMovePacket(moveIndex, true);
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
     * SERVER: Block inputs after move execution (breathing or demon)
     */
    public static void blockInputsAfterMoveExecution(Player player) {
        if (!player.level().isClientSide) {
            PlayerInputState state = getOrCreatePlayerState(player);
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

        // Katana input handling (breathing users)
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, KATANA_INPUT_PACKET, (buf, context) -> {
            InputType inputType = buf.readEnum(InputType.class);
            ServerPlayer player = (ServerPlayer) context.getPlayer();

            context.queue(() -> {
                // AUTHORITATIVE CHECK: Block if needed
                if (shouldBlockInputsServer(player)) {
                    return;
                }

                // Execute katana input (requires katana)
                executeKatanaInput(player, inputType);
            });
        });

        // Demon input handling (demon art users)
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DEMON_INPUT_PACKET, (buf, context) -> {
            String inputTypeStr = buf.readUtf();
            InputType inputType = InputType.valueOf(inputTypeStr);
            ServerPlayer player = (ServerPlayer) context.getPlayer();

            context.queue(() -> {
                // AUTHORITATIVE CHECK: Block if needed
                if (shouldBlockInputsServer(player)) {
                    return;
                }

                // Execute demon input (only if has demon moveset)
                executeDemonInput(player, inputType);
            });
        });
    }

    /**
     * SERVER SIDE: Execute katana input (breathing users - requires katana)
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
                katana.use(player.level(), player, InteractionHand.MAIN_HAND);
            }
            case RIGHT_CLICK_CROUCH -> {
                // Temporarily set crouch state
                boolean wasCrouching = player.isShiftKeyDown();
                player.setShiftKeyDown(true);
                katana.use(player.level(), player, InteractionHand.MAIN_HAND);
                player.setShiftKeyDown(wasCrouching);
            }
        }
    }

    /**
     * SERVER SIDE: Execute demon input (demon art users - no katana required)
     */
    private static void executeDemonInput(ServerPlayer player, InputType inputType) {
        // Check if player has demon moveset
        if (!MovesetHelper.hasDemonMoveset(player)) {
            return;
        }

        // Handle right-click and crouch+right-click for entity attacks
        switch (inputType) {
            case RIGHT_CLICK -> {
                // Execute demon right-click ability on entities (move index 0)
                executeDemonMove(player, 0);
            }
            case RIGHT_CLICK_CROUCH -> {
                // Execute demon crouch+right-click ability on entities (move index 1)
                executeDemonMove(player, 1);
            }
            // LEFT_CLICK not used for demon abilities
        }
    }

    /**
     * SERVER SIDE: Execute a specific demon move by index
     */
    private static void executeDemonMove(ServerPlayer player, int moveIndex) {
        try {
            var moveset = MovesetHelper.getDemonMoveset(player);
            if (moveset == null || moveIndex >= moveset.getMoveCount()) {
                return;
            }

            // Execute the demon move
            moveset.performMove(player, moveIndex);

            // Block inputs after execution
            blockInputsAfterMoveExecution(player);

        } catch (Exception e) {
            System.err.println("ERROR: Failed to execute demon move " + moveIndex + " for player " + player.getName().getString());
            e.printStackTrace();
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