package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.water.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.util.BreathingManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Water Breathing moveset implementation with manual combo system
 * Water Breathing excels at close-range pressure with high average damage and lenient hitboxes
 * Features telegraphed mobility options and continuous pressure attacks
 *
 * Right-click: Water Surface Slash combo (3-stage combo system)
 * Crouch + Right-click: Water Wheel (lunging wheel attack)
 */
public class WaterBreathingMoveset extends AbstractMoveset {

    // Track cooldowns per player per move
    private static final Map<UUID, Map<Integer, Long>> playerCooldowns = new HashMap<>();

    // Track active attacks to prevent breath consumption on failed attempts
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();

    // Manual combo system
    private static final Map<UUID, ComboState> playerComboStates = new HashMap<>();

    // Thread-local to store current moveset instance for action access
    private static final ThreadLocal<WaterBreathingMoveset> CURRENT_MOVESET = new ThreadLocal<>();

    // Combo state tracking
    private static class ComboState {
        int currentStage = 0;
        long lastAttackTime = 0;
        long comboWindow = 1000; // 1 second window to continue combo

        boolean canContinueCombo() {
            return System.currentTimeMillis() - lastAttackTime <= comboWindow;
        }

        void updateAttackTime() {
            lastAttackTime = System.currentTimeMillis();
        }

        void reset() {
            currentStage = 0;
            lastAttackTime = 0;
        }

        void nextStage() {
            currentStage++;
            updateAttackTime();
        }
    }

    public WaterBreathingMoveset() {
        super("water_breathing", "Water Breathing", MovesetType.BREATHING, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:water_idle")
                .withSpeedMultiplier(1.1f) // Faster than normal for pressure

                // Third Form: Flowing Dance - Empowerment and trail attack (INDEX 0)
                .withMove(new MoveBuilder("flowing_dance", "Flowing Dance")
                        .withAnimation("nichirin:flowing_dance", 12)
                        .withTiming(240, 15, 60) // 6 second cooldown, 0.75s windup, 3s duration
                        .withDamage(4.0f) // Continuous damage
                        .withRange(3.0f) // Close range continuous
                        .withKnockback(0.05f) // Very light knockback
                        .withBreathCost(25.0f)
                        .withHitStun(3) // Very short for continuous hits
                        .withHitboxSize(2.5f)
                        .withAction(player -> {
                            FlowingDanceAttack attack = new FlowingDanceAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(0));
                            }
                            MoveExecutor.executeAttack(player, attack, "water_breathing", "flowing_dance");
                        })
                )

