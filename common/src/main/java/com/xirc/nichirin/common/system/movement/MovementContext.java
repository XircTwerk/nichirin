package com.xirc.nichirin.common.system.movement;

import com.xirc.nichirin.common.network.CooldownDisplayPacket;
import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Input-based movement system
 */
public class MovementContext {

    private static final float MOVEMENT_STAMINA_COST = 15.0f;
    private static final int MOVEMENT_COOLDOWN_TICKS = 40; // 2 seconds

    // Track cooldowns per player
    private static final Map<UUID, Long> playerCooldowns = new HashMap<>();
    // Track current input states per player
    private static final Map<UUID, InputState> playerInputs = new HashMap<>();

    /**
     * Update player input state (called from client sync)
     */
    public static void updatePlayerInput(UUID playerId, boolean forward, boolean backward, boolean left, boolean right, boolean jump) {
        InputState input = new InputState();
        input.forward = forward;
        input.backward = backward;
        input.left = left;
        input.right = right;
        input.jump = jump;

        playerInputs.put(playerId, input);
    }

    /**
     * Main entry point for movement system
     * Called when Mouse Button 5 is pressed
     */
    public static void handleMovementInput(Player player) {
        if (player == null || player.level().isClientSide) {
            return; // Server-side only
        }

        // Check if player is stunned or disoriented
        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get()) ||
                player.hasEffect(NichirinEffectRegistry.DISORIENTED.get())) {

            // Different messages for different effects
            if (player.hasEffect(NichirinEffectRegistry.DISORIENTED.get())) {
                player.displayClientMessage(
                        Component.literal("You are too disoriented to use movement abilities!").withStyle(style -> style.withColor(0x9932CC)),
                        true
                );
            } else {
                player.displayClientMessage(
                        Component.literal("You are stunned and cannot move!").withStyle(style -> style.withColor(0xFF0000)),
                        true
                );
            }
            return;
        }

        // Check cooldown
        if (isOnCooldown(player)) {
            player.displayClientMessage(
                    Component.literal("Movement on cooldown!").withStyle(style -> style.withColor(0xFFAA00)),
                    true
            );
            return;
        }

        // Check stamina
        if (!StaminaManager.hasStamina(player, MOVEMENT_STAMINA_COST)) {
            player.displayClientMessage(
                    Component.literal("Not enough stamina for movement!").withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.5f, 0.5f);
            return;
        }

        // Get current input state
        InputState input = playerInputs.getOrDefault(player.getUUID(), new InputState());

        // Determine movement type based on input and player state
        MovementType movementType = determineMovementType(player, input);


        if (movementType == MovementType.NONE) {
            return;
        }

        // Consume stamina
        if (!StaminaManager.consume(player, MOVEMENT_STAMINA_COST)) {
            return;
        }

        // Set cooldown and display on HUD
        setCooldown(player);
        displayMovementCooldown(player, movementType);

        // Execute the appropriate movement
        executeMovement(player, movementType, input);
    }

    /**
     * Determines movement type based on input state and player condition
     */
    private static MovementType determineMovementType(Player player, InputState input) {
        boolean onGround = player.onGround();

        // Air Dodge: Not on ground
        if (!onGround) {
            return MovementType.AIR_DODGE;
        }

        // Ground movement based on input
        boolean hasMovementInput = input.forward || input.backward || input.left || input.right;

        if (!hasMovementInput) {
            // No movement keys pressed - dodge
            return MovementType.DODGE;
        }

        // Has movement input - check what type
        if (input.backward && !input.forward) {
            // Only S pressed (can have A/D too) - backstep
            return MovementType.BACKSTEP;
        } else {
            // Any other combination (W, A, D, or combinations) - dash
            return MovementType.DASH;
        }
    }

    /**
     * Execute movement with input context
     */
    private static void executeMovement(Player player, MovementType movementType, InputState input) {
        switch (movementType) {
            case DODGE -> Dodge.executeGroundDodge(player); // FIXED: use executeGroundDodge
            case AIR_DODGE -> {
                // Convert InputState to DashInput for air dodge
                DashInput airDodgeInput = new DashInput();
                airDodgeInput.forward = input.forward;
                airDodgeInput.backward = input.backward;
                airDodgeInput.left = input.left;
                airDodgeInput.right = input.right;
                Dodge.executeAirDodge(player, airDodgeInput); // Pass input for directional air dodge
            }
            case BACKSTEP -> Backstep.execute(player); // Backstep doesn't need input direction
            case DASH -> {
                // Convert InputState to public class for Dash
                DashInput dashInput = new DashInput();
                dashInput.forward = input.forward;
                dashInput.backward = input.backward;
                dashInput.left = input.left;
                dashInput.right = input.right;
                Dash.execute(player, dashInput);
            }
            case NONE -> {
                // Should not happen
            }
        }
    }

    /**
     * Public input class for Dash system
     */
    public static class DashInput {
        public boolean forward = false;
        public boolean backward = false;
        public boolean left = false;
        public boolean right = false;
    }

    /**
     * Display cooldown on client HUD
     */
    private static void displayMovementCooldown(Player player, MovementType movementType) {
        if (!player.level().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            String cooldownName = switch (movementType) {
                case DODGE -> "Dodge";
                case AIR_DODGE -> "Air Dodge";
                case BACKSTEP -> "Backstep";
                case DASH -> "Dash";
                case NONE -> "Movement";
            };

            CooldownDisplayPacket.sendToClient(serverPlayer, cooldownName, MOVEMENT_COOLDOWN_TICKS);
        }
    }

    /**
     * Cooldown management
     */
    private static boolean isOnCooldown(Player player) {
        Long lastUse = playerCooldowns.get(player.getUUID());
        if (lastUse == null) return false;

        long currentTime = player.level().getGameTime();
        return (currentTime - lastUse) < MOVEMENT_COOLDOWN_TICKS;
    }

    private static void setCooldown(Player player) {
        playerCooldowns.put(player.getUUID(), player.level().getGameTime());
    }

    /**
     * Get remaining cooldown time in ticks
     */
    public static int getRemainingCooldown(Player player) {
        Long lastUse = playerCooldowns.get(player.getUUID());
        if (lastUse == null) return 0;

        long currentTime = player.level().getGameTime();
        long timeSinceUse = currentTime - lastUse;

        return Math.max(0, (int)(MOVEMENT_COOLDOWN_TICKS - timeSinceUse));
    }

    /**
     * Movement types
     */
    public enum MovementType {
        DODGE,
        AIR_DODGE,
        BACKSTEP,
        DASH,
        NONE
    }

    /**
     * Input state tracking
     */
    private static class InputState {
        boolean forward = false;
        boolean backward = false;
        boolean left = false;
        boolean right = false;
        boolean jump = false;
    }
}