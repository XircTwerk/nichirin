package com.xirc.nichirin.common.attack.moveset.demon;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moves.demon.basic.*;
import com.xirc.nichirin.common.network.s2c.MovesetConfigSyncPacket;
import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.common.system.DemonManager;
import com.xirc.nichirin.common.util.BreathingManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Default demon moveset available to all demons
 * Focuses on basic demonic abilities and blood mechanics
 * Now aligned with breathing moveset patterns while retaining demon-specific features
 *
 * Right-click: Slash combo (2-stage combo system)
 * Crouch + Right-click: High Jump / Stomp
 */
public class DefaultDemonMoveset extends AbstractMoveset {

    // Track cooldowns per player per move (using base system now)
    private static final Map<UUID, Map<Integer, Long>> playerCooldowns = new HashMap<>();

    // Track active attacks to prevent resource consumption on failed attempts
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();

    // Immediate execution flags to prevent rapid-fire spam
    private static final Map<UUID, Boolean> executingHighJump = new HashMap<>();

    // Manual combo system for slash
    private static final Map<UUID, SlashComboState> playerSlashStates = new HashMap<>();

    // Track high jump state per player - only allow stomp after high jump
    private static final Map<UUID, Boolean> canStompAfterHighJump = new HashMap<>();

    // Simple flag to prevent high jump spam - resets when player lands
    private static final Map<UUID, Boolean> hasUsedHighJumpInAir = new HashMap<>();

    // Same-tick execution counters to prevent multiple calls in same game tick
    private static final Map<UUID, Long> lastHighJumpTick = new HashMap<>();

    // Thread-local to store current moveset instance for action access
    private static final ThreadLocal<DefaultDemonMoveset> CURRENT_MOVESET = new ThreadLocal<>();

    // Slash combo state tracking
    private static class SlashComboState {
        int currentStage = 0;
        long lastAttackTime = 0;
        long comboWindow = 800; // 0.8 second window to continue combo

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

    public DefaultDemonMoveset() {
        super("default_demon", "Demon Arts", MovesetType.DEMON, createBuilder());

        // Auto-capture configs for GUI display
        captureInitialConfigs();
    }

    private void captureInitialConfigs() {
        createAndCaptureSlashConfig();
        createAndCaptureHighJumpConfig();
    }

    private void createAndCaptureSlashConfig() {
        MoveConfiguration tempConfig = new MoveBuilder("demon_slash", "Slash")
                .withAnimation("nichirin:demon_slash", 6)
                .withTiming(0, 0, 20) // No cooldown on first slash
                .withDamage(4.0f)
                .withRange(3.0f)
                .withKnockback(0f) // First hit no knockback
                .withHitStun(15)
                .withHitboxSize(2.0f)
                .withDescription("Basic claw slash with followup potential")
                .build();
        this.captureRightClickConfig(tempConfig, false);
    }

    private void createAndCaptureHighJumpConfig() {
        MoveConfiguration tempConfig = new MoveBuilder("high_jump", "High Jump")
                .withAnimation("nichirin:demon_high_jump", 8)
                .withTiming(100, 0, 5) // 5 second cooldown for high jump
                .withDescription("Launch 5 blocks into the air, crouch right-click mid-air for stomp attack")
                .build();
        this.captureRightClickConfig(tempConfig, true);
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:demon_idle")
                .withSpeedMultiplier(1.05f) // Slight speed boost for demons

                // Kick - High knockback front push (INDEX 0)
                .withMove(new MoveBuilder("demon_kick", "Kick")
                        .withAnimation("nichirin:demon_kick", 8)
                        .withTiming(60, 5, 15) // 3 second cooldown, quick windup, 15 tick duration
                        .withDamage(6.0f)
                        .withRange(2.5f)
                        .withKnockback(1.2f)
                        .withHitStun(25)
                        .withHitboxSize(2.0f)
                        .withDescription("Powerful front kick with high knockback and crowd control")
                        .withAction(player -> {
                            DemonKickAttack kickAttack = new DemonKickAttack();
                            DefaultDemonMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                kickAttack.configure(moveset.getMove(0));
                            }
                            MoveExecutor.executeAttack(player, kickAttack, "default_demon", "demon_kick");
                        })
                )

