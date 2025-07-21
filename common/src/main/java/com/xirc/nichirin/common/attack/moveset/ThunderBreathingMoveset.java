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
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Thunder Breathing moveset implementation
 * All 7 forms of Thunder Breathing with full configuration in builder
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
                .withSpeedMultiplier(1.3f) // Thunder Breathing emphasizes speed

                // First Form: Thunderclap and Flash - Instant teleport dash (NO COOLDOWN)
                .withMove(new MoveBuilder("thunderclap_flash", "Thunderclap and Flash")
                        .withIcon("nichirin:textures/gui/moves/thunder_first_form.png")
                        .withAnimation("nichirin:thunderclap_flash", 10)
                        .withTiming(0, 1, 15) // NO COOLDOWN, windup, duration
                        .withDamage(12.0f) // Moderate damage - spammable
                        .withTeleportDistance(15.0f) // 15 block teleport dash
                        .withKnockback(0f)
                        .withBreathCost(15.0f) // Lower breath cost since no cooldown
                        .withHitStun(15) // Short stun since spammable
                        .withHitboxSize(2.0f)
                        .withAction(player -> {
                            ThunderClapFlashAttack attack = new ThunderClapFlashAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(0));
                            }
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "thunderclap_flash");
                        })
                )

                // Second Form: Rice Spirit - 5 slashes on locked target
                .withMove(new MoveBuilder("rice_spirit", "Rice Spirit")
                        .withIcon("nichirin:textures/gui/moves/thunder_second_form.png")
                        .withAnimation("nichirin:rice_spirit", 8)
                        .withTiming(60, 5, 30) // 3 second cooldown, windup, duration
                        .withDamage(8.0f) // Lower per slash but 5 total = 40 damage
                        .withRange(6.0f) // Range to find enemies
                        .withKnockback(0.3f)
                        .withBreathCost(20.0f)
                        .withHitStun(20)
                        .withHitboxSize(2.0f)
                        .withAction(player -> {
                            RiceSpiritAttack attack = new RiceSpiritAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(1));
                            }
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "rice_spirit");
                        })
                )

                // Third Form: Thunder Swarm - 6 large slashes in wide area
                .withMove(new MoveBuilder("thunder_swarm", "Thunder Swarm")
                        .withIcon("nichirin:textures/gui/moves/thunder_third_form.png")
                        .withAnimation("nichirin:thunder_swarm", 9)
                        .withTiming(80, 10, 40) // 4 second cooldown, windup, duration
                        .withDamage(7.0f) // 6 slashes = 42 damage total
                        .withRange(8.0f) // Large area around player
                        .withKnockback(0.3f)
                        .withBreathCost(25.0f)
                        .withHitStun(25)
                        .withHitboxSize(3.0f) // Large hitbox for AOE
                        .withAction(player -> {
                            ThunderSwarmAttack attack = new ThunderSwarmAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(2));
                            }
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "thunder_swarm");
                        })
                )

                // Fourth Form: Distant Thunder - Lightning strikes over 8 seconds
                .withMove(new MoveBuilder("distant_thunder", "Distant Thunder")
                        .withIcon("nichirin:textures/gui/moves/thunder_fourth_form.png")
                        .withAnimation("nichirin:distant_thunder", 7)
                        .withTiming(120, 15, 160) // 6 second cooldown, windup, 8 seconds duration
                        .withDamage(15.0f) // 4 strikes = 60 damage over time
                        .withRange(20.0f) // Very large AOE radius
                        .withKnockback(0.4f)
                        .withBreathCost(35.0f)
                        .withHitStun(30) // Longer stun for lightning
                        .withAction(player -> {
                            DistantThunderAttack attack = new DistantThunderAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(3));
                            }
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "distant_thunder");
                        })
                )

                // Fifth Form: Heat Lightning - Upward slash with lightning on airborne targets
                .withMove(new MoveBuilder("heat_lightning", "Heat Lightning")
                        .withIcon("nichirin:textures/gui/moves/thunder_fifth_form.png")
                        .withAnimation("nichirin:heat_lightning", 9)
                        .withTiming(100, 8, 20) // 5 second cooldown, windup, duration
                        .withDamage(25.0f) // High damage for armor-piercing + lightning combo
                        .withRange(12.0f)
                        .withKnockback(0.0f) // No horizontal knockback, just launch
                        .withBreathCost(30.0f)
                        .withHitStun(40) // Long stun for combo potential
                        .withHitboxSize(2.5f)
                        .withAction(player -> {
                            HeatLightningAttack attack = new HeatLightningAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(4));
                            }
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "heat_lightning");
                        })
                )

                // Sixth Form: Rumble and Flash - Lightning barrage at long range
                .withMove(new MoveBuilder("rumble_flash", "Rumble and Flash")
                        .withIcon("nichirin:textures/gui/moves/thunder_sixth_form.png")
                        .withAnimation("nichirin:rumble_flash", 8)
                        .withTiming(140, 10, 30) // 7 second cooldown, windup, duration
                        .withDamage(18.0f) // Multiple strikes possible
                        .withRange(25.0f) // Very long range
                        .withKnockback(0.6f)
                        .withBreathCost(40.0f)
                        .withHitStun(50) // 2.5 second stun
                        .withAction(player -> {
                            RumbleFlashAttack attack = new RumbleFlashAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(5));
                            }
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "rumble_flash");
                        })
                )

                // Seventh Form: Honoikazuchi no Kami - Ultimate technique (INSTAKILL)
                .withMove(new MoveBuilder("honoikazuchi_no_kami", "Honoikazuchi no Kami")
                        .withIcon("nichirin:textures/gui/moves/thunder_seventh_form.png")
                        .withAnimation("nichirin:honoikazuchi_no_kami", 15)
                        .withTiming(600, 40, 60) // 30 second cooldown, long windup, duration
                        .withDamage(9999.0f) // INSTAKILL damage
                        .withTeleportDistance(30.0f) // Very long dash
                        .withKnockback(3.0f) // Massive knockback
                        .withBreathCost(60.0f) // Expensive
                        .withHitStun(100) // 5 second stun
                        .withHitboxSize(5.0f) // Huge hitbox for ultimate
                        .withAction(player -> {
                            HonoikazuchiNoKamiAttack attack = new HonoikazuchiNoKamiAttack();
                            ThunderBreathingMoveset moveset = getCurrentMoveset();
                            if (moveset != null) {
                                attack.configure(moveset.getMove(6));
                            }
                            MoveExecutor.executeAttack(player, attack, "thunder_breathing", "honoikazuchi_no_kami");
                        })
                );
    }

    @Override
    public boolean handleRightClick(Player player, boolean isCrouching) {
        // Always Thunder Clap and Flash - just store the crouch state
        ThunderClapFlashAttack.setCrouchDash(player, isCrouching);

        // Execute Thunder Clap and Flash (index 0)
        performMove(player, 0);
        onMovePerformed(player, 0, isCrouching);

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
            // Skip cooldown display for Thunder Clap Flash (index 0)
            if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer
                    && config.getCooldownOrDefault(0) > 0 && moveIndex != 0) {
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
        return 0; // Always Thunder Clap and Flash
    }

    @Override
    public String getRightClickMoveName() {
        return "Thunder Clap and Flash";
    }

    @Override
    public String getCrouchRightClickMoveName() {
        return "Thunder Clap and Flash"; // Same move, just with backwards turn
    }

    @Override
    public void onMovePerformed(Player player, int moveIndex, boolean isCrouching) {
        // Special handling for Thunder Clap and Flash when used while initially crouching
        if (moveIndex == 0 && isCrouching) {
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