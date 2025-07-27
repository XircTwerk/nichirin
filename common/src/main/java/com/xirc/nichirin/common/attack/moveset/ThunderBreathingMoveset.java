package com.xirc.nichirin.common.attack.moveset;

import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.moves.thunder.*;
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
 * Balanced Thunder Breathing moveset implementation
 * Thunder Clap Flash is right-click only, remaining 6 forms for attack wheel
 */
public class ThunderBreathingMoveset extends AbstractMoveset {

    // Track cooldowns per player per move
    private static final Map<UUID, Map<Integer, Long>> playerCooldowns = new HashMap<>();

    // Track active attacks to prevent breath consumption on failed attempts
    private static final Map<UUID, Boolean> executingMove = new HashMap<>();

    // Thread-local to store current moveset instance for action access
    private static final ThreadLocal<ThunderBreathingMoveset> CURRENT_MOVESET = new ThreadLocal<>();

    public ThunderBreathingMoveset() {
        super("thunder_breathing", "Thunder Breathing", createBuilder());
    }

    private static MovesetBuilder createBuilder() {
        return new MovesetBuilder()
                .withIdleAnimation("nichirin:thunder_idle")
                .withSpeedMultiplier(1.2f) // Slight speed boost for Thunder Breathing

                // SKIP INDEX 0 - Thunder Clap Flash is right-click only, not in attack wheel

                // Second Form: Rice Spirit - 5 quick slashes (INDEX 0 in wheel)
                .withMove(new MoveBuilder("rice_spirit", "Rice Spirit")
                        .withIcon("nichirin:textures/gui/moves/thunder_second_form.png")
                        .withAnimation("nichirin:rice_spirit", 8)
                        .withTiming(100, 8, 25) // 5 second cooldown, quick windup, duration
                        .withDamage(4.5f) // 5 slashes = 22.5 total damage (was 6.0f = 30 total)
                        .withRange(5.0f) // Medium range
                        .withKnockback(0.2f)
                        .withBreathCost(25.0f)
                        .withHitStun(15)
                        .withHitboxSize(1.8f)
                        .withAction(player -> {
                            RiceSpiritAttack attack = new RiceSpiritAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(0));
                            }
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "rice_spirit");
                        })
                )

                // Third Form: Thunder Swarm - AOE slashes (INDEX 1 in wheel)
                .withMove(new MoveBuilder("thunder_swarm", "Thunder Swarm")
                        .withIcon("nichirin:textures/gui/moves/thunder_third_form.png")
                        .withAnimation("nichirin:thunder_swarm", 9)
                        .withTiming(140, 12, 35) // 7 second cooldown, windup, duration
                        .withDamage(6.0f) // 4 slashes = 24 damage total (was 8.0f = 32 total)
                        .withRange(7.0f) // Large area around player
                        .withKnockback(0.4f)
                        .withBreathCost(35.0f) // Higher cost for AOE
                        .withHitStun(20)
                        .withHitboxSize(2.5f) // Large hitbox for AOE
                        .withAction(player -> {
                            ThunderSwarmAttack attack = new ThunderSwarmAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(1));
                            }
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "thunder_swarm");
                        })
                )

                // Fourth Form: Distant Thunder - Lightning over time (INDEX 2 in wheel)
                .withMove(new MoveBuilder("distant_thunder", "Distant Thunder")
                        .withIcon("nichirin:textures/gui/moves/thunder_fourth_form.png")
                        .withAnimation("nichirin:distant_thunder", 7)
                        .withTiming(200, 20, 120) // 10 second cooldown, long windup, 6 second duration
                        .withDamage(9.0f) // 3 strikes = 27 damage over time (was 12.0f = 36 total)
                        .withRange(15.0f) // Large AOE radius
                        .withKnockback(0.3f)
                        .withBreathCost(45.0f) // High cost for area denial
                        .withHitStun(25)
                        .withAction(player -> {
                            DistantThunderAttack attack = new DistantThunderAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(2));
                            }
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "distant_thunder");
                        })
                )

                // Fifth Form: Heat Lightning - Anti-air combo (INDEX 3 in wheel)
                .withMove(new MoveBuilder("heat_lightning", "Heat Lightning")
                        .withIcon("nichirin:textures/gui/moves/thunder_fifth_form.png")
                        .withAnimation("nichirin:heat_lightning", 9)
                        .withTiming(160, 10, 20) // 8 second cooldown, windup, duration
                        .withDamage(13.5f) // Single hit + lightning follow-up (was 18.0f)
                        .withRange(8.0f)
                        .withKnockback(0.1f) // Minimal horizontal, focuses on launch
                        .withBreathCost(30.0f)
                        .withHitStun(30) // Good combo potential
                        .withHitboxSize(2.0f)
                        .withAction(player -> {
                            HeatLightningAttack attack = new HeatLightningAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(3));
                            }
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "heat_lightning");
                        })
                )

                // Sixth Form: Rumble and Flash - Long range precision (INDEX 4 in wheel)
                .withMove(new MoveBuilder("rumble_flash", "Rumble and Flash")
                        .withIcon("nichirin:textures/gui/moves/thunder_sixth_form.png")
                        .withAnimation("nichirin:rumble_flash", 8)
                        .withTiming(180, 15, 25) // 9 second cooldown, aim time, duration
                        .withDamage(16.5f) // High damage for long range precision (was 22.0f)
                        .withRange(20.0f) // Very long range
                        .withKnockback(0.6f)
                        .withBreathCost(40.0f) // High cost for range and damage
                        .withHitStun(35) // Good stun for follow-up
                        .withAction(player -> {
                            RumbleFlashAttack attack = new RumbleFlashAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(4));
                            }
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "rumble_flash");
                        })
                )

                // Seventh Form: Honoikazuchi no Kami - Ultimate finisher (INDEX 5 in wheel)
                .withMove(new MoveBuilder("honoikazuchi_no_kami", "Honoikazuchi no Kami")
                        .withIcon("nichirin:textures/gui/moves/thunder_seventh_form.png")
                        .withAnimation("nichirin:honoikazuchi_no_kami", 15)
                        .withTiming(600, 60, 40) // 30 second cooldown, long windup, execution
                        .withDamage(60.0f) // Very high damage ultimate (was 80.0f)
                        .withTeleportDistance(20.0f) // Long dash
                        .withKnockback(2.0f) // High knockback
                        .withBreathCost(70.0f) // Very expensive ultimate
                        .withHitStun(60) // 3 second stun
                        .withHitboxSize(3.0f) // Large hitbox for ultimate
                        .withAction(player -> {
                            HonoikazuchiNoKamiAttack attack = new HonoikazuchiNoKamiAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(5));
                            }
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "honoikazuchi_no_kami");
                        })
                );
    }

    @Override
    public int getMoveCount() {
        return 6; // Only 6 moves in attack wheel (excluding Thunder Clap Flash)
    }

    @Override
    public boolean handleRightClick(Player player, boolean isCrouching) {
        // Thunder Clap Flash - right-click exclusive move
        ThunderClapFlashAttack.setCrouchDash(player, isCrouching);

        // Check breath cost for Thunder Clap Flash
        float breathCost = 7.5f; // for some reason it's double the amount. 7.5 would be 15 breath cost.
        if (!BreathingManager.hasBreath(player, breathCost)) {
            player.displayClientMessage(
                    Component.literal("Not enough breath for Thunderclap and Flash!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            return true; // Still handled, just blocked
        }

        // Consume breath for Thunder Clap Flash
        if (BreathingManager.consume(player, breathCost)) {
            // Execute Thunder Clap Flash attack directly
            ThunderClapFlashAttack attack = new ThunderClapFlashAttack();

            // Create temporary config for Thunder Clap Flash
            MoveConfiguration tempConfig = new MoveBuilder("thunderclap_flash", "Thunderclap and Flash")
                    .withAnimation("nichirin:thunderclap_flash", 10)
                    .withTiming(0, 1, 15) // No cooldown
                    .withDamage(10.5f) // Moderate damage for mobility move (was 14.0f)
                    .withTeleportDistance(12.0f) // Good mobility
                    .withKnockback(0.2f)
                    .withBreathCost(breathCost)
                    .withHitStun(10)
                    .withHitboxSize(1.5f)
                    .build();

            attack.configure(tempConfig);
            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "thunderclap_flash");

            onMovePerformed(player, -1, isCrouching); // Use -1 to indicate right-click move
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
                                        .withStyle(style -> style.withColor(0xFFFF00)),
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
            // Get the breath cost from the move configuration
            float breathCost = config.getBreathCostOrDefault(0.0f);

            if (breathCost > 0 && !BreathingManager.hasBreath(player, breathCost)) {
                player.displayClientMessage(
                        Component.literal("Not enough breath for " + config.getDisplayName() + "!")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true
                );
                return;
            }
        }

        // SPECIAL CHECK FOR RICE SPIRIT - Don't execute if no targets in range
        if (moveIndex == 0) { // Rice Spirit is index 0 in the wheel
            if (!hasTargetsInRange(player, config.getRangeOrDefault(5.0f))) {
                player.displayClientMessage(
                        Component.literal("Rice Spirit: No enemies in range!")
                                .withStyle(style -> style.withColor(0xFFAA00)),
                        true
                );
                return; // Don't execute at all - no breath consumed, no cooldown
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
     * Check if there are valid targets within range for Rice Spirit
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
    public static ThunderBreathingMoveset getCurrentMoveset() {
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
        return -1; // Not in attack wheel, handled separately
    }

    @Override
    public String getRightClickMoveName() {
        return "Thunderclap and Flash";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Thunderclap and Flash (Backwards)";
    }

    @Override
    public void onMovePerformed(Player player, int moveIndex, boolean isCrouching) {
        // Special handling for Thunder Clap and Flash when used while initially crouching
        if (moveIndex == -1 && isCrouching) {
            // The crouch state is already stored in ThunderClapFlashAttack via setCrouchDash
        }
    }

    /**
     * Called when a player logs out - clean up their data
     */
    public static void cleanupPlayer(Player player) {
        playerCooldowns.remove(player.getUUID());
        executingMove.remove(player.getUUID());
    }
}