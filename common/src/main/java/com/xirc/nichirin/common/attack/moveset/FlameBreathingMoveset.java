package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.flame.*;
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
 * Flame Breathing moveset implementation
 * Flame Breathing excels at crowd control and burning effects
 * All attacks hit multiple enemies and apply fire damage
 *
 * Right-click: Pommel Slash (6 rapid slashes)
 * Crouch + Right-click: Thrust Attack (dash forward with thrust)
 */
public class FlameBreathingMoveset extends AbstractMoveset {

    // Track cooldowns per player per move
    private static final Map<UUID, Map<Integer, Long>> playerCooldowns = new HashMap<>();

    // Track active attacks to prevent breath consumption on failed attempts
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();

    // Thread-local to store current moveset instance for action access
    private static final ThreadLocal<FlameBreathingMoveset> CURRENT_MOVESET = new ThreadLocal<>();

    public FlameBreathingMoveset() {
        super("flame_breathing", "Flame Breathing", createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:flame_idle")
                .withDamageMultiplier(1.1f) // Slight damage boost for slugger style
                .withSpeedMultiplier(0.9f) // Slightly slower but more powerful

                // First Form: Unknowing Fire - Quick dash strike (INDEX 0 in wheel)
                .withMove(new MoveBuilder("unknowing_fire", "Unknowing Fire")
                        .withAnimation("nichirin:unknowing_fire", 9)
                        .withTiming(120, 8, 20) // 6 second cooldown, quick execution
                        .withDamage(18.0f) // High damage single hit
                        .withTeleportDistance(12.0f) // 12 block dash
                        .withRange(3.0f) // Close range after dash
                        .withKnockback(0.4f)
                        .withBreathCost(30.0f)
                        .withHitStun(25)
                        .withHitboxSize(2.0f)
                        .withAction(player -> {
                            UnknowingFireAttack attack = new UnknowingFireAttack();
                            FlameBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(0));
                            }
                            MoveExecutor.executeAttack(player, attack, "flame_breathing", "unknowing_fire");
                        })
                )

