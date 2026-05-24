package com.xirc.nichirin.common.system.blocking;

import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.system.StanceManager;
import com.xirc.nichirin.common.util.InputHandler;
import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NicirinSoundRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
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
 * Katana blocking and parrying system.
 * Uses stance as the blocking resource, not stamina.
 */
public class KatanaBlock {

    private static final Map<UUID, BlockingState> BLOCKING_STATES = new HashMap<>();

    private static final float BLOCK_STANCE_DRAIN = 0.8f;
    private static final float PARRY_STANCE_COST = 15.0f;
    private static final int PARRY_WINDOW_TICKS = 10;
    private static final int BLOCK_COOLDOWN_TICKS = 15;
    private static final int EARLY_RELEASE_STUN_TICKS = 4;
    private static final float BACKSTAB_ANGLE = 90.0f;
    private static final int PARRIED_ATTACK_COOLDOWN = 100;

    private static final ResourceLocation COOLDOWN_PACKET_ID = new ResourceLocation("nichirin", "cooldown_display");

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
        long blockCooldownUntil = 0; // New: cooldown for all blocking attempts
        boolean wasBlockingLastTick = false;

        void reset() {
            stance = BlockingStance.NONE;
            blockTicks = 0;
            parryWindowTicks = 0;
        }

        void setBlockCooldown(long currentTime) {
            blockCooldownUntil = currentTime + BLOCK_COOLDOWN_TICKS;
        }

