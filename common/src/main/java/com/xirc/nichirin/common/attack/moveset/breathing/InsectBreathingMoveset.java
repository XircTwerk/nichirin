package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.breathing.insect.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.network.s2c.MovesetConfigSyncPacket;
import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import com.xirc.nichirin.common.util.EntityResources;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.phys.AABB;

/**
 * Insect Breathing moveset implementation
 * Insect Breathing excels at precision strikes, venom, and agile movement
 * Focus on high mobility, single-target damage, and poison effects
 *
 * Right-click: Quick Sting (rapid thrust with poison)
 * Crouch + Right-click: Poison Dash (short dash with venom trail)
 */
public class InsectBreathingMoveset extends AbstractMoveset {

    // Track cooldowns per player per move
    private static final Map<UUID, Map<Integer, Long>> entityCooldowns = new HashMap<>();

    // Track active attacks to prevent breath consumption on failed attempts
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();

    // Thread-local to store current moveset instance for action access
    private static final ThreadLocal<InsectBreathingMoveset> CURRENT_MOVESET = new ThreadLocal<>();

    public InsectBreathingMoveset() {
        super("insect_breathing", "Insect Breathing", MovesetType.BREATHING, createBuilder());

        // Auto-capture configs for GUI display
        captureInitialConfigs();
    }

    private void captureInitialConfigs() {
        // Execute the config creation logic without actually performing the moves
        createAndCaptureQuickStingConfig();
        createAndCaptureBeeStingConfig();
    }

    private void createAndCaptureQuickStingConfig() {
        MoveConfiguration tempConfig = new MoveBuilder("quick_sting", "Quick Sting")
                .withAnimation("nichirin:quick_sting", 6)
                .withTiming(5, 3, 14)
                .withDamage(2.0f)
                .withRange(2.5f)
                .withKnockback(0f)
                .withBreathCost(10.0f)
                .withHitStun(15)
                .withHitboxSize(0.5f)
                .withDescription("Fast low-damage thrust that poisons on hit.")
                .build();
        this.captureRightClickConfig(tempConfig, false);
    }

    private void createAndCaptureBeeStingConfig() {
        MoveConfiguration tempConfig = new MoveBuilder("bee_sting", "Bee Sting")
                .withAnimation("nichirin:bee_sting", 9)
                .withTiming(0, 6, 9)
                .withDamage(5.0f)
                .withDashSpeed(6.0f)
                .withRange(6.0f)
                .withKnockback(0.1f)
                .withBreathCost(20.0f)
                .withHitStun(10)
                .withHitboxSize(2.0f)
                .withDescription("Short dash strike that deals moderate damage.")
                .build();
        this.captureRightClickConfig(tempConfig, true);
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:insect_idle")
                .withSpeedMultiplier(1.3f) // Very fast and agile

                // First Form: Butterfly - Precision dash strike (INDEX 0 in wheel)
                .withMove(new MoveBuilder("butterfly", "Butterfly")
                        .withAnimation("nichirin:butterfly", 8)
                        .withTiming(120, 8, 28) // 6 second cooldown, quick windup, LONGER duration for 2-phase attack
                        .withDamage(11.0f) // High single-target damage
                        .withDashSpeed(5.0f) // Dash speed (changed from withTeleportDistance)
                        .withRange(10.0f) // Lock-on range
                        .withKnockback(0.3f) // Light knockback
                        .withBreathCost(20.0f)
                        .withHitStun(30) // Good stun for precision strike
                        .withHitboxSize(2f) // Small precise hitbox
                        .withDescription("Lock-on dash strike that deals high single-target damage.")
                        .withAction(entity -> {
                            ButterflyAttack attack = new ButterflyAttack();
                            InsectBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(0));
                            }
                            MoveExecutor.executeAttack(entity, attack, "insect_breathing", "butterfly");
                        })
                )

                // Third Form: Dragonfly - Multi-hit lock-on (INDEX 1 in wheel)
                .withMove(new MoveBuilder("dragonfly", "Dragonfly")
                        .withAnimation("nichirin:dragonfly", 12)
                        .withTiming(180, 15, 21) // 9 second cooldown, root during windup
                        .withDamage(2.0f) // 6 hits = 36 total damage
                        .withRange(6.0f) // Lock-on range
                        .withKnockback(0.0f) // Minimal knockback to keep target close
                        .withBreathCost(25.0f) // Higher cost for multi-hit
                        .withHitStun(5) // Very short per hit, final hit has more
                        .withHitboxSize(2.0f) // Target lock area
                        .withDescription("Lock-on multi-hit that lands 6 rapid strikes on a single target.")
                        .withAction(entity -> {
                            DragonflyAttack attack = new DragonflyAttack();
                            InsectBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(1));
                            }
                            MoveExecutor.executeAttack(entity, attack, "insect_breathing", "dragonfly");
                        })
                )

                // Fourth Form: Centipede - Zigzag dash finisher (INDEX 2 in wheel)
                .withMove(new MoveBuilder("centipede", "Centipede")
                        .withAnimation("nichirin:centipede", 15)
                        .withTiming(240, 20, 35) // 12 second cooldown, complex movement
                        .withDamage(14.0f) // High damage finisher
                        .withDashSpeed(4.0f) // Multiple zigzag dashes (was 8.0f)
                        .withRange(2.0f)
                        .withKnockback(0.8f) // Strong finisher knockback
                        .withBreathCost(45.0f) // Expensive ultimate-level move
                        .withHitStun(40) // Strong stun on finisher
                        .withHitboxSize(2.5f) // Larger finisher hitbox
                        .withDescription("Zigzag multi-dash finisher with high damage and strong knockback.")
                        .withAction(entity -> {
                            CentipedeAttack attack = new CentipedeAttack();
                            InsectBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(2));
                            }
                            MoveExecutor.executeAttack(entity, attack, "insect_breathing", "centipede");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 3; //three forms in attack wheel
    }

    @Override
    public boolean handleRightClick(LivingEntity entity, boolean isCrouching) {
        if (!canPerformMoves(entity)) return true;

        if (isCrouching) {
            // Crouch + Right-click: Poison Dash
            return executeBeeSting(entity);
        } else {
            // Regular Right-click: Quick Sting
            return executeQuickSting(entity);
        }
    }

    private boolean executeQuickSting(LivingEntity entity) {
        // Use the same config creation method and sync to client
        createAndCaptureQuickStingConfig();
        MoveConfiguration tempConfig = getRightClickConfiguration();
        if (tempConfig == null) return false;
        if (!hasResourcesForMove(entity, tempConfig)) return true;

        if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
            MovesetConfigSyncPacket packet = new MovesetConfigSyncPacket(
                    "insect_breathing",
                    this.getRightClickConfiguration(),
                    this.getCrouchRightClickConfiguration()
            );
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }

        QuickStingAttack attack = new QuickStingAttack();
        attack.configure(tempConfig);
        MoveExecutor.executeAttack(entity, attack, "insect_breathing", "quick_sting");
        onMovePerformed(entity, -1, false);
        return true;
    }

    private boolean executeBeeSting(LivingEntity entity) {
        // Use the same config creation method and sync to client
        createAndCaptureBeeStingConfig();
        MoveConfiguration tempConfig = getCrouchRightClickConfiguration();
        if (tempConfig == null) return false;
        if (!hasResourcesForMove(entity, tempConfig)) return true;

        if (!entity.level().isClientSide && entity instanceof ServerPlayer serverPlayer) {
            MovesetConfigSyncPacket packet = new MovesetConfigSyncPacket(
                    "insect_breathing",
                    this.getRightClickConfiguration(),
                    this.getCrouchRightClickConfiguration()
            );
            NichirinPacketRegistry.sendToPlayer(packet, serverPlayer);
        }

        BeeStingAttack attack = new BeeStingAttack();
        attack.configure(tempConfig);
        MoveExecutor.executeAttack(entity, attack, "insect_breathing", "bee_sting");
        onMovePerformed(entity, -2, true);
        return true;
    }

    @Override
    public void performMove(LivingEntity entity, int moveIndex) {
        if (!canPerformMoves(entity)) {
            return;
        }

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
                        EntityResources.sendMessage(entity, Component.literal(config.getDisplayName() + " on cooldown! " + remaining + "s remaining")
                                        .withStyle(style -> style.withColor(0xAA00FF)), // Purple color for insect
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
            if (breathCost > 0 && !EntityResources.hasBreath(entity, breathCost + 0.1f)) {
                EntityResources.sendMessage(entity, Component.literal("Not enough breath for " + config.getDisplayName() + "!")
                                .withStyle(style -> style.withColor(0xFF3333)), // Red for no breath
                        true
                );
                return;
            }
        }

        // Removed range checks - all moves should execute regardless of targets

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
                CooldownDisplayPacket.sendToClient(serverPlayer, "insect_breathing", config);
            }
        }
    }

    /**
     * Check if there are valid targets within range for targeted moves
     */
    private boolean hasTargetsInRange(LivingEntity entity, float range) {
        AABB searchBox = new AABB(
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
    public static InsectBreathingMoveset getCurrentMoveset() {
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
        return "Quick Sting";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Poison Dash";
    }

    @Override
    public void onMovePerformed(LivingEntity entity, int moveIndex, boolean isCrouching) {
        // Insect Breathing specific post-move effects can be added here
        // moveIndex -1 = Quick Sting (right-click)
        // moveIndex -2 = Poison Dash (crouch + right-click)
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
    }

}