                // Fourth Form: Striking Tide - Omnidirectional slashes (INDEX 1)
                .withMove(new MoveBuilder("striking_tide", "Striking Tide")
                        .withAnimation("nichirin:striking_tide", 14)
                        .withTiming(280, 25, 40) // 7 second cooldown, 1.25s windup, 2s execution
                        .withDamage(10.0f) // Good damage for 360° attack
                        .withRange(4.5f) // Large omnidirectional range
                        .withKnockback(0.4f)
                        .withBreathCost(25.0f)
                        .withHitStun(8)
                        .withHitboxSize(4.5f) // Full radius
                        .withAction(player -> {
                            StrikingTideAttack attack = new StrikingTideAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(1));
                            }
                            MoveExecutor.executeAttack(player, attack, "water_breathing", "striking_tide");
                        })
                )

                // Fifth Form: Blessed Rain After the Drought - Ultimate precision dash (INDEX 2)
                .withMove(new MoveBuilder("blessed_rain", "Blessed Rain")
                        .withAnimation("nichirin:blessed_rain", 18)
                        .withTiming(400, 15, 25) // 10 second cooldown, 0.75s windup, 1.25s duration
                        .withDamage(20.0f) // Drops half a health bar
                        .withRange(8.0f) // Long dash range
                        .withKnockback(0.8f)
                        .withBreathCost(45.0f)
                        .withHitStun(30)
                        .withHitboxSize(1.0f) // Very small precise hitbox
                        .withDashSpeed(12.0f) // Fast dash
                        .withAction(player -> {
                            BlessedRainAttack attack = new BlessedRainAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(2));
                            }
                            MoveExecutor.executeAttack(player, attack, "water_breathing", "blessed_rain");
                        })
                )

                // Sixth Form: Whirlpool - Rising whirlpool attack (INDEX 3)
                .withMove(new MoveBuilder("whirlpool", "Whirlpool")
                        .withAnimation("nichirin:whirlpool", 15)
                        .withTiming(240, 20, 50) // 8 second cooldown, 1s windup, 2.5s duration
                        .withDamage(8.0f) // Multi-hit spinning damage
                        .withRange(3.0f) // Whirlpool radius
                        .withKnockback(0.1f) // Light knockback, enemies spin around
                        .withBreathCost(20.0f)
                        .withHitStun(5) // Short for spinning effect
                        .withHitboxSize(3.0f)
                        .withAction(player -> {
                            WhirlpoolAttack attack = new WhirlpoolAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(3));
                            }
                            MoveExecutor.executeAttack(player, attack, "water_breathing", "whirlpool");
                        })
                )

                // Seventh Form: Drop Ripple Thrust - Shield and thrust attack (INDEX 4)
                .withMove(new MoveBuilder("drop_ripple_thrust", "Drop Ripple Thrust")
                        .withAnimation("nichirin:drop_ripple_thrust", 13)
                        .withTiming(200, 10, 35) // 5 second cooldown, 0.5s windup, 1.75s duration
                        .withDamage(10.0f) // Good thrust damage
                        .withRange(5.0f) // Thrust range
                        .withKnockback(0.3f)
                        .withBreathCost(15.0f)
                        .withHitStun(20)
                        .withHitboxSize(4.0f) // Wall of ripples
                        .withAction(player -> {
                            DropRippleThrustAttack attack = new DropRippleThrustAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(4));
                            }
                            MoveExecutor.executeAttack(player, attack, "water_breathing", "drop_ripple_thrust");
                        })
                )

                // Eighth Form: Waterfall Basin - BIG ASS MULTIHIT (INDEX 5)
                .withMove(new MoveBuilder("waterfall_basin", "Waterfall Basin")
                        .withAnimation("nichirin:waterfall_basin", 16)
                        .withTiming(300, 30, 60) // 9 second cooldown, 1.5s windup, 3s duration
                        .withDamage(8.5f) // High DPS multi-hit
                        .withRange(6.0f) // Large waterfall area
                        .withKnockback(0.15f) // Light knockback to keep enemies in waterfall
                        .withBreathCost(35.0f)
                        .withHitStun(8) // Medium stun for multi-hit
                        .withHitboxSize(6.0f) // BIG ASS HITBOX
                        .withAction(player -> {
                            WaterfallBasinAttack attack = new WaterfallBasinAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(5));
                            }
                            MoveExecutor.executeAttack(player, attack, "water_breathing", "waterfall_basin");
                        })
                )

                // Ninth Form: Splashing Water Flow - Zigzag dash attack (INDEX 6)
                .withMove(new MoveBuilder("splashing_water_flow", "Splashing Water Flow")
                        .withAnimation("nichirin:splashing_water_flow", 14)
                        .withTiming(240, 10, 40) // 7 second cooldown, 0.5s windup, 2s duration
                        .withDamage(8.0f) // Good dash damage
                        .withRange(5.0f) // 10 block zigzag range
                        .withKnockback(0.4f)
                        .withBreathCost(25.0f)
                        .withHitStun(18)
                        .withHitboxSize(6f)
                        .withDashSpeed(8.0f) // Fast zigzag speed
                        .withAction(player -> {
                            SplashingWaterFlowAttack attack = new SplashingWaterFlowAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(6));
                            }
                            MoveExecutor.executeAttack(player, attack, "water_breathing", "splashing_water_flow");
                        })
                )

                // Tenth Form: Constant Flux - 5-hit combo with dragon finisher (INDEX 7)
                .withMove(new MoveBuilder("constant_flux", "Constant Flux")
                        .withAnimation("nichirin:constant_flux", 20)
                        .withTiming(360, 20, 80) // 15 second cooldown, 1s windup, 4s duration
                        .withDamage(24.0f) // Strong combo damage
                        .withRange(5.0f) // Drag range
                        .withKnockback(0.2f) // Light knockback for dragging
                        .withBreathCost(50.0f)
                        .withHitStun(12)
                        .withHitboxSize(4.0f)
                        .withAction(player -> {
                            ConstantFluxAttack attack = new ConstantFluxAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(7));
                            }
                            MoveExecutor.executeAttack(player, attack, "water_breathing", "constant_flux");
                        })
                )

                // Eleventh Form: Dead Calm - Auto-target AoE field (INDEX 8)
                .withMove(new MoveBuilder("dead_calm", "Dead Calm")
                        .withAnimation("nichirin:dead_calm", 17)
                        .withTiming(360, 12, 100) // 11 second cooldown, 1.25s windup, 5s duration
                        .withDamage(7.0f) // Persistent area damage
                        .withRange(6.0f) // Large persistent area
                        .withKnockback(0f)
                        .withBreathCost(55.0f)
                        .withHitStun(10)
                        .withHitboxSize(6.0f) // Large area field
                        .withAction(player -> {
                            DeadCalmAttack attack = new DeadCalmAttack();
                            WaterBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(8));
                            }
                            MoveExecutor.executeAttack(player, attack, "water_breathing", "dead_calm");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 9; // 9 forms in the wheel (3rd through 11th forms)
    }

    @Override
    public boolean handleRightClick(Player player, boolean isCrouching) {
        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return true; // Block the move by overriding
        }

        if (isCrouching) {
            // Crouch + Right-click: Water Wheel (separate from combo)
            return executeWaterWheel(player);
        } else {
            // Regular Right-click: Water Surface Slash combo system
            return executeWaterSurfaceSlashCombo(player);
        }
    }

    private boolean executeWaterSurfaceSlashCombo(Player player) {
        ComboState comboState = playerComboStates.computeIfAbsent(player.getUUID(), k -> new ComboState());

        int nextStage;

        if (comboState.currentStage == 0 || !comboState.canContinueCombo()) {
            // Start new combo or restart if window expired
            nextStage = 1;
            comboState.reset();
        } else {
            // Continue existing combo
            nextStage = comboState.currentStage + 1;
        }

        // Cap at stage 3
        if (nextStage > 3) {
            comboState.reset();
            nextStage = 1;
        }

        // Execute the appropriate stage
        boolean success = executeWaterSurfaceSlashStage(player, nextStage);

        if (success) {
            comboState.currentStage = nextStage;
            comboState.updateAttackTime();

            // Reset combo after final stage
            if (nextStage == 3) {
                comboState.reset();
            }
        } else {
        }

        return success;
    }

    private boolean executeWaterSurfaceSlashStage(Player player, int stage) {

        WaterSurfaceSlashAttack attack = new WaterSurfaceSlashAttack();
        attack.setComboStage(stage);

        // Different configurations for each stage
        MoveConfiguration config = createStageConfig(stage);

        attack.configure(config);

        MoveExecutor.executeAttack(player, attack, "water_breathing", "water_surface_slash_stage_" + stage);
        onMovePerformed(player, -1, false);

        return true;
    }

    private MoveConfiguration createStageConfig(int stage) {
        switch (stage) {
            case 1 -> {
                return new MoveBuilder("water_surface_slash_1", "Water Surface Slash I")
                        .withAnimation("nichirin:water_surface_slash", 6)
                        .withTiming(0, 0, 18) // No cooldown, instant, 18 ticks duration
                        .withDamage(5.0f)
                        .withRange(3.5f)
                        .withKnockback(0f) // No knockback for first hit
                        .withBreathCost(8.0f)
                        .withHitStun(20)
                        .withHitboxSize(3.0f)
                        .build();
            }
            case 2 -> {
                return new MoveBuilder("water_surface_slash_2", "Water Surface Slash II")
                        .withAnimation("nichirin:water_surface_slash_2", 6)
                        .withTiming(0, 0, 18) // No windup, 18 tick duration
                        .withDamage(6.0f) // Slightly more damage
                        .withRange(3.5f)
                        .withKnockback(0f) // Still no knockback
                        .withBreathCost(10.0f) // Higher breath cost
                        .withHitStun(22) // Slightly more stun
                        .withHitboxSize(3.0f)
                        .build();
            }
            case 3 -> {
                return new MoveBuilder("water_slam_finisher", "Water Slam")
                        .withAnimation("nichirin:water_slam", 10)
                        .withTiming(0, 0, 25) // No windup, longer duration for slam impact
                        .withDamage(12.0f) // High damage finisher
                        .withRange(4.0f) // Larger range for slam
                        .withKnockback(0.8f) // High knockback for finisher
                        .withBreathCost(15.0f) // Expensive finisher
                        .withHitStun(30) // High stun for finisher
                        .withHitboxSize(4.0f) // Larger slam area
                        .build();
            }
            default -> throw new IllegalArgumentException("Invalid stage: " + stage);
        }
    }

    private boolean executeWaterWheel(Player player) {
        WaterWheelAttack attack = new WaterWheelAttack();

        MoveConfiguration tempConfig = new MoveBuilder("water_wheel", "Water Wheel")
                .withAnimation("nichirin:water_wheel", 10)
                .withTiming(0, 10, 30) // No cooldown, 0.5s windup, 1.5s duration
                .withDamage(6.0f)
                .withRange(4.0f)
                .withKnockback(0.1f)
                .withBreathCost(18.0f)
                .withHitStun(5)
                .withHitboxSize(3.5f)
                .withDashSpeed(4.0f)
                .build();

        attack.configure(tempConfig);
        MoveExecutor.executeAttack(player, attack, "water_breathing", "water_wheel");
        onMovePerformed(player, -2, true);
        return true;
    }

    @Override
    public void performMove(Player player, int moveIndex) {
        // Check cooldown before allowing move
        if (!canUseMove(player, moveIndex)) {
            // Show cooldown message
            MoveConfiguration config = getMove(moveIndex);
            if (config != null) {
                Map<Integer, Long> cooldowns = playerCooldowns.get(player.getUUID());
                if (cooldowns != null) {
                    Long cooldownEnd = cooldowns.get(moveIndex);
                    if (cooldownEnd != null) {
                        long remaining = (cooldownEnd - player.level().getGameTime()) / 20;
                        player.displayClientMessage(
                                Component.literal(config.getDisplayName() + " on cooldown! " + remaining + "s remaining")
                                        .withStyle(style -> style.withColor(0x4A90E2)), // Blue color for water
                                true
                        );
                    }
                }
            }
            return;
        }

        // Check breath BEFORE executing
        MoveConfiguration config = getMove(moveIndex);
        if (config != null) {
            float breathCost = config.getBreathCostOrDefault(0.0f);

            // Add small buffer to prevent race conditions
            if (breathCost > 0 && !BreathingManager.hasBreath(player, breathCost + 0.1f)) {
                player.displayClientMessage(
                        Component.literal("Not enough breath for " + config.getDisplayName() + "!")
                                .withStyle(style -> style.withColor(0xFF3333)), // Red for no breath
                        true
                );
                return;
            }
        }

        // Mark that we're executing a move
        executingMove.put(player.getUUID(), true);

        // Store current moveset instance for access by actions
        CURRENT_MOVESET.set(this);

        try {
            // Execute the move
            super.performMove(player, moveIndex);
        } finally {
            // Always clean up the thread local
            CURRENT_MOVESET.remove();
        }

        // Check if move actually executed by seeing if breath was consumed
        boolean moveExecuted = !executingMove.getOrDefault(player.getUUID(), false);
        executingMove.remove(player.getUUID());

        if (moveExecuted && config != null) {
            // Set cooldown after successful execution
            setMoveCooldown(player, moveIndex);

            // Send cooldown display packet if on server and has cooldown
            if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer
                    && config.getCooldownOrDefault(0) > 0) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeUtf(config.getDisplayName());
                buf.writeInt(config.getCooldownOrDefault(0));

                NetworkManager.sendToPlayer(serverPlayer, new ResourceLocation("nichirin", "cooldown_display"), buf);
            }
        }
    }

    /**
     * Get the current moveset instance (for use in action lambdas)
     */
    public static WaterBreathingMoveset getCurrentMoveset() {
        return CURRENT_MOVESET.get();
    }

    /**
     * Check if a player can use a specific move (not on cooldown)
     */
    private boolean canUseMove(Player player, int moveIndex) {
        MoveConfiguration config = getMove(moveIndex);
        if (config == null || config.getCooldownOrDefault(0) <= 0) {
            return true; // No cooldown
        }

        Map<Integer, Long> cooldowns = playerCooldowns.get(player.getUUID());
        if (cooldowns == null) {
            return true; // No cooldowns tracked yet
        }

        Long cooldownEnd = cooldowns.get(moveIndex);
        if (cooldownEnd == null) {
            return true; // Move never used
        }

        long currentTime = player.level().getGameTime();
        return currentTime >= cooldownEnd;
    }

    /**
     * Set a move on cooldown
     */
    private void setMoveCooldown(Player player, int moveIndex) {
        MoveConfiguration config = getMove(moveIndex);
        if (config == null || config.getCooldownOrDefault(0) <= 0) {
            return; // No cooldown
        }

        long cooldownEnd = player.level().getGameTime() + config.getCooldownOrDefault(0);
        playerCooldowns.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(moveIndex, cooldownEnd);
    }

    @Override
    public int getRightClickMoveIndex(boolean isCrouching) {
        return isCrouching ? -2 : -1; // Not in attack wheel, handled separately
    }

    @Override
    public String getRightClickMoveName() {
        return "Water Surface Slash";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Water Wheel";
    }

    @Override
    public void onMovePerformed(Player player, int moveIndex, boolean isCrouching) {
        // Water Breathing specific post-move effects can be added here
        // moveIndex -1 = Water Surface Slash (right-click)
        // moveIndex -2 = Water Wheel (crouch + right-click)
    }

    /**
     * Called when a player logs out - clean up their data
     */
    public static void cleanupPlayer(Player player) {
        playerCooldowns.remove(player.getUUID());
        executingMove.remove(player.getUUID());
        playerComboStates.remove(player.getUUID());
    }
}