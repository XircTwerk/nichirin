package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.breathing.sound.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.network.s2c.MovesetConfigSyncPacket;
import com.xirc.nichirin.common.util.BreathingManager;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import com.xirc.nichirin.common.entity.MovesetCapableNPC;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sound Breathing moveset implementation
 * Sound Breathing excels at combo building and explosive burst damage
 * All attacks build momentum with consecutive hits and create particle explosions
 *
 * Right-click: Tempo Breaker (wide sweep with delayed explosion)
 * Crouch + Right-click: Rhythmic Step (8 block instant dash with trail damage)
 */
public class SoundBreathingMoveset extends AbstractMoveset {

    // Track cooldowns per player per move
    private static final Map<UUID, Map<Integer, Long>> entityCooldowns = new HashMap<>();

    // Track active attacks to prevent breath consumption on failed attempts
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();

    // Thread-local to store current moveset instance for action access
    private static final ThreadLocal<SoundBreathingMoveset> CURRENT_MOVESET = new ThreadLocal<>();

    public SoundBreathingMoveset() {
        super("sound_breathing", "Sound Breathing", MovesetType.BREATHING, createBuilder());

        // Auto-capture configs for GUI display
        captureInitialConfigs();
    }

    private void captureInitialConfigs() {
        // Execute the config creation logic without actually performing the moves
        createAndCaptureTempoBreakerConfig();
        createAndCaptureRhythmicStepConfig();
    }

    private void createAndCaptureTempoBreakerConfig() {
        MoveConfiguration tempConfig = new MoveBuilder("tempo_breaker", "Tempo Breaker")
                .withAnimation("nichirin:tempo_breaker", 8)
                .withTiming(0, 8, 60) // Extended duration to allow delayed explosions
                .withDamage(0f) //explosion is what deals the damage
                .withRange(5.0f) // Wide sweep range
                .withKnockback(0.8f) // Reduced from 1.2f - still too strong
                .withBreathCost(20.0f) // Moderate cost
                .withHitStun(10)
                .withHitboxSize(3.0f)
                .withDescription("Wide sweep that triggers a delayed explosion dealing area damage.")
                .build();
        this.captureRightClickConfig(tempConfig, false);
    }

    private void createAndCaptureRhythmicStepConfig() {
        MoveConfiguration tempConfig = new MoveBuilder("rhythmic_step", "Rhythmic Step")
                .withAnimation("nichirin:rhythmic_step", 9)
                .withTiming(0, 0, 20) // Fast dash with finishing duration
                .withDamage(12.0f) // Moderate damage but hits multiple times
                .withDashSpeed(4.0f) // 4 block dash (halved from 8)
                .withRange(4.0f) // Dash distance (halved from 8)
                .withKnockback(0.5f) // Light knockback during dash
                .withBreathCost(25.0f) // Mobility move cost
                .withHitStun(15) // Good stun for finishing slash
                .withHitboxSize(3.0f)
                .withDescription("Short dash that damages enemies you pass through.")
                .build();
        this.captureRightClickConfig(tempConfig, true);
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:sound_idle")
                .withSpeedMultiplier(1.1f) // Slightly faster for combo building

                // First Form: Roar - AOE slam (INDEX 0 in wheel)
                .withMove(new MoveBuilder("roar", "Roar")
                        .withAnimation("nichirin:roar", 10)
                        .withTiming(160, 50, 20) // 5 second cooldown, moderate windup
                        .withDamage(23.0f) // Good AOE damage
                        .withRange(13.5f) // Tripled from 4.5f (4.5 * 3 = 13.5)
                        .withKnockback(0.3f) // Strong knockback
                        .withBreathCost(25.0f)
                        .withHitStun(10) // 0.5 second stun
                        .withHitboxSize(13.5f) // Full radius
                        .withDescription("AOE slam that hits all enemies in a large radius.")
                        .withAction(entity -> {
                            RoarAttack attack = new RoarAttack();
                            SoundBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(0));
                            }
                            MoveExecutor.executeAttack(entity, attack, "sound_breathing", "roar");
                        })
                )

                // Fourth Form: Constant Resounding Slashes - 360° defense (INDEX 1 in wheel)
                .withMove(new MoveBuilder("constant_resounding_slashes", "Constant Resounding Slashes")
                        .withAnimation("nichirin:constant_resounding_slashes", 12)
                        .withTiming(180, 5, 50)
                        .withDamage(10.0f)
                        .withRange(20.0f) // Increased from 5.5f (1.5x = 8.25f)
                        .withKnockback(0f) // Light knockback to keep enemies close
                        .withBreathCost(25.0f)
                        .withHitStun(10) // Brief stun per hit
                        .withHitboxSize(12.25f) // Full 360° radius
                        .withDescription("Spinning 360° attack that hits all nearby enemies multiple times.")
                        .withAction(entity -> {
                            ConstantResoundingSlashesAttack attack = new ConstantResoundingSlashesAttack();
                            SoundBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(1));
                            }
                            MoveExecutor.executeAttack(entity, attack, "sound_breathing", "constant_resounding_slashes");
                        })
                )

                // Fifth Form: String Performance - Multi-segment dash (INDEX 2 in wheel)
                .withMove(new MoveBuilder("string_performance", "String Performance")
                        .withAnimation("nichirin:string_performance", 15)
                        .withTiming(160, 20, 80) // 8 second cooldown, 4s duration
                        .withDamage(22.0f) // High damage for finale
                        .withDashSpeed(16.0f) // 16 block total dash
                        .withRange(16.0f) // Dash distance
                        .withKnockback(0f) // Light knockback during dash
                        .withBreathCost(40.0f) // Expensive ultimate-style move
                        .withHitStun(20) // Good stun
                        .withHitboxSize(3.5f) // Wide chain hitbox
                        .withDescription("Multi-segment dash that strikes everything along a 16-block path.")
                        .withAction(entity -> {
                            StringPerformanceAttack attack = new StringPerformanceAttack();
                            SoundBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(2)); // Index 2 for String Performance
                            }
                            MoveExecutor.executeAttack(entity, attack, "sound_breathing", "string_performance");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 3; // Amount of moves in wheel
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (isCrouching) {
            // Crouch + Right-click: Rhythmic Step
            return executeRhythmicStep(entity);
        } else {
            // Regular Right-click: Tempo Breaker
            return executeTempoBreaker(entity);
        }
    }

    private boolean executeTempoBreaker(LivingEntity entity) {
        // Remove manual breath consumption - let attack system handle it
        TempoBreakerAttack attack = new TempoBreakerAttack();

        // Use the same config creation method and sync to client
        createAndCaptureTempoBreakerConfig();
        MoveConfiguration tempConfig = getRightClickConfiguration();

        if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
            MovesetConfigSyncPacket packet = new MovesetConfigSyncPacket(
                    "sound_breathing",
                    this.getRightClickConfiguration(),
                    this.getCrouchRightClickConfiguration()
            );
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }

        attack.configure(tempConfig);
        MoveExecutor.executeAttack(entity, attack, "sound_breathing", "tempo_breaker");
        onMovePerformed(entity, -1, false);
        return true;
    }

    private boolean executeRhythmicStep(LivingEntity entity) {
        // Remove manual breath consumption - let attack system handle it
        RhythmicStepAttack attack = new RhythmicStepAttack();

        // Use the same config creation method and sync to client
        createAndCaptureRhythmicStepConfig();
        MoveConfiguration tempConfig = getCrouchRightClickConfiguration();

        if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
            MovesetConfigSyncPacket packet = new MovesetConfigSyncPacket(
                    "sound_breathing",
                    this.getRightClickConfiguration(),
                    this.getCrouchRightClickConfiguration()
            );
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }

        attack.configure(tempConfig);
        MoveExecutor.executeAttack(entity, attack, "sound_breathing", "rhythmic_step");
        onMovePerformed(entity, -2, true);
        return true;
    }

    @Override
    public void performMove(LivingEntity entity, int moveIndex) {
        // Check cooldown before allowing move
        if (!canUseMove(entity, moveIndex)) {
            // Show cooldown message
            MoveConfiguration config = getMove(moveIndex);
            if (config != null) {
                Map<Integer, Long> cooldowns = entityCooldowns.get(entity.getUUID());
                if (cooldowns != null) {
                    Long cooldownEnd = cooldowns.get(moveIndex);
                    if (cooldownEnd != null) {
                        long remaining = (cooldownEnd - entity.level().getGameTime()) / 20;
                        showMessage(entity,
                                Component.literal(config.getDisplayName() + " on cooldown! " + remaining + "s remaining")
                                        .withStyle(style -> style.withColor(0x9900FF)), // Purple color for sound
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
            if (breathCost > 0 && !BreathingManager.hasBreath((Player) entity, breathCost + 0.1f)) {
                showMessage(entity,
                        Component.literal("Not enough breath for " + config.getDisplayName() + "!")
                                .withStyle(style -> style.withColor(0xFF3333)), // Red for no breath
                        true
                );
                return;
            }
        }

        // Check if there are valid targets for offensive moves (only for defensive moves that need them)
        // Most Sound Breathing moves should work without targets

        // Mark that we're executing a move
        executingMove.put(entity.getUUID(), true);

        // Store current moveset instance for access by actions
        CURRENT_MOVESET.set(this);

        try {
            // Execute the move
            super.performMove(entity, moveIndex);
        } finally {
            // Always clean up the thread local
            CURRENT_MOVESET.remove();
        }

        // Check if move actually executed by seeing if breath was consumed
        boolean moveExecuted = !executingMove.getOrDefault(entity.getUUID(), false);
        executingMove.remove(entity.getUUID());

        if (moveExecuted && config != null) {
            // Set cooldown after successful execution
            setMoveCooldown(entity, moveIndex);

            // Send cooldown display packet if on server and has cooldown
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
     * Check if there are valid targets within range
     */
    private boolean hasTargetsInRange(LivingEntity entity, float range) {
        net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(
                entity.getX() - range, entity.getY() - range, entity.getZ() - range,
                entity.getX() + range, entity.getY() + range, entity.getZ() + range
        );

        List<LivingEntity> entities = entity.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != entity && entity.isAlive() && !entity.isSpectator());
        return !entities.isEmpty();
    }

    /**
     * Get the current moveset instance (for use in action lambdas)
     */
    public static SoundBreathingMoveset getCurrentMoveset() {
        return CURRENT_MOVESET.get();
    }

    /**
     * Check if a player can use a specific move (not on cooldown)
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
     * Set a move on cooldown
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
    public int getRightClickMoveIndex(boolean isCrouching) {
        return isCrouching ? -2 : -1; // Not in attack wheel, handled separately
    }

    @Override
    public String getRightClickMoveName() {
        return "Tempo Breaker";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Rhythmic Step";
    }

    @Override
    public void onMovePerformed(LivingEntity entity, int moveIndex, boolean isCrouching) {
        // Sound Breathing specific post-move effects can be added here
        // moveIndex -1 = Tempo Breaker (right-click)
        // moveIndex -2 = Rhythmic Step (crouch + right-click)

        // Could add combo tracking or other Sound-specific mechanics here
    }

    /**
     * Called when a player logs out - clean up their data
     */
    public static void resetCooldowns(LivingEntity entity) {
        entityCooldowns.remove(entity.getUUID());
    }

    public static void cleanupPlayer(LivingEntity entity) {
        entityCooldowns.remove(entity.getUUID());
        executingMove.remove(entity.getUUID());

        // Also clean up combo tracking from the base class
        SoundBreathingAttackBase.resetCombo(entity.getUUID());
    }

    /**
     * Reset combo for a player (can be called externally)
     */
    public static void resetPlayerCombo(LivingEntity entity) {
        SoundBreathingAttackBase.resetCombo(entity.getUUID());
    }

    // ==================== NPC-PLAYER HELPER METHODS ====================

    /**
     * Check if entity has enough breath (works for both Players and NPCs)
     */
    private boolean hasEnoughBreath(LivingEntity entity, float amount) {
        if (entity instanceof Player player) {
            return BreathingManager.hasBreath(player, amount);
        } else if (entity instanceof MovesetCapableNPC npc) {
            return npc.getBreathGauge() >= amount;
        }
        return false;
    }

    /**
     * Show message to entity (only works for Players)
     */
    private void showMessage(LivingEntity entity, Component message, boolean actionBar) {
        if (entity instanceof Player player) {
            player.displayClientMessage(message, actionBar);
        }
        // NPCs don't need messages
    }
}