package com.xirc.nichirin.common.system.blocking;

import com.xirc.nichirin.common.effect.BlockingStatusEffect;
import com.xirc.nichirin.common.system.StanceManager;
import com.xirc.nichirin.common.util.KatanaInputHandler;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Complete katana blocking and parrying system using STANCE instead of stamina
 */
public class KatanaBlock {

    // Player blocking states
    private static final Map<UUID, BlockingState> BLOCKING_STATES = new HashMap<>();

    // Configuration constants - using STANCE not stamina
    private static final float BLOCK_STANCE_DRAIN = 0.8f; // Per tick while blocking (higher than stamina drain)
    private static final float PARRY_STANCE_COST = 15.0f; // One-time cost for successful parry
    private static final int PARRY_WINDOW_TICKS = 10; // 0.5 seconds at 20 TPS (reduced from 20)
    private static final int PARRY_COOLDOWN_TICKS = 60; // 3 seconds (increased from 40)
    private static final float BACKSTAB_ANGLE = 90.0f; // Degrees for backstab detection

    /**
     * Blocking stance enum
     */
    public enum BlockingStance {
        NONE,
        BLOCKING,
        PARRY_READY,
        PARRY_SUCCESS,
        PARRY_FAILED
    }

    /**
     * Player blocking state data
     */
    private static class BlockingState {
        BlockingStance stance = BlockingStance.NONE;
        int blockTicks = 0;
        int parryWindowTicks = 0;
        long parryCooldownUntil = 0;
        boolean wasBlockingLastTick = false;

        void reset() {
            stance = BlockingStance.NONE;
            blockTicks = 0;
            parryWindowTicks = 0;
            // Don't reset cooldown on block end - it persists
        }
    }

    /**
     * Starts blocking for a player
     */
    public static boolean startBlocking(Player player) {
        if (player.level().isClientSide) return false;

        BlockingState state = getOrCreateState(player);

        // Check if player can block (including parry cooldown)
        if (!canStartBlocking(player, state)) {
            // Check if it's specifically a parry cooldown issue
            if (isOnParryCooldown(player, state)) {
                int remainingTicks = getRemainingParryCooldown(player, state);
                player.displayClientMessage(
                        Component.literal("Parry on cooldown! (" + (remainingTicks / 20.0f) + "s)")
                                .withStyle(style -> style.withColor(0xFFAA00)),
                        true
                );
            }
            return false;
        }

        // Start blocking with automatic parry window
        state.stance = BlockingStance.PARRY_READY; // Start with parry window
        state.blockTicks = 0;
        state.parryWindowTicks = PARRY_WINDOW_TICKS; // 0.5 second parry window (10 ticks)

        // Apply blocking effect
        applyBlockingEffect(player);

        // Block katana inputs
        KatanaInputHandler.blockAfterBreathingMove(player);

        // Send message to player about parry window
        player.displayClientMessage(
                Component.literal("Blocking - Perfect parry window active! (0.5s)")
                        .withStyle(style -> style.withColor(0x55FF55)),
                true // Overlay message
        );

        // Play blocking sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.2f);

        return true;
    }

    /**
     * Stops blocking for a player
     */
    public static void stopBlocking(Player player) {
        if (player.level().isClientSide) return;

        BlockingState state = BLOCKING_STATES.get(player.getUUID());
        if (state == null || state.stance == BlockingStance.NONE) return;

        // Remove blocking effect
        removeBlockingEffect(player);

        // Reset state
        state.reset();
    }

    /**
     * Attempts to parry for a player
     */
    public static boolean attemptParry(Player player) {
        // Remove this method - parrying is now automatic at start of blocking
        return false;
    }

    /**
     * Handles incoming damage for blocking/parrying
     */
    public static boolean handleIncomingDamage(Player player, Player attacker, float damage) {
        if (player.level().isClientSide) return false;

        BlockingState state = BLOCKING_STATES.get(player.getUUID());
        if (state == null || state.stance == BlockingStance.NONE) return false;

        System.out.println("DEBUG: handleIncomingDamage - stance: " + state.stance + ", parryWindowTicks: " + state.parryWindowTicks);

        // Check for backstab ONLY if attacker is a player
        if (attacker != null && isBackstab(player, attacker)) {
            stopBlocking(player);
            return false; // Block negated by backstab
        }

        // Handle parry window - works against ALL damage sources
        if (state.stance == BlockingStance.PARRY_READY && state.parryWindowTicks > 0) {
            System.out.println("DEBUG: PARRY DETECTED - parryWindowTicks remaining: " + state.parryWindowTicks);
            return handleSuccessfulParry(player, attacker, state);
        }

        // Handle regular blocking - works against ALL damage sources
        if (state.stance == BlockingStance.BLOCKING) {
            System.out.println("DEBUG: REGULAR BLOCK DETECTED");
            return handleSuccessfulBlock(player, state, damage);
        }

        return false;
    }

    /**
     * Ticks the blocking system for a player
     */
    public static void tick(Player player) {
        if (player.level().isClientSide) return;

        BlockingState state = BLOCKING_STATES.get(player.getUUID());
        if (state == null) return;

        boolean isBlocking = (state.stance == BlockingStance.BLOCKING ||
                state.stance == BlockingStance.PARRY_READY);

        if (isBlocking) {
            state.blockTicks++;

            // DON'T drain stance while blocking - only lose stance when hit
            // No more continuous stance consumption

            // Handle parry window countdown
            if (state.stance == BlockingStance.PARRY_READY) {
                state.parryWindowTicks--;
                if (state.parryWindowTicks <= 0) {
                    // Parry window expired - transition to regular blocking
                    state.stance = BlockingStance.BLOCKING;

                    // Send message to player that parry window ended
                    player.displayClientMessage(
                            Component.literal("Parry window ended - now blocking")
                                    .withStyle(style -> style.withColor(0xFFAA00)),
                            true // Overlay message
                    );
                }
            }

            // Apply blocking effect if not already applied
            if (!player.hasEffect(NichirinEffectRegistry.BLOCKING.get())) {
                applyBlockingEffect(player);
            }
        } else {
            // Clean up if no longer blocking
            if (state.wasBlockingLastTick) {
                removeBlockingEffect(player);
            }
        }

        state.wasBlockingLastTick = isBlocking;
    }

    /**
     * Gets the current blocking stance for a player
     */
    public static BlockingStance getStance(Player player) {
        BlockingState state = BLOCKING_STATES.get(player.getUUID());
        return state != null ? state.stance : BlockingStance.NONE;
    }

    /**
     * Checks if player is currently blocking
     */
    public static boolean isBlocking(Player player) {
        BlockingStance stance = getStance(player);
        return stance == BlockingStance.BLOCKING || stance == BlockingStance.PARRY_READY;
    }

    /**
     * Cleanup when player disconnects
     */
    public static void cleanupPlayer(Player player) {
        BLOCKING_STATES.remove(player.getUUID());
    }

    // Private helper methods

    private static BlockingState getOrCreateState(Player player) {
        return BLOCKING_STATES.computeIfAbsent(player.getUUID(), k -> new BlockingState());
    }

    private static boolean canStartBlocking(Player player, BlockingState state) {
        // Can't block if already blocking
        if (state.stance != BlockingStance.NONE) return false;

        // Check parry cooldown
        if (isOnParryCooldown(player, state)) return false;

        // Must have minimum STANCE (not stamina)
        if (!StanceManager.hasStance(player, BLOCK_STANCE_DRAIN * 25)) return false; // ~2 seconds worth

        return true;
    }

    /**
     * Check if player is on parry cooldown
     */
    private static boolean isOnParryCooldown(Player player, BlockingState state) {
        long currentTime = player.level().getGameTime();
        return currentTime < state.parryCooldownUntil;
    }

    /**
     * Get remaining parry cooldown ticks
     */
    private static int getRemainingParryCooldown(Player player, BlockingState state) {
        long currentTime = player.level().getGameTime();
        return Math.max(0, (int)(state.parryCooldownUntil - currentTime));
    }

    /**
     * Set parry cooldown
     */
    private static void setParryCooldown(Player player, BlockingState state) {
        long currentTime = player.level().getGameTime();
        state.parryCooldownUntil = currentTime + PARRY_COOLDOWN_TICKS;

        // Display cooldown on HUD like movement system
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            com.xirc.nichirin.common.network.CooldownDisplayPacket.sendToClient(
                    serverPlayer, "Parry", PARRY_COOLDOWN_TICKS);
        }
    }

    private static boolean canParry(Player player, BlockingState state) {
        // Check cooldown
        long currentTime = player.level().getGameTime();
        if (currentTime < state.parryCooldownUntil) return false;

        // Can't parry if already in parry state
        if (state.stance == BlockingStance.PARRY_READY ||
                state.stance == BlockingStance.PARRY_SUCCESS) return false;

        return true;
    }

    private static boolean isBackstab(Player defender, Player attacker) {
        // Calculate angle between defender's facing direction and attacker's position
        Vec3 defenderLook = defender.getLookAngle();
        Vec3 toAttacker = attacker.position().subtract(defender.position()).normalize();

        double dot = defenderLook.dot(toAttacker);
        double angle = Math.toDegrees(Math.acos(Math.abs(dot)));

        // Backstab if attacker is more than 90 degrees behind defender
        return angle > BACKSTAB_ANGLE;
    }

    private static boolean handleSuccessfulParry(Player player, Player attacker, BlockingState state) {
        // Set parry success state
        state.stance = BlockingStance.PARRY_SUCCESS;
        state.parryWindowTicks = 0;

        // Set parry cooldown (3 seconds)
        setParryCooldown(player, state);

        // Show successful parry message
        player.displayClientMessage(
                Component.literal("Successful Parry!")
                        .withStyle(style -> style.withColor(0x00FF00).withBold(true)),
                true // Overlay message
        );

        // Play parry success sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 2.0f);

        // Stun ANY living entity that attacked (players AND mobs)
        if (attacker instanceof ServerPlayer serverPlayer) {
            // Apply 1-second stun to player (reduced from 1.5 seconds)
            MobEffectInstance stunEffect = new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    20, // 1 second (20 ticks, reduced from 30)
                    0, // Amplifier
                    false, // Ambient
                    false, // Show particles - DISABLED
                    true   // Show icon
            );
            serverPlayer.addEffect(stunEffect);
        }
        // Note: We need the actual LivingEntity from damage source for mobs
        // This method only gets Player attacker, so we'll handle mob stunning in the event handler

        // Stop blocking after successful parry (brief window) - DON'T DO THIS IMMEDIATELY
        // The stance should stay PARRY_SUCCESS until the event handler processes it
        // Delay stopping to allow damage negation to process
        // Remove the immediate stopping - let the tick system handle it naturally

        return true; // Damage completely negated
    }

    private static boolean handleSuccessfulBlock(Player player, BlockingState state, float damage) {
        // Take 10 stance damage when hit while blocking
        if (!StanceManager.consume(player, 10.0f)) {
            // Out of stance - stance broken! Apply stun to the blocker
            MobEffectInstance stunEffect = new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    60, // 3 seconds (60 ticks)
                    0, // Amplifier
                    false, // Ambient
                    true, // Show particles
                    true   // Show icon
            );
            player.addEffect(stunEffect);

            // Stop blocking
            stopBlocking(player);

            // Send message to player
            player.displayClientMessage(
                    Component.literal("Stance broken! You are stunned!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true // Overlay message
            );

            return false; // Stance broken, take full damage
        }

        // Play block sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.8f, 1.0f);

        return true; // Damage reduced by blocking effect (80% resistance)
    }

    private static void applyBlockingEffect(Player player) {
        // Apply blocking status effect (40% slowdown + prevents sprinting)
        MobEffectInstance blockingEffect = new MobEffectInstance(
                NichirinEffectRegistry.BLOCKING.get(),
                Integer.MAX_VALUE, // Permanent while blocking
                0, // Amplifier
                false, // Ambient
                false, // Show particles
                true   // Show icon
        );
        player.addEffect(blockingEffect);

        // Apply Resistance IV (80% damage reduction)
        MobEffectInstance resistanceEffect = new MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,
                Integer.MAX_VALUE, // Permanent while blocking
                3, // Amplifier 3 = Resistance IV (80% damage reduction)
                false, // Ambient
                false, // Show particles
                true   // Show icon
        );
        player.addEffect(resistanceEffect);
    }

    private static void removeBlockingEffect(Player player) {
        player.removeEffect(NichirinEffectRegistry.BLOCKING.get());
        player.removeEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE);
    }

    /**
     * Save blocking data to NBT
     */
    public static void save(Player player, CompoundTag tag) {
        BlockingState state = BLOCKING_STATES.get(player.getUUID());
        if (state != null) {
            CompoundTag blockingTag = new CompoundTag();
            blockingTag.putString("stance", state.stance.name());
            blockingTag.putInt("blockTicks", state.blockTicks);
            blockingTag.putInt("parryWindowTicks", state.parryWindowTicks);
            blockingTag.putLong("parryCooldownUntil", state.parryCooldownUntil);
            tag.put("BlockingData", blockingTag);
        }
    }

    /**
     * Load blocking data from NBT
     */
    public static void load(Player player, CompoundTag tag) {
        if (tag.contains("BlockingData")) {
            CompoundTag blockingTag = tag.getCompound("BlockingData");
            BlockingState state = getOrCreateState(player);

            try {
                state.stance = BlockingStance.valueOf(blockingTag.getString("stance"));
                state.blockTicks = blockingTag.getInt("blockTicks");
                state.parryWindowTicks = blockingTag.getInt("parryWindowTicks");
                state.parryCooldownUntil = blockingTag.getLong("parryCooldownUntil");

                // Reapply blocking effect if needed
                if (state.stance == BlockingStance.BLOCKING || state.stance == BlockingStance.PARRY_READY) {
                    applyBlockingEffect(player);
                }
            } catch (IllegalArgumentException e) {
                // Invalid stance name, reset to none
                state.reset();
            }
        }
    }
}