                // Dashing Strike - Dash 4 blocks forward with punch (INDEX 1)
                .withMove(new MoveBuilder("dashing_strike", "Dashing Strike")
                        .withAnimation("nichirin:demon_dash_strike", 10)
                        .withTiming(80, 8, 20) // 4 second cooldown, dash windup, 20 tick duration
                        .withDamage(12.0f)
                        .withDashSpeed(4.0f)
                        .withRange(4.0f)
                        .withKnockback(0.4f)
                        .withHitStun(20)
                        .withHitboxSize(2)
                        .withDescription("Dash forward 4 blocks and deliver a devastating punch")
                        .withAction(player -> {
                            DemonDashStrikeAttack dashStrikeAttack = new DemonDashStrikeAttack();
                            DefaultDemonMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                dashStrikeAttack.configure(moveset.getMove(1));
                            }
                            MoveExecutor.executeAttack(player, dashStrikeAttack, "default_demon", "dashing_strike");
                        })
                )

                // Bite - Strong bite that steals blood (INDEX 2)
                .withMove(new MoveBuilder("demon_bite", "Bite")
                        .withAnimation("nichirin:demon_bite", 12)
                        .withTiming(100, 10, 25) // 5 second cooldown, bite windup, 25 tick duration
                        .withDamage(15.0f)
                        .withRange(2.0f)
                        .withKnockback(0f)
                        .withHitStun(30)
                        .withHitboxSize(1.5f)
                        .withDescription("Powerful bite that steals blood from enemies and deals high damage")
                        .withAction(player -> {
                            DemonBiteAttack biteAttack = new DemonBiteAttack();
                            DefaultDemonMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                biteAttack.configure(moveset.getMove(2));
                            }
                            MoveExecutor.executeAttack(player, biteAttack, "default_demon", "demon_bite");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 3; // Kick, Dashing Strike, Bite
    }

    @Override
    public boolean handleRightClick(Player player, boolean isCrouching) {
        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return true; // Block the move by overriding
        }

        if (isCrouching) {
            // Crouch + Right-click: High Jump or Stomp
            return executeHighJumpOrStomp(player);
        } else {
            // Regular Right-click: Slash combo system
            return executeSlashCombo(player);
        }
    }

    private boolean executeSlashCombo(Player player) {
        // Direct cooldown check without separate method to avoid compilation issues
        Map<Integer, Long> cooldowns = playerCooldowns.get(player.getUUID());
        if (cooldowns != null) {
            Long cooldownEnd = cooldowns.get(-1);
            if (cooldownEnd != null) {
                long currentTime = player.level().getGameTime();
                boolean onCooldown = currentTime < cooldownEnd;

                if (onCooldown) {
                    long remaining = (cooldownEnd - currentTime);
                    player.displayClientMessage(
                            Component.literal("Move on cooldown! " + (remaining / 20.0f) + "s remaining")
                                    .withStyle(style -> style.withColor(0xFF5555)),
                            true
                    );
                    return false;
                }
            }
        }

        SlashComboState comboState = playerSlashStates.computeIfAbsent(player.getUUID(), k -> new SlashComboState());

        int nextStage;

        if (comboState.currentStage == 0 || !comboState.canContinueCombo()) {
            // Start new combo or restart if window expired
            nextStage = 1;
            comboState.reset();
        } else {
            // Continue existing combo
            nextStage = comboState.currentStage + 1;
        }

        // Cap at stage 2 (2-hit combo)
        if (nextStage > 2) {
            comboState.reset();
            nextStage = 1;
        }

        // Execute the appropriate stage
        boolean success = executeSlashStage(player, nextStage);

        if (success) {
            comboState.currentStage = nextStage;
            comboState.updateAttackTime();

            // Set cooldown after final stage
            if (nextStage == 2) {
                setMoveCooldown(player, -1, 40); // 2 second cooldown for followup

                // Send cooldown display packet using utility
                if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                    com.xirc.nichirin.common.network.util.CooldownDisplayPacket.sendToClient(serverPlayer, "Slash Followup", 40);
                }
                comboState.reset(); // Reset after final stage
            }
        }

        return success;
    }

    private boolean executeSlashStage(Player player, int stage) {
        DemonSlashAttack attack = new DemonSlashAttack();
        attack.setSlashStage(stage);

        // Different configurations for each stage
        MoveConfiguration config = createSlashStageConfig(stage);

        // AUTOMATIC ANIMATION HANDLING - Send animation packet
        if (config.animationId != null && player instanceof ServerPlayer serverPlayer) {
            String animationName = config.animationId.getPath();
            PlayerAnimationPacket packet = new PlayerAnimationPacket(serverPlayer.getId(), animationName);
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }

        // Sync to client
        createAndCaptureSlashConfig();
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            MovesetConfigSyncPacket packet = new MovesetConfigSyncPacket(
                    "default_demon",
                    this.getRightClickConfiguration(),
                    this.getCrouchRightClickConfiguration()
            );
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }

        attack.configure(config);
        MoveExecutor.executeAttack(player, attack, "default_demon", "demon_slash_stage_" + stage);
        onMovePerformed(player, -1, false);

        return true;
    }

    private MoveConfiguration createSlashStageConfig(int stage) {
        switch (stage) {
            case 1 -> {
                return new MoveBuilder("demon_slash_1", "Slash")
                        .withAnimation("nichirin:demon_slash", 6)
                        .withTiming(0, 0, 20) // No cooldown, instant, 10 ticks duration
                        .withDamage(4.0f)
                        .withRange(3.0f)
                        .withKnockback(0f) // No knockback for first hit
                        .withHitStun(15)
                        .withHitboxSize(2.0f)
                        .build();
            }
            case 2 -> {
                return new MoveBuilder("demon_slash_2", "Slash Followup")
                        .withAnimation("nichirin:demon_slash_2", 6)
                        .withTiming(0, 5, 20)
                        .withDamage(4.0f) // Same damage
                        .withRange(3.0f)
                        .withKnockback(0.6f) // Higher knockback on second hit
                        .withHitStun(20) // More stun
                        .withHitboxSize(2.0f)
                        .build();
            }
            default -> throw new IllegalArgumentException("Invalid stage: " + stage);
        }
    }

    private boolean executeHighJumpOrStomp(Player player) {
        if (player.onGround()) {
            // Player is on ground - can always high jump (after cooldown)
            return executeHighJump(player);
        } else {
            // Player is in air - check if they can stomp or high jump
            if (canStompAfterHighJump.getOrDefault(player.getUUID(), false)) {
                return executeStomp(player);
            } else if (!hasUsedHighJumpInAir.getOrDefault(player.getUUID(), false)) {
                // Can high jump in air if they haven't used it yet this air session
                return executeHighJump(player);
            }
            return false;
        }
    }

    private boolean executeHighJump(Player player) {
        // Same-tick prevention check
        long currentTick = player.level().getGameTime();
        Long lastTick = lastHighJumpTick.get(player.getUUID());
        if (lastTick != null && lastTick.equals(currentTick)) {
            return false;
        }

        // Immediate execution flag check to prevent rapid-fire calls
        boolean isExecuting = executingHighJump.getOrDefault(player.getUUID(), false);
        if (isExecuting) {
            return false;
        }

        // Direct cooldown check for high jump
        Map<Integer, Long> cooldowns = playerCooldowns.get(player.getUUID());
        if (cooldowns != null) {
            Long cooldownEnd = cooldowns.get(-2);
            if (cooldownEnd != null) {
                long currentTime = player.level().getGameTime();
                boolean onCooldown = currentTime < cooldownEnd;

                if (onCooldown) {
                    long remaining = (cooldownEnd - currentTime);
                    player.displayClientMessage(
                            Component.literal("Move on cooldown! " + (remaining / 20.0f) + "s remaining")
                                    .withStyle(style -> style.withColor(0xFF5555)),
                            true
                    );
                    return false;
                }
            }
        }

        // Record this tick to prevent same-tick re-execution
        lastHighJumpTick.put(player.getUUID(), currentTick);

        // SET EXECUTION FLAG IMMEDIATELY to block rapid-fire calls
        executingHighJump.put(player.getUUID(), true);

        // SET COOLDOWN IMMEDIATELY to prevent double execution
        setMoveCooldown(player, -2, 100); // 5 second cooldown

        // Send cooldown display packet using utility
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            com.xirc.nichirin.common.network.util.CooldownDisplayPacket.sendToClient(serverPlayer, "High Jump", 100);
        }

        try {
            // Mark as used if in air to prevent spam
            if (!player.onGround()) {
                hasUsedHighJumpInAir.put(player.getUUID(), true);
            }

            DemonHighJumpAttack highJumpAttack = new DemonHighJumpAttack();

            // Use config without cooldown to prevent double cooldown
            MoveConfiguration tempConfig = new MoveBuilder("high_jump", "High Jump")
                    .withAnimation("nichirin:demon_high_jump", 8)
                    .withTiming(0, 0, 5) // No cooldown here - we handle it manually
                    .withDescription("Launch 5 blocks into the air, crouch right-click mid-air for stomp attack")
                    .build();

            // AUTOMATIC ANIMATION HANDLING - Send animation packet
            if (tempConfig.animationId != null && player instanceof ServerPlayer serverPlayer) {
                String animationName = tempConfig.animationId.getPath();
                PlayerAnimationPacket packet = new PlayerAnimationPacket(serverPlayer.getId(), animationName);
                NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
            }

            if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                MovesetConfigSyncPacket packet = new MovesetConfigSyncPacket(
                        "default_demon",
                        this.getRightClickConfiguration(),
                        this.getCrouchRightClickConfiguration()
                );
                NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
            }

            highJumpAttack.configure(tempConfig);
            MoveExecutor.executeAttack(player, highJumpAttack, "default_demon", "high_jump");

            // Mark player as able to stomp after high jump
            canStompAfterHighJump.put(player.getUUID(), true);

            onMovePerformed(player, -2, true);
            return true;

        } finally {
            // Always clear execution flag when done
            executingHighJump.remove(player.getUUID());
        }
    }

    private boolean executeStomp(Player player) {
        DemonStompAttack stompAttack = new DemonStompAttack();

        // Create stomp configuration
        MoveConfiguration stompConfig = new MoveBuilder("demon_stomp", "Stomp")
                .withAnimation("nichirin:demon_stomp", 10)
                .withTiming(0, 0, 15)
                .withDamage(20.0f) // High damage
                .withRange(3.0f) // Area around landing
                .withKnockback(0f) // No knockback, bury instead
                .withHitStun(40) // Very high stun
                .withHitboxSize(3.0f)
                .build();

        // AUTOMATIC ANIMATION HANDLING - Send animation packet
        if (stompConfig.animationId != null && player instanceof ServerPlayer serverPlayer) {
            String animationName = stompConfig.animationId.getPath();
            PlayerAnimationPacket packet = new PlayerAnimationPacket(serverPlayer.getId(), animationName);
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }

        stompAttack.configure(stompConfig);
        MoveExecutor.executeAttack(player, stompAttack, "default_demon", "demon_stomp");

        // Clear stomp ability after use
        canStompAfterHighJump.remove(player.getUUID());

        return true;
    }

    /**
     * Set a move on cooldown with custom cooldown time
     */
    private void setMoveCooldown(Player player, int moveIndex, int cooldownTicks) {
        long currentTime = player.level().getGameTime();
        long cooldownEnd = currentTime + cooldownTicks;

        playerCooldowns.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(moveIndex, cooldownEnd);
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
                        long remaining = (cooldownEnd - player.level().getGameTime());
                        player.displayClientMessage(
                                Component.literal("Move on cooldown! " + (remaining / 20.0f) + "s remaining")
                                        .withStyle(style -> style.withColor(0xFF5555)),
                                true
                        );
                    }
                }
            }
            return;
        }

        // Demons don't require breath/blood - skip resource checks

        // Mark that we're executing a move
        executingMove.put(player.getUUID(), true);

        // Store current moveset instance for access by actions
        CURRENT_MOVESET.set(this);

        try {
            // Execute the move (this will handle animations automatically)
            super.performMove(player, moveIndex);
        } finally {
            // Always clean up the thread local
            CURRENT_MOVESET.remove();
        }

        // Check if move actually executed by seeing if breath was consumed
        boolean moveExecuted = !executingMove.getOrDefault(player.getUUID(), false);
        executingMove.remove(player.getUUID());

        if (moveExecuted && getMove(moveIndex) != null) {
            MoveConfiguration config = getMove(moveIndex);
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
    public static DefaultDemonMoveset getCurrentMoveset() {
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
        return "Slash";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "High Jump";
    }

    @Override
    public void onMovePerformed(Player player, int moveIndex, boolean isCrouching) {
        // Track move usage for demons
        // moveIndex -1 = Slash (right-click)
        // moveIndex -2 = High Jump/Stomp (crouch + right-click)
        // moveIndex 0 = Kick
        // moveIndex 1 = Dashing Strike
        // moveIndex 2 = Bite
    }

    /**
     * Simple tick method to reset high jump flag when player lands
     * Call this every tick for demon players
     */
    public static void tickPlayer(Player player) {
        // Reset high jump spam flag when player lands
        if (player.onGround()) {
            hasUsedHighJumpInAir.remove(player.getUUID());
        }
    }

    /**
     * Clean up demon-specific data when player disconnects
     */
    public static void cleanupPlayer(Player player) {
        playerCooldowns.remove(player.getUUID());
        playerSlashStates.remove(player.getUUID());
        canStompAfterHighJump.remove(player.getUUID());
        hasUsedHighJumpInAir.remove(player.getUUID());
        executingMove.remove(player.getUUID());
        executingHighJump.remove(player.getUUID());
        lastHighJumpTick.remove(player.getUUID());
    }
}