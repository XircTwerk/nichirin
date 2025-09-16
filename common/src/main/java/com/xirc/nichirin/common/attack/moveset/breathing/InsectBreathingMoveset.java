package com.xirc.nichirin.common.attack.moveset.breathing;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.insect.*;
import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.util.BreathingManager;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private static final Map<UUID, Map<Integer, Long>> playerCooldowns = new HashMap<>();

    // Track active attacks to prevent breath consumption on failed attempts
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();

    // Thread-local to store current moveset instance for action access
    private static final ThreadLocal<InsectBreathingMoveset> CURRENT_MOVESET = new ThreadLocal<>();

    public InsectBreathingMoveset() {
        super("insect_breathing", "Insect Breathing", MovesetType.BREATHING, createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:insect_idle")
                .withSpeedMultiplier(1.3f) // Very fast and agile

                // First Form: Butterfly - Precision dash strike (INDEX 0 in wheel)
                .withMove(new MoveBuilder("butterfly", "Butterfly")
                        .withAnimation("nichirin:butterfly", 8)
                        .withTiming(120, 8, 40) // 6 second cooldown, quick windup, LONGER duration for 2-phase attack
                        .withDamage(18.0f) // High single-target damage
                        .withDashSpeed(5.0f) // Dash speed (changed from withTeleportDistance)
                        .withRange(10.0f) // Lock-on range
                        .withKnockback(0.3f) // Light knockback
                        .withBreathCost(20.0f)
                        .withHitStun(25) // Good stun for precision strike
                        .withHitboxSize(1.5f) // Small precise hitbox
                        .withAction(player -> {
                            ButterflyAttack attack = new ButterflyAttack();
                            InsectBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(0));
                            }
                            MoveExecutor.executeAttack(player, attack, "insect_breathing", "butterfly");
                        })
                )

                // Third Form: Dragonfly - Multi-hit lock-on (INDEX 1 in wheel)
                .withMove(new MoveBuilder("dragonfly", "Dragonfly")
                        .withAnimation("nichirin:dragonfly", 12)
                        .withTiming(180, 15, 30) // 9 second cooldown, root during windup
                        .withDamage(6.0f) // 6 hits = 36 total damage
                        .withRange(6.0f) // Lock-on range
                        .withKnockback(0.05f) // Minimal knockback to keep target close
                        .withBreathCost(30.0f) // Higher cost for multi-hit
                        .withHitStun(5) // Very short per hit, final hit has more
                        .withHitboxSize(2.0f) // Target lock area
                        .withAction(player -> {
                            DragonflyAttack attack = new DragonflyAttack();
                            InsectBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(1));
                            }
                            MoveExecutor.executeAttack(player, attack, "insect_breathing", "dragonfly");
                        })
                )

                // Fourth Form: Centipede - Zigzag dash finisher (INDEX 2 in wheel)
                .withMove(new MoveBuilder("centipede", "Centipede")
                        .withAnimation("nichirin:centipede", 15)
                        .withTiming(240, 20, 50) // 12 second cooldown, complex movement
                        .withDamage(22.0f) // High damage finisher
                        .withDashSpeed(4.0f) // Multiple zigzag dashes (was 8.0f)
                        .withRange(2.0f)
                        .withKnockback(0.8f) // Strong finisher knockback
                        .withBreathCost(45.0f) // Expensive ultimate-level move
                        .withHitStun(40) // Strong stun on finisher
                        .withHitboxSize(2.5f) // Larger finisher hitbox
                        .withAction(player -> {
                            CentipedeAttack attack = new CentipedeAttack();
                            InsectBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(2));
                            }
                            MoveExecutor.executeAttack(player, attack, "insect_breathing", "centipede");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 3; //three forms in attack wheel
    }

    @Override
    public boolean handleRightClick(Player player, boolean isCrouching) {
        if (isCrouching) {
            // Crouch + Right-click: Poison Dash
            return executeBeeSting(player);
        } else {
            // Regular Right-click: Quick Sting
            return executeQuickSting(player);
        }
    }

    private boolean executeQuickSting(Player player) {
        // Remove manual breath consumption - let attack system handle it
        QuickStingAttack attack = new QuickStingAttack();

        MoveConfiguration tempConfig = new MoveBuilder("quick_sting", "Quick Sting")
                .withAnimation("nichirin:quick_sting", 6)
                .withTiming(5, 3, 20)
                .withDamage(9.0f)
                .withRange(4.0f)
                .withKnockback(0f)
                .withBreathCost(10.0f)
                .withHitStun(15)
                .withHitboxSize(1.5f)
                .build();

        attack.configure(tempConfig);
        MoveExecutor.executeAttack(player, attack, "insect_breathing", "quick_sting");
        onMovePerformed(player, -1, false);
        return true;
    }

    private boolean executeBeeSting(Player player) {
        // Remove manual breath consumption - let attack system handle it
        BeeStingAttack attack = new BeeStingAttack();

        MoveConfiguration tempConfig = new MoveBuilder("bee_sting", "Bee Sting")
                .withAnimation("nichirin:bee_sting", 9)
                .withTiming(0, 6, 13)
                .withDamage(8.0f)
                .withDashSpeed(6.0f)
                .withRange(6.0f)
                .withKnockback(0.1f)
                .withBreathCost(20.0f)
                .withHitStun(10)
                .withHitboxSize(2.0f)
                .build();

        attack.configure(tempConfig);
        MoveExecutor.executeAttack(player, attack, "insect_breathing", "bee_sting");
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
            if (breathCost > 0 && !BreathingManager.hasBreath(player, breathCost + 0.1f)) {
                player.displayClientMessage(
                        Component.literal("Not enough breath for " + config.getDisplayName() + "!")
                                .withStyle(style -> style.withColor(0xFF3333)), // Red for no breath
                        true
                );
                return;
            }
        }

        // Removed range checks - all moves should execute regardless of targets

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
     * Check if there are valid targets within range for targeted moves
     */
    private boolean hasTargetsInRange(Player player, float range) {
        net.minecraft.world.phys.AABB searchBox = new net.minecraft.world.phys.AABB(
                player.getX() - range, player.getY() - range, player.getZ() - range,
                player.getX() + range, player.getY() + range, player.getZ() + range
        );

        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> entity != player && entity.isAlive() && !entity.isSpectator());
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
        return "Quick Sting";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Poison Dash";
    }

    @Override
    public void onMovePerformed(Player player, int moveIndex, boolean isCrouching) {
        // Insect Breathing specific post-move effects can be added here
        // moveIndex -1 = Quick Sting (right-click)
        // moveIndex -2 = Poison Dash (crouch + right-click)
    }

    /**
     * Called when a player logs out - clean up their data
     */
    public static void cleanupPlayer(Player player) {
        playerCooldowns.remove(player.getUUID());
        executingMove.remove(player.getUUID());
    }
}