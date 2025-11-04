package com.xirc.nichirin.common.attack.moveset.demon;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.attack.moves.demon.basic.*;
import com.xirc.nichirin.common.entity.MovesetCapableNPC;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Default demon moveset available to all demons
 * Focuses on basic demonic abilities and blood mechanics
 * Now aligned with breathing moveset patterns while retaining demon-specific features
 * REFACTORED: Works with both Players and NPCs (LivingEntity)
 *
 * Left-click: Gut Punch (high damage, zero knockback, high stun)
 * Right-click: Slash combo (2-stage combo system)
 * Crouch + Right-click: High Jump / Stomp
 */
public class DefaultDemonMoveset extends AbstractMoveset {

    // Track cooldowns per entity per move - UNIFIED for Players and NPCs
    private static final Map<UUID, Map<Integer, Long>> entityCooldowns = new HashMap<>();

    // Track active attacks to prevent resource consumption on failed attempts
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();

    // Immediate execution flags to prevent rapid-fire spam
    private static final Map<UUID, Boolean> executingHighJump = new HashMap<>();

    // Manual combo system for slash - works for both Players and NPCs
    private static final Map<UUID, SlashComboState> entitySlashStates = new HashMap<>();

    // Track high jump state per entity - only allow stomp after high jump
    private static final Map<UUID, Boolean> canStompAfterHighJump = new HashMap<>();

    // Simple flag to prevent high jump spam - resets when entity lands
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
        createAndCaptureLeftClickConfig();
        createAndCaptureSlashConfig();
        createAndCaptureHighJumpConfig();
    }

    private void createAndCaptureLeftClickConfig() {
        MoveConfiguration tempConfig = new MoveBuilder("demon_gut_punch", "Gut Punch")
                .withAnimation("nichirin:demon_gut_punch", 6)
                .withTiming(15, 5, 10) // 1 second cooldown, quick windup, short duration
                .withDamage(8.0f) // High damage
                .withRange(1.5f) // Very close range
                .withKnockback(0.1f) // NO knockback
                .withHitStun(15) // High stun
                .withHitboxSize(1.5f)
                .withDescription("Powerful close-range punch that stuns enemies")
                .build();
        this.captureLeftClickConfig(tempConfig);
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
        this.captureRightClickConfig(tempConfig, false); // Keep for right-click
    }

    private void createAndCaptureHighJumpConfig() {
        MoveConfiguration tempConfig = new MoveBuilder("high_jump", "High Jump")
                .withAnimation("nichirin:demon_high_jump", 8)
                .withTiming(140, 0, 5) // 5 second cooldown for high jump
                .withDescription("Launch 5 blocks into the air, crouch right-click mid-air for stomp attack")
                .build();
        this.captureRightClickConfig(tempConfig, true);
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:demon_idle")
                .withSpeedMultiplier(1.05f) // Slight speed boost for demons

                // LEFT CLICK: Gut Punch attack
                .withLeftClickMove(new MoveBuilder("demon_gut_punch", "Gut Punch")
                                .withAnimation("nichirin:demon_punch", 6)
                                .withTiming(10, 5, 10) // 1 second cooldown, quick windup, short duration
                                .withDamage(2.0f) // High damage
                                .withRange(1.5f) // Very close range
                                .withKnockback(0.1f) // NO knockback
                                .withHitStun(15) // High stun
                                .withHitboxSize(1.5f)
                                .withDescription("Powerful close-range punch that stuns enemies")
                        // NO ACTION - handled in handleLeftClick override
                )

                // Kick - High knockback front push (INDEX 0)
                .withMove(new MoveBuilder("demon_kick", "Kick")
                        .withAnimation("nichirin:demon_kick", 8)
                        .withTiming(60, 5, 15) // 3 second cooldown, quick windup, 15 tick duration
                        .withDamage(6.0f)
                        .withRange(2.5f)
                        .withKnockback(1f)
                        .withHitStun(25)
                        .withHitboxSize(2.0f)
                        .withDescription("Powerful front kick with high knockback and crowd control")
                        .withAction(entity -> {
                            // FIXED: Works with LivingEntity instead of Player
                            DemonKickAttack kickAttack = new DemonKickAttack();
                            DefaultDemonMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                kickAttack.configure(moveset.getMove(0));
                            }
                            MoveExecutor.executeAttack(entity, kickAttack, "default_demon", "demon_kick");
                        })
                )

                // Dashing Strike - Dash 4 blocks forward with punch (INDEX 1)
                .withMove(new MoveBuilder("dashing_strike", "Dashing Strike")
                        .withAnimation("nichirin:demon_dash_strike", 10)
                        .withTiming(80, 8, 20) // 4 second cooldown, dash windup, 20 tick duration
                        .withDamage(12.0f)
                        .withDashSpeed(4.0f)
                        .withRange(4.0f)
                        .withKnockback(0.2f)
                        .withHitStun(20)
                        .withHitboxSize(2)
                        .withDescription("Dash forward 4 blocks and deliver a devastating punch")
                        .withAction(entity -> {
                            // FIXED: Works with LivingEntity instead of Player
                            DemonDashStrikeAttack dashStrikeAttack = new DemonDashStrikeAttack();
                            DefaultDemonMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                dashStrikeAttack.configure(moveset.getMove(1));
                            }
                            MoveExecutor.executeAttack(entity, dashStrikeAttack, "default_demon", "dashing_strike");
                        })
                )

                // Bite - Life steal attack (INDEX 2)
                .withMove(new MoveBuilder("demon_bite", "Bite")
                        .withAnimation("nichirin:demon_bite", 9)
                        .withTiming(100, 5, 15) // 5 second cooldown, quick windup, 15 tick duration
                        .withDamage(8.0f)
                        .withRange(2.0f)
                        .withKnockback(0.1f)
                        .withHitStun(20)
                        .withHitboxSize(1.8f)
                        .withDescription("Bite attack that heals you and adds blood points")
                        .withAction(entity -> {
                            // FIXED: Works with LivingEntity instead of Player
                            DemonBiteAttack biteAttack = new DemonBiteAttack();
                            DefaultDemonMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                biteAttack.configure(moveset.getMove(2));
                            }
                            MoveExecutor.executeAttack(entity, biteAttack, "default_demon", "demon_bite");
                        })
                );
    }

    /**
     * REFACTORED: Override left-click to support both Players and NPCs
     */
    @Override
    public boolean handleLeftClick(LivingEntity entity) {
        if (entity.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return true;
        }

        // Check cooldown
        if (!canUseMove(entity, -3)) {
            // Only show message for players
            if (entity instanceof Player player) {
                Map<Integer, Long> cooldowns = entityCooldowns.get(entity.getUUID());
                if (cooldowns != null) {
                    Long cooldownEnd = cooldowns.get(-3);
                    if (cooldownEnd != null) {
                        long remaining = (cooldownEnd - entity.level().getGameTime());
                        player.displayClientMessage(
                                Component.literal("Gut Punch on cooldown! " + (remaining / 20.0f) + "s remaining")
                                        .withStyle(style -> style.withColor(0xFF5555)),
                                true
                        );
                    }
                }
            }
            return true;
        }

        MoveConfiguration gutPunchConfig = getLeftClickConfiguration();
        if (gutPunchConfig == null) return false;

        // Apply stun effect
        applyMoveStun(entity, gutPunchConfig);

        // Trigger animation (works for both Player and NPC)
        if (gutPunchConfig.animationId != null) {
            triggerAnimation(entity, gutPunchConfig.animationId.getPath());
        }

        // Execute gut punch attack
        DemonGutPunchAttack gutPunchAttack = new DemonGutPunchAttack();
        gutPunchAttack.configure(gutPunchConfig);
        MoveExecutor.executeAttack(entity, gutPunchAttack, "default_demon", "demon_gut_punch");

        // Set cooldown
        setMoveCooldown(entity, -3, gutPunchConfig.getCooldownOrDefault(0));

        // Send cooldown display for players
        if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer
                && gutPunchConfig.getCooldownOrDefault(0) > 0) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(gutPunchConfig.getDisplayName());
            buf.writeInt(gutPunchConfig.getCooldownOrDefault(0));

            NetworkManager.sendToPlayer(serverPlayer, new ResourceLocation("nichirin", "cooldown_display"), buf);
        }

        return true;
    }

    /**
     * REFACTORED: Override right-click to support both Players and NPCs
     */
    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (entity.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            return true;
        }

        if (isCrouching) {
            return handleCrouchRightClick(entity);
        } else {
            return handleSlashCombo(entity);
        }
    }

    /**
     * REFACTORED: Handle slash combo for both Players and NPCs
     */
    private boolean handleSlashCombo(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();
        SlashComboState comboState = entitySlashStates.computeIfAbsent(entityUUID, k -> new SlashComboState());

        // Check if combo window expired
        if (!comboState.canContinueCombo()) {
            comboState.reset();
        }

        int currentStage = comboState.currentStage;

        // Stage 0: First slash (no cooldown check)
        if (currentStage == 0) {
            return executeSlashStage(entity, 0, comboState);
        }
        // Stage 1: Second slash (follow-up)
        else if (currentStage == 1) {
            return executeSlashStage(entity, 1, comboState);
        }
        // Stage 2: Combo complete, check cooldown for restart
        else {
            if (!canUseMove(entity, -1)) {
                // Only show message for players
                if (entity instanceof Player player) {
                    Map<Integer, Long> cooldowns = entityCooldowns.get(entityUUID);
                    if (cooldowns != null) {
                        Long cooldownEnd = cooldowns.get(-1);
                        if (cooldownEnd != null) {
                            long remaining = (cooldownEnd - entity.level().getGameTime());
                            player.displayClientMessage(
                                    Component.literal("Slash on cooldown! " + (remaining / 20.0f) + "s remaining")
                                            .withStyle(style -> style.withColor(0xFF5555)),
                                    true
                            );
                        }
                    }
                }
                return true;
            }

            // Reset combo and start fresh
            comboState.reset();
            return executeSlashStage(entity, 0, comboState);
        }
    }

    /**
     * Execute a specific slash stage
     */
    private boolean executeSlashStage(LivingEntity entity, int stage, SlashComboState comboState) {
        MoveConfiguration slashConfig;
        String animationId;

        if (stage == 0) {
            // First slash
            slashConfig = new MoveBuilder("demon_slash_1", "Slash")
                    .withAnimation("nichirin:demon_slash", 6)
                    .withTiming(0, 0, 20)
                    .withDamage(4.0f)
                    .withRange(3.0f)
                    .withKnockback(0f)
                    .withHitStun(15)
                    .withHitboxSize(2.0f)
                    .build();
            animationId = "nichirin:demon_slash";
        } else {
            // Second slash (stronger)
            slashConfig = new MoveBuilder("demon_slash_2", "Slash Finisher")
                    .withAnimation("nichirin:demon_slash_2", 7)
                    .withTiming(60, 0, 25) // 3 second cooldown after second hit
                    .withDamage(6.0f)
                    .withRange(3.0f)
                    .withKnockback(0.5f)
                    .withHitStun(20)
                    .withHitboxSize(2.2f)
                    .build();
            animationId = "nichirin:demon_slash_2";
        }

        // Apply stun
        applyMoveStun(entity, slashConfig);

        // Trigger animation (works for both Player and NPC)
        triggerAnimation(entity, animationId);

        // Execute slash attack
        DemonSlashAttack slashAttack = new DemonSlashAttack();
        slashAttack.configure(slashConfig);
        MoveExecutor.executeAttack(entity, slashAttack, "default_demon", stage == 0 ? "demon_slash_1" : "demon_slash_2");

        // Advance combo state
        comboState.nextStage();

        // Set cooldown only after second slash
        if (stage == 1) {
            setMoveCooldown(entity, -1, slashConfig.getCooldownOrDefault(0));

            // Send cooldown display for players
            if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeUtf("Slash");
                buf.writeInt(slashConfig.getCooldownOrDefault(0));

                NetworkManager.sendToPlayer(serverPlayer, new ResourceLocation("nichirin", "cooldown_display"), buf);
            }
        }

        return true;
    }

    /**
     * REFACTORED: Handle crouch + right-click (High Jump / Stomp) for both Players and NPCs
     */
    private boolean handleCrouchRightClick(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();

        // Check if entity can stomp (mid-air after high jump)
        if (canStompAfterHighJump.getOrDefault(entityUUID, false)) {
            return executeStompAttack(entity);
        }

        // Otherwise, try to execute high jump
        return executeHighJump(entity);
    }

    /**
     * Execute high jump for both Players and NPCs
     */
    private boolean executeHighJump(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();

        // Prevent spam in same tick
        long currentTick = entity.level().getGameTime();
        Long lastTick = lastHighJumpTick.get(entityUUID);
        if (lastTick != null && lastTick == currentTick) {
            return true;
        }

        // Check if already used in air
        if (!entity.onGround() && hasUsedHighJumpInAir.getOrDefault(entityUUID, false)) {
            return true;
        }

        // Check cooldown
        if (!canUseMove(entity, -2)) {
            // Only show message for players
            if (entity instanceof Player player) {
                Map<Integer, Long> cooldowns = entityCooldowns.get(entityUUID);
                if (cooldowns != null) {
                    Long cooldownEnd = cooldowns.get(-2);
                    if (cooldownEnd != null) {
                        long remaining = (cooldownEnd - entity.level().getGameTime());
                        player.displayClientMessage(
                                Component.literal("High Jump on cooldown! " + (remaining / 20.0f) + "s remaining")
                                        .withStyle(style -> style.withColor(0xFF5555)),
                                true
                        );
                    }
                }
            }
            return true;
        }

        // Prevent double execution
        if (executingHighJump.getOrDefault(entityUUID, false)) {
            return true;
        }

        executingHighJump.put(entityUUID, true);
        lastHighJumpTick.put(entityUUID, currentTick);

        try {
            MoveConfiguration highJumpConfig = getCrouchRightClickConfiguration();
            if (highJumpConfig == null) return false;

            // Apply stun
            applyMoveStun(entity, highJumpConfig);

            // Trigger animation (works for both Player and NPC)
            if (highJumpConfig.animationId != null) {
                triggerAnimation(entity, highJumpConfig.animationId.getPath());
            }

            // Launch entity upward
            entity.setDeltaMovement(entity.getDeltaMovement().x, 1.5, entity.getDeltaMovement().z);
            entity.hurtMarked = true;
            entity.hasImpulse = true;

            // Mark that high jump was used in air
            hasUsedHighJumpInAir.put(entityUUID, true);

            // Enable stomp ability
            canStompAfterHighJump.put(entityUUID, true);

            // Set cooldown
            setMoveCooldown(entity, -2, highJumpConfig.getCooldownOrDefault(0));

            // Send cooldown display for players
            if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                buf.writeUtf("High Jump");
                buf.writeInt(highJumpConfig.getCooldownOrDefault(0));

                NetworkManager.sendToPlayer(serverPlayer, new ResourceLocation("nichirin", "cooldown_display"), buf);
            }

            return true;

        } finally {
            executingHighJump.remove(entityUUID);
        }
    }

    /**
     * Execute stomp attack for both Players and NPCs
     */
    private boolean executeStompAttack(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();

        MoveConfiguration stompConfig = new MoveBuilder("demon_stomp", "Stomp")
                .withAnimation("nichirin:demon_stomp", 8)
                .withTiming(0, 0, 15)
                .withDamage(10.0f)
                .withRange(4.0f)
                .withKnockback(0.8f)
                .withHitStun(30)
                .withHitboxSize(3.0f)
                .build();

        // Apply stun
        applyMoveStun(entity, stompConfig);

        // Trigger animation for players
        if (entity instanceof ServerPlayer serverPlayer) {
            String animationName = stompConfig.animationId.getPath();
            PlayerAnimationPacket packet = new PlayerAnimationPacket(serverPlayer.getId(), animationName);
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }
        // Animation for NPCs is handled by triggerAnimation in applyMoveStun

        // Execute stomp
        DemonStompAttack stompAttack = new DemonStompAttack();
        stompAttack.configure(stompConfig);
        MoveExecutor.executeAttack(entity, stompAttack, "default_demon", "demon_stomp");

        // Clear stomp ability after use
        canStompAfterHighJump.remove(entityUUID);

        return true;
    }

    /**
     * Set a move on cooldown with custom cooldown time - UNIFIED for Players and NPCs
     */
    private void setMoveCooldown(LivingEntity entity, int moveIndex, int cooldownTicks) {
        long currentTime = entity.level().getGameTime();
        long cooldownEnd = currentTime + cooldownTicks;

        entityCooldowns.computeIfAbsent(entity.getUUID(), k -> new HashMap<>())
                .put(moveIndex, cooldownEnd);
    }

    /**
     * REFACTORED: Override performMove to support both Players and NPCs
     */
    @Override
    public void performMove(LivingEntity entity, int moveIndex) {
        // Check cooldown before allowing move
        if (!canUseMove(entity, moveIndex)) {
            // Show cooldown message only for players
            if (entity instanceof Player player) {
                MoveConfiguration config = getMove(moveIndex);
                if (config != null) {
                    Map<Integer, Long> cooldowns = entityCooldowns.get(entity.getUUID());
                    if (cooldowns != null) {
                        Long cooldownEnd = cooldowns.get(moveIndex);
                        if (cooldownEnd != null) {
                            long remaining = (cooldownEnd - entity.level().getGameTime());
                            player.displayClientMessage(
                                    Component.literal("Move on cooldown! " + (remaining / 20.0f) + "s remaining")
                                            .withStyle(style -> style.withColor(0xFF5555)),
                                    true
                            );
                        }
                    }
                }
            }
            return;
        }

        // Demons don't require breath - skip resource checks

        // Mark that we're executing a move
        executingMove.put(entity.getUUID(), true);

        // Store current moveset instance for access by actions
        CURRENT_MOVESET.set(this);

        try {
            // Execute the move (this will handle animations automatically)
            super.performMove(entity, moveIndex);
        } finally {
            // Always clean up the thread local
            CURRENT_MOVESET.remove();
        }

        // Check if move actually executed
        boolean moveExecuted = !executingMove.getOrDefault(entity.getUUID(), false);
        executingMove.remove(entity.getUUID());

        if (moveExecuted && getMove(moveIndex) != null) {
            MoveConfiguration config = getMove(moveIndex);
            // Set cooldown after successful execution
            setMoveCooldown(entity, moveIndex);

            // Send cooldown display packet if on server and entity is a player
            if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer
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
     * Check if an entity can use a specific move (not on cooldown) - UNIFIED for Players and NPCs
     */
    private boolean canUseMove(LivingEntity entity, int moveIndex) {
        MoveConfiguration config = getMove(moveIndex);
        if (config == null || config.getCooldownOrDefault(0) <= 0) {
            return true; // No cooldown
        }

        Map<Integer, Long> cooldowns = entityCooldowns.get(entity.getUUID());
        if (cooldowns == null) {
            return true; // No cooldowns tracked yet
        }

        Long cooldownEnd = cooldowns.get(moveIndex);
        if (cooldownEnd == null) {
            return true; // Move never used
        }

        long currentTime = entity.level().getGameTime();
        return currentTime >= cooldownEnd;
    }

    /**
     * Set a move on cooldown - UNIFIED for Players and NPCs
     */
    private void setMoveCooldown(LivingEntity entity, int moveIndex) {
        MoveConfiguration config = getMove(moveIndex);
        if (config == null || config.getCooldownOrDefault(0) <= 0) {
            return; // No cooldown
        }

        long cooldownEnd = entity.level().getGameTime() + config.getCooldownOrDefault(0);
        entityCooldowns.computeIfAbsent(entity.getUUID(), k -> new HashMap<>())
                .put(moveIndex, cooldownEnd);
    }

    @Override
    public int getLeftClickMoveIndex() {
        return -3; // Left click slash combo
    }

    @Override
    public int getRightClickMoveIndex(boolean isCrouching) {
        return isCrouching ? -2 : -1; // Not in attack wheel, handled separately
    }

    @Override
    public String getLeftClickMoveName() {
        return "Gut Punch";
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
        // moveIndex -3 = Gut Punch (left-click)
        // moveIndex -1 = Slash (right-click)
        // moveIndex -2 = High Jump/Stomp (crouch + right-click)
        // moveIndex 0 = Kick
        // moveIndex 1 = Dashing Strike
        // moveIndex 2 = Bite
    }

    /**
     * Simple tick method to reset high jump flag when entity lands - UNIFIED for Players and NPCs
     * Call this every tick for demon entities
     */
    public static void tickEntity(LivingEntity entity) {
        // Reset high jump spam flag when entity lands
        if (entity.onGround()) {
            hasUsedHighJumpInAir.remove(entity.getUUID());
        }
    }

    /**
     * Legacy player version for backwards compatibility
     */
    public static void tickPlayer(Player player) {
        tickEntity(player);
    }

    /**
     * Clean up demon-specific data when entity disconnects/is removed - UNIFIED
     */
    public static void cleanupEntity(LivingEntity entity) {
        UUID entityUUID = entity.getUUID();
        entityCooldowns.remove(entityUUID);
        entitySlashStates.remove(entityUUID);
        canStompAfterHighJump.remove(entityUUID);
        hasUsedHighJumpInAir.remove(entityUUID);
        executingMove.remove(entityUUID);
        executingHighJump.remove(entityUUID);
        lastHighJumpTick.remove(entityUUID);
    }

    /**
     * Legacy player version for backwards compatibility
     */
    public static void cleanupPlayer(Player player) {
        cleanupEntity(player);
    }
}