        boolean isOnCooldown(long currentTime) {
            return currentTime < blockCooldownUntil;
        }
    }

    /**
     * Starts blocking for a player
     */
    public static boolean startBlocking(Player player) {
        if (player.level().isClientSide) return false;

        BlockingState state = getOrCreateState(player);

        // Check if player can block
        if (!canStartBlocking(player, state)) {
            return false;
        }

        // Start blocking - open parry window only when parry system is enabled
        state.blockTicks = 0;
        if (NichirinModConfig.get().combat.enableParrySystem) {
            state.stance = BlockingStance.PARRY_READY;
            state.parryWindowTicks = NichirinModConfig.get().combat.parryWindowTicks;
            player.displayClientMessage(
                    Component.literal("⚔ Guard Up!")
                            .withStyle(style -> style.withColor(0xFFAA00).withBold(true)),
                    true
            );
        } else {
            state.stance = BlockingStance.BLOCKING;
            state.parryWindowTicks = 0;
            player.displayClientMessage(
                    Component.literal("⚔ Blocking")
                            .withStyle(style -> style.withColor(0xAAAAAA)),
                    true
            );
        }

        // Apply blocking effect
        applyBlockingEffect(player);

        // Block katana inputs
        InputHandler.blockAfterBreathingMove(player);

        // Play blocking sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.2f);

        // Trigger block animation
        if (player instanceof ServerPlayer serverPlayer) {
            NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer,
                    new PlayerAnimationPacket(serverPlayer.getId(), "sword.block"));
        }

        return true;
    }

    /**
     * Stops blocking for a player
     */
    public static void stopBlocking(Player player) {
        if (player.level().isClientSide) return;

        BlockingState state = BLOCKING_STATES.get(player.getUUID());
        if (state == null || state.stance == BlockingStance.NONE) return;

        long currentTime = player.level().getGameTime();
        boolean wasInParryWindow = (state.stance == BlockingStance.PARRY_READY && state.parryWindowTicks > 0);

        // Check for early release punishment
        if (wasInParryWindow) {
            // Player released during parry window without getting hit - punish them
            applyEarlyReleasePunishment(player);

            // Still set cooldown for early release
            state.setBlockCooldown(currentTime);
        } else if (state.stance != BlockingStance.PARRY_SUCCESS) {
            // Normal block end - set cooldown (unless it was a successful parry)
            state.setBlockCooldown(currentTime);
        }
        // Note: Successful parries don't get cooldown (already handled in handleSuccessfulParry)

        // Remove blocking effect
        removeBlockingEffect(player);

        // Stop the block animation on all nearby clients
        if (player instanceof ServerPlayer serverPlayer) {
            NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer,
                    new PlayerAnimationPacket(serverPlayer.getId(), ""));
        }

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

        // Check for backstab ONLY if attacker is a player
        if (attacker != null && isBackstab(player, attacker)) {
            stopBlocking(player);
            return false; // Block negated by backstab
        }

        // Handle parry window - works against ALL damage sources
        if (state.stance == BlockingStance.PARRY_READY && state.parryWindowTicks > 0
                && NichirinModConfig.get().combat.enableParrySystem) {
            return handleSuccessfulParry(player, attacker, state);
        }

        // Handle regular blocking - works against ALL damage sources
        if (state.stance == BlockingStance.BLOCKING) {
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

            // Handle parry window countdown
            if (state.stance == BlockingStance.PARRY_READY) {
                state.parryWindowTicks--;
                if (state.parryWindowTicks <= 0) {
                    // Parry window expired - transition to regular blocking
                    state.stance = BlockingStance.BLOCKING;
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

        // Can't block if stunned
        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            player.displayClientMessage(
                    Component.literal("Cannot block while stunned!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            return false;
        }

        // Check block cooldown (0.75 seconds after last block ended)
        long currentTime = player.level().getGameTime();
        if (state.isOnCooldown(currentTime)) {
            int remainingTicks = (int)(state.blockCooldownUntil - currentTime);
            player.displayClientMessage(
                    Component.literal("Block on cooldown (" + (remainingTicks / 20.0f) + "s)")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            return false;
        }

        // Must have minimum STANCE
        if (!StanceManager.hasStance(player, BLOCK_STANCE_DRAIN * 25)) {
            player.displayClientMessage(
                    Component.literal("Not enough stance to block!")
                            .withStyle(style -> style.withColor(0xFF5555)),
                    true
            );
            return false;
        }

        return true;
    }

    /**
     * Apply punishment for releasing block key during parry window
     */
    private static void applyEarlyReleasePunishment(Player player) {
        // Apply short stun (0.2 seconds)
        MobEffectInstance stunEffect = new MobEffectInstance(
                NichirinEffectRegistry.STUNNED.get(),
                EARLY_RELEASE_STUN_TICKS, // 0.2 seconds (4 ticks)
                0, // Amplifier
                false, // Ambient
                false, // Show particles
                true   // Show icon
        );
        player.addEffect(stunEffect);

        // Send message to player
        player.displayClientMessage(
                Component.literal("✗ Early release!")
                        .withStyle(style -> style.withColor(0xFF5555)),
                true // Overlay message
        );

        // Play failure sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.5f, 1.5f);

        // Trigger early release / flinch animation
        if (player instanceof ServerPlayer serverPlayer) {
            NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer,
                    new PlayerAnimationPacket(serverPlayer.getId(), "sword.early_release"));
        }
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

        // NO COOLDOWN for successful parry - can immediately block again!
        // This bypasses the normal 0.75 second cooldown

        // Show successful parry message
        player.displayClientMessage(
                Component.literal("✦ Perfect Parry!")
                        .withStyle(style -> style.withColor(0x00FF00).withBold(true)),
                true // Overlay message
        );

        // Remove stun from the defender if present
        if (player.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            player.removeEffect(NichirinEffectRegistry.STUNNED.get());
        }

        // Trigger parry animation on defender
        if (player instanceof ServerPlayer serverPlayer) {
            NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer,
                    new PlayerAnimationPacket(serverPlayer.getId(), "sword.parry"));
        }

        // Interrupt the attacker's moves but don't stun them
        if (attacker instanceof ServerPlayer serverPlayer) {
            interruptAttackerMoves(serverPlayer);
        }

        return true; // Damage completely negated
    }

    /**
     * Interrupt the attacker's moves when they get parried, and stun them for 1 second.
     */
    private static void interruptAttackerMoves(ServerPlayer attacker) {
        try {
            MoveExecutor.clearAttacks(attacker);
            AbstractBreathingAttack.clearSelfTickingAttacks(attacker);
            sendParriedCooldown(attacker, "Move (Parried)", PARRIED_ATTACK_COOLDOWN);

            // Stun the attacker for 1 second (20 ticks); amplifier 1 triggers movement restriction
            attacker.addEffect(new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    20, 1, false, false, true));

            attacker.displayClientMessage(
                    Component.literal("✗ Parried!")
                            .withStyle(style -> style.withColor(0xFF5555).withBold(true)),
                    true
            );

            attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f);

        } catch (Exception e) {
            sendParriedCooldown(attacker, "Move (Parried)", PARRIED_ATTACK_COOLDOWN);
        }
    }

    /**
     * Send cooldown info to client for parried attacks
     */
    private static void sendParriedCooldown(ServerPlayer player, String moveName, int cooldownTicks) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(moveName);
            buf.writeInt(cooldownTicks);

            NetworkManager.sendToPlayer(player, COOLDOWN_PACKET_ID, buf);
        } catch (Exception ignored) {
        }
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
                    Component.literal("✗ Stance broken!")
                            .withStyle(style -> style.withColor(0xFF5555).withBold(true)),
                    true // Overlay message
            );

            return false; // Stance broken, take full damage
        }

        // Play block clang sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                NicirinSoundRegistry.BLOCK_CLANG.get(), SoundSource.PLAYERS, 0.9f, 1.0f);

        player.displayClientMessage(
                Component.literal("🛡 Blocked!")
                        .withStyle(style -> style.withColor(0xAAAAAA)),
                true
        );

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
            blockingTag.putLong("blockCooldownUntil", state.blockCooldownUntil);
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
                state.blockCooldownUntil = blockingTag.getLong("blockCooldownUntil");

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