                // Second Form: Rising Scorching Sun - Upward arc (INDEX 1 in wheel)
                .withMove(new MoveBuilder("rising_scorching_sun", "Scorching Sun")
                        .withAnimation("nichirin:rising_scorching_sun", 8)
                        .withTiming(100, 12, 25) // 5 second cooldown
                        .withDamage(15.0f) // Good damage + bonus vs airborne
                        .withRange(6.0f) // Upward arc range
                        .withKnockback(0.8f) // Strong upward knockback
                        .withBreathCost(25.0f)
                        .withHitStun(20)
                        .withHitboxSize(2.5f) // Larger for arc
                        .withAction(player -> {
                            RisingScorchingSunAttack attack = new RisingScorchingSunAttack();
                            FlameBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(1));
                            }
                            MoveExecutor.executeAttack(player, attack, "flame_breathing", "rising_scorching_sun");
                        })
                )

                // Third Form: Blazing Universe - Heavy downward strike (INDEX 2 in wheel)
                .withMove(new MoveBuilder("blazing_universe", "Blazing Universe")
                        .withAnimation("nichirin:blazing_universe", 12)
                        .withTiming(160, 40, 50) // 8 second cooldown, 2s windup, explosive finish
                        .withDamage(25.0f) // Very high damage
                        .withRange(8.0f) // Large AOE
                        .withKnockback(0.6f)
                        .withBreathCost(45.0f) // Expensive for heavy attack
                        .withHitStun(35)
                        .withHitboxSize(4.0f) // Large explosion hitbox
                        .withAction(player -> {
                            BlazingUniverseAttack attack = new BlazingUniverseAttack();
                            FlameBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(2));
                            }
                            MoveExecutor.executeAttack(player, attack, "flame_breathing", "blazing_universe");
                        })
                )

                // Fourth Form: Blooming Flame Undulation - 360° defense (INDEX 3 in wheel)
                .withMove(new MoveBuilder("blooming_flame_undulation", "Blooming Flame")
                        .withAnimation("nichirin:blooming_flame_undulation", 10)
                        .withTiming(140, 15, 35) // 7 second cooldown
                        .withDamage(12.0f) // Multiple hits around user
                        .withRange(3.5f) // 3.5 block radius
                        .withKnockback(0.3f)
                        .withBreathCost(35.0f)
                        .withHitStun(15)
                        .withHitboxSize(3.5f) // Full radius
                        .withAction(player -> {
                            BloomingFlameUndulationAttack attack = new BloomingFlameUndulationAttack();
                            FlameBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(3));
                            }
                            MoveExecutor.executeAttack(player, attack, "flame_breathing", "blooming_flame_undulation");
                        })
                )

                // Fifth Form: Flame Tiger - Multi-hit dash (INDEX 4 in wheel)
                .withMove(new MoveBuilder("flame_tiger", "Flame Tiger")
                        .withAnimation("nichirin:flame_tiger", 11)
                        .withTiming(120, 10, 40) // 6 second cooldown, dash duration
                        .withDamage(8.0f) // Lower per hit, but many hits
                        .withDashSpeed(8.0f) // 8 block dash
                        .withRange(8.0f) // Dash distance
                        .withKnockback(0.2f) // Light knockback to keep enemies close
                        .withBreathCost(40.0f)
                        .withHitStun(10) // Short stun for combo potential
                        .withHitboxSize(2.0f)
                        .withAction(player -> {
                            FlameTigerAttack attack = new FlameTigerAttack();
                            FlameBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(4));
                            }
                            MoveExecutor.executeAttack(player, attack, "flame_breathing", "flame_tiger");
                        })
                )

                // Ninth Form: Rengoku - Ultimate dragon technique (INDEX 5 in wheel)
                .withMove(new MoveBuilder("rengoku", "Rengoku")
                        .withAnimation("nichirin:rengoku", 20)
                        .withTiming(600, 80, 60) // 30 second cooldown, 4s windup, dragon dash
                        .withDamage(120.0f) // Massive damage ultimate
                        .withDashSpeed(25.0f) // Very fast dash
                        .withRange(20.0f) // Long range dash
                        .withKnockback(2.5f) // Massive knockback
                        .withBreathCost(80.0f) // Most expensive ultimate
                        .withHitStun(80) // 4 second stun
                        .withHitboxSize(4.0f) // Large dragon hitbox
                        .withAction(player -> {
                            RengokuAttack attack = new RengokuAttack();
                            FlameBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(5));
                            }
                            MoveExecutor.executeAttack(player, attack, "flame_breathing", "rengoku");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 6; // 6 forms in the attack wheel
    }

    @Override
    public boolean handleRightClick(Player player, boolean isCrouching) {
        if (isCrouching) {
            // Crouch + Right-click: Unknowing Fire
            return executeUnknowingFire(player);
        } else {
            // Regular Right-click: Pommel Slash
            return executePommelSlash(player);
        }
    }

    private boolean executePommelSlash(Player player) {
        // Check breath cost for Pommel Slash (halved to compensate for doubling bug)
        float breathCost = 7.5f; // Will become 15 after doubling

        // Use atomic consume - no separate check needed
        if (BreathingManager.consume(player, breathCost)) {
            // Breath was successfully consumed - execute attack
            PommelSlashAttack attack = new PommelSlashAttack();

            // Create temporary config for Pommel Slash
            MoveConfiguration tempConfig = new MoveBuilder("pommel_slash", "Pommel Slash")
                    .withAnimation("nichirin:pommel_slash", 8)
                    .withTiming(0, 5, 18) // No cooldown, quick windup, 6 slashes over 18 ticks
                    .withDamage(6.0f) // 6 slashes, first hit full damage, rest 30%
                    .withRange(4.0f) // Medium range
                    .withKnockback(0.2f) // Light knockback for combos
                    .withBreathCost(breathCost)
                    .withHitStun(8) // Short stun for combo potential
                    .withHitboxSize(2.0f)
                    .build();

            attack.configure(tempConfig);
            MoveExecutor.executeAttack(player, attack, "flame_breathing", "pommel_slash");

            onMovePerformed(player, -1, false); // Use -1 to indicate right-click move
        } else {
            // Breath consumption failed - show error message
            player.displayClientMessage(
                    Component.literal("Not enough breath for Pommel Slash!")
                            .withStyle(style -> style.withColor(0xFF3333)), // Red for no breath
                    true
            );
        }

        return true;
    }

    private boolean executeUnknowingFire(Player player) {
        // Check breath cost for Unknowing Fire (halved to compensate for doubling bug)
        float breathCost = 15.0f; // Will become 30 after doubling (same as wheel version)

        // Use atomic consume - no separate check needed
        if (BreathingManager.consume(player, breathCost)) {
            // Breath was successfully consumed - execute attack
            UnknowingFireAttack attack = new UnknowingFireAttack();

            // Create temporary config for Unknowing Fire (similar to wheel version but faster)
            MoveConfiguration tempConfig = new MoveBuilder("unknowing_fire_quick", "Unknowing Fire")
                    .withAnimation("nichirin:unknowing_fire", 9)
                    .withTiming(0, 6, 15) // No cooldown, quicker windup for right-click version
                    .withDamage(16.0f) // Slightly less than wheel version (18.0f)
                    .withRange(3.0f) // Close range after dash
                    .withKnockback(0.4f)
                    .withBreathCost(breathCost)
                    .withHitStun(20) // Slightly less stun than wheel version
                    .withHitboxSize(2.0f)
                    .build();

            attack.configure(tempConfig);
            MoveExecutor.executeAttack(player, attack, "flame_breathing", "unknowing_fire_quick");

            onMovePerformed(player, -2, true); // Use -2 to indicate crouch right-click move
        } else {
            // Breath consumption failed - show error message
            player.displayClientMessage(
                    Component.literal("Not enough breath for Unknowing Fire!")
                            .withStyle(style -> style.withColor(0xFF3333)), // Red for no breath
                    true
            );
        }

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
                                        .withStyle(style -> style.withColor(0xFF6600)), // Orange color for flame
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

        // Remove the range check for Flame Breathing - it's designed for crowd control
        // and should work even without enemies nearby (unlike Thunder's targeted attacks)

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
     * Check if there are valid targets within range for crowd control moves
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
    public static FlameBreathingMoveset getCurrentMoveset() {
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
        return "Pommel Slash";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Unknowing Fire";
    }

    @Override
    public void onMovePerformed(Player player, int moveIndex, boolean isCrouching) {
        // Flame Breathing specific post-move effects can be added here
        // moveIndex -1 = Pommel Slash (right-click)
        // moveIndex -2 = Unknowing Fire (crouch + right-click)
    }

    /**
     * Called when a player logs out - clean up their data
     */
    public static void cleanupPlayer(Player player) {
        playerCooldowns.remove(player.getUUID());
        executingMove.remove(player.getUUID());
    }
}