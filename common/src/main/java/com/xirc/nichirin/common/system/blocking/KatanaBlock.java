package com.xirc.nichirin.common.system.blocking;

import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.config.NichirinModConfig.BlockingConfig;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity;
import com.xirc.nichirin.common.system.StanceManager;
import com.xirc.nichirin.common.util.ComboTracker;
import com.xirc.nichirin.common.util.InputHandler;
import com.xirc.nichirin.common.util.NetworkBufferUtils;
import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.moveset.DefaultKatanaMoveset;
import com.xirc.nichirin.common.attack.moveset.DualKatanaMoveset;
import com.xirc.nichirin.common.item.katana.Katana;
import com.xirc.nichirin.common.network.s2c.PlayerAnimationPacket;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinSoundRegistry;
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
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Katana blocking and parrying system.
 * Works for both players and NPCs (trainers, demons).
 * Uses stance as the blocking resource for players; NPCs block freely but are time-limited by their AI.
 */
public class KatanaBlock {

    private static final Map<UUID, BlockingState> BLOCKING_STATES = new HashMap<>();

    private static final int PARRY_WINDOW_TICKS = 10;
    private static final int PARRIED_ATTACK_COOLDOWN = 100;

    // Blocking tuning is read from config (blocking.* in nichirin-server.toml).
    private static BlockingConfig bcfg() {
        return NichirinModConfig.get().blocking;
    }
    private static float blockStanceDrain()      { return (float) bcfg().blockStanceDrain; }
    private static float parryStanceCost()       { return (float) bcfg().parryStanceCost; }
    private static int   blockCooldownTicks()    { return bcfg().blockCooldownTicks; }
    private static int   earlyReleaseStunTicks() { return bcfg().earlyReleaseStunTicks; }
    private static float backstabAngle()         { return (float) bcfg().backstabAngle; }

    private static final ResourceLocation COOLDOWN_PACKET_ID = ResourceLocation.fromNamespaceAndPath("nichirin", "cooldown_display");

    public enum BlockingStance {
        NONE,
        BLOCKING,
        PARRY_READY,
        PARRY_SUCCESS,
        PARRY_FAILED
    }

    private static class BlockingState {
        BlockingStance stance = BlockingStance.NONE;
        int blockTicks = 0;
        int parryWindowTicks = 0;
        long blockCooldownUntil = 0;
        long blockStunUntil = 0;   // can't release the guard until this game-time
        boolean wasBlockingLastTick = false;
        boolean wasAttackingLastTick = false;

        void reset() {
            stance = BlockingStance.NONE;
            blockTicks = 0;
            parryWindowTicks = 0;
            blockStunUntil = 0;
            wasAttackingLastTick = false;
        }

        void setBlockCooldown(long currentTime) {
            blockCooldownUntil = currentTime + blockCooldownTicks();
        }

        boolean isOnCooldown(long currentTime) {
            return currentTime < blockCooldownUntil;
        }
    }


    public static boolean startBlocking(LivingEntity entity) {
        if (entity.level().isClientSide) return false;
        if (!isKatanaBlocker(entity)) return false;

        BlockingState state = getOrCreateState(entity);

        if (!canStartBlocking(entity, state)) return false;

        // Parry window opens at the START of the block (click-timed).
        state.blockTicks = 0;
        if (parryAllowed(entity)) {
            state.stance = BlockingStance.PARRY_READY;
            state.parryWindowTicks = NichirinModConfig.get().combat.parryWindowTicks;
            if (entity instanceof Player player) {
                player.displayClientMessage(
                        Component.literal("Guard Up!")
                                .withStyle(style -> style.withColor(0xFFAA00).withBold(true)),
                        true);
            }
        } else {
            state.stance = BlockingStance.BLOCKING;
            state.parryWindowTicks = 0;
            if (entity instanceof Player player) {
                player.displayClientMessage(
                        Component.literal("Blocking")
                                .withStyle(style -> style.withColor(0xAAAAAA)),
                        true);
            }
        }

        applyBlockingEffect(entity);

        if (entity instanceof Player player) {
            InputHandler.blockAfterBreathingMove(player);
        }

        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.7f, 1.2f);

        if (entity instanceof ServerPlayer serverPlayer) {
            NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer,
                    new PlayerAnimationPacket(serverPlayer.getId(), blockAnimation(entity)));
        } else if (entity instanceof BaseBreathingTrainerEntity trainer) {
            trainer.setAnimation("sword.block", 1.0f);
        }

        return true;
    }


    public static void stopBlocking(LivingEntity entity) {
        if (entity.level().isClientSide) return;

        BlockingState state = BLOCKING_STATES.get(entity.getUUID());
        if (state == null || state.stance == BlockingStance.NONE) return;

        long currentTime = entity.level().getGameTime();

        // Blockstun: after taking a blocked hit you're locked in guard for a few ticks.
        if (currentTime < state.blockStunUntil && state.stance == BlockingStance.BLOCKING) {
            return;
        }

        boolean wasInParryWindow = (state.stance == BlockingStance.PARRY_READY && state.parryWindowTicks > 0);

        if (wasInParryWindow) {
            if (entity instanceof Player player) {
                applyEarlyReleasePunishment(player);
            }
            state.setBlockCooldown(currentTime);
        } else if (state.stance != BlockingStance.PARRY_SUCCESS) {
            state.setBlockCooldown(currentTime);
        }

        removeBlockingEffect(entity);

        if (entity instanceof ServerPlayer serverPlayer && state.stance != BlockingStance.PARRY_SUCCESS) {
            NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer,
                    new PlayerAnimationPacket(serverPlayer.getId(), ""));
        } else if (!(entity instanceof Player) && state.stance != BlockingStance.PARRY_SUCCESS) {
            if (entity instanceof BaseBreathingTrainerEntity trainer) {
                trainer.setAnimation("", 1.0f);
            }
        }

        state.reset();
    }


    public static boolean handleIncomingDamage(LivingEntity defender, LivingEntity attacker, float damage) {
        if (defender.level().isClientSide) return false;

        BlockingState state = BLOCKING_STATES.get(defender.getUUID());
        if (state == null || state.stance == BlockingStance.NONE) return false;

        if (attacker != null && isBackstab(defender, attacker)) {
            state.blockStunUntil = 0; // backstab always breaks the guard
            stopBlocking(defender);
            return false;
        }

        if (state.stance == BlockingStance.PARRY_READY && state.parryWindowTicks > 0
                && NichirinModConfig.get().combat.enableParrySystem) {
            return handleSuccessfulParry(defender, attacker, state);
        }

        if (state.stance == BlockingStance.BLOCKING) {
            boolean blocked = handleSuccessfulBlock(defender, state, damage);
            if (blocked) {
                // Formula: 4 + damage/2 ticks of forced guard.
                state.blockStunUntil = defender.level().getGameTime() + 4 + Math.round(damage / 2.0f);
            }
            return blocked;
        }

        return false;
    }

    // Keep old signature for backwards compat
    public static boolean handleIncomingDamage(Player player, Player attacker, float damage) {
        return handleIncomingDamage((LivingEntity) player, (LivingEntity) attacker, damage);
    }


    public static void tick(LivingEntity entity) {
        if (entity.level().isClientSide) return;

        BlockingState state = BLOCKING_STATES.get(entity.getUUID());
        if (state == null) return;

        boolean currentlyBlocking = (state.stance == BlockingStance.BLOCKING ||
                state.stance == BlockingStance.PARRY_READY);

        if (currentlyBlocking) {
            // Count how long the guard's been held (drives guard-loss scaling).
            state.blockTicks++;

            // Click-timed parry window decays into a plain block once it expires.
            if (state.stance == BlockingStance.PARRY_READY) {
                state.parryWindowTicks--;
                if (state.parryWindowTicks <= 0) {
                    state.stance = BlockingStance.BLOCKING;
                }
            }

            if (!entity.hasEffect(NichirinEffectRegistry.blocking())) {
                applyBlockingEffect(entity);
            }

            // A block + left-click special replaces the held block animation while it plays. When it
            // finishes and the guard is still up, resume the block hold animation for everyone.
            boolean attacking = MoveExecutor.hasActiveAttacks(entity);
            if (state.wasAttackingLastTick && !attacking) {
                if (entity instanceof ServerPlayer serverPlayer) {
                    NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer,
                            new PlayerAnimationPacket(serverPlayer.getId(), blockAnimation(entity)));
                } else if (entity instanceof BaseBreathingTrainerEntity trainer) {
                    trainer.setAnimation("sword.block", 1.0f);
                }
            }
            state.wasAttackingLastTick = attacking;
        } else {
            state.wasAttackingLastTick = false;
            if (state.wasBlockingLastTick) {
                removeBlockingEffect(entity);
            }
        }

        state.wasBlockingLastTick = currentlyBlocking;
    }

    // Keep old Player signature
    public static void tick(Player player) { tick((LivingEntity) player); }


    public static BlockingStance getStance(LivingEntity entity) {
        BlockingState state = BLOCKING_STATES.get(entity.getUUID());
        return state != null ? state.stance : BlockingStance.NONE;
    }

    public static boolean isBlocking(LivingEntity entity) {
        BlockingStance stance = getStance(entity);
        return stance == BlockingStance.BLOCKING || stance == BlockingStance.PARRY_READY;
    }

    public static void cleanupPlayer(Player player) {
        BLOCKING_STATES.remove(player.getUUID());
    }

    public static void cleanup(LivingEntity entity) {
        BLOCKING_STATES.remove(entity.getUUID());
    }


    private static BlockingState getOrCreateState(LivingEntity entity) {
        return BLOCKING_STATES.computeIfAbsent(entity.getUUID(), k -> new BlockingState());
    }

    private static boolean isKatanaBlocker(LivingEntity entity) {
        if (!(entity instanceof Player player)) return true;
        return player.getMainHandItem().getItem() instanceof Katana
                || player.getOffhandItem().getItem() instanceof Katana;
    }

    private static boolean isDualWieldingKatanas(LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        return player.getMainHandItem().getItem() instanceof Katana
                && player.getOffhandItem().getItem() instanceof Katana;
    }

    private static String blockAnimation(LivingEntity entity) {
        return isDualWieldingKatanas(entity) ? "dual_block" : "sword.block";
    }

    private static String parryAnimation(LivingEntity entity) {
        return isDualWieldingKatanas(entity) ? "dual_parry" : "sword.parry";
    }

    private static boolean isCqcBlocker(LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        return false;
    }

    private static int resistanceAmplifier(LivingEntity entity) {
        return 2;
    }

    private static boolean parryAllowed(LivingEntity entity) {
        return NichirinModConfig.get().combat.enableParrySystem;
    }

    private static final float BASE_GUARD_LOSS = 10.0f;

    /** Stance cost per blocked hit, reduced the longer you've held guard and the slower you move. */
    private static float guardLossFor(LivingEntity defender, BlockingState state) {
        // Held longer = steadier guard: down to 0.5x after ~50 ticks (2.5s) of holding.
        float heldFactor = Math.max(0.5f, 1.0f - state.blockTicks * 0.01f);
        // Slower = more planted: standing still ~0.5x, walking ~0.9x, sprinting ~1.0x.
        float speed = (float) defender.getDeltaMovement().horizontalDistance();
        float speedFactor = Math.min(1.0f, 0.5f + speed * 4.0f);
        return BASE_GUARD_LOSS * heldFactor * speedFactor;
    }

    private static boolean canStartBlocking(LivingEntity entity, BlockingState state) {
        if (state.stance != BlockingStance.NONE) return false;

        if (entity.hasEffect(NichirinEffectRegistry.stunned())) {
            if (entity instanceof Player player) {
                player.displayClientMessage(
                        Component.literal("Cannot block while stunned!")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true);
            }
            return false;
        }

        long currentTime = entity.level().getGameTime();
        if (state.isOnCooldown(currentTime)) {
            if (entity instanceof Player player) {
                int remainingTicks = (int) (state.blockCooldownUntil - currentTime);
                player.displayClientMessage(
                        Component.literal("Block on cooldown (" + (remainingTicks / 20.0f) + "s)")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true);
            }
            return false;
        }

        // Stance check only for players
        if (entity instanceof Player player) {
            if (!StanceManager.hasStance(player, blockStanceDrain() * 25)) {
                player.displayClientMessage(
                        Component.literal("Not enough stance to block!")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true);
                return false;
            }
        }

        return true;
    }

    private static void applyEarlyReleasePunishment(Player player) {
        MobEffectInstance stunEffect = new MobEffectInstance(
                NichirinEffectRegistry.stunned(),
                earlyReleaseStunTicks(), 0, false, false, true);
        player.addEffect(stunEffect);

        player.displayClientMessage(
                Component.literal("Early release!")
                        .withStyle(style -> style.withColor(0xFF5555)),
                true);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.5f, 1.5f);

        if (player instanceof ServerPlayer serverPlayer) {
            NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer,
                    new PlayerAnimationPacket(serverPlayer.getId(), "sword.early_release"));
        }
    }

    private static boolean isBackstab(LivingEntity defender, LivingEntity attacker) {
        Vec3 defenderLook = defender.getLookAngle();
        Vec3 flatLook = new Vec3(defenderLook.x, 0, defenderLook.z).normalize();
        Vec3 toAttacker = attacker.position().subtract(defender.position());
        Vec3 flatToAttacker = new Vec3(toAttacker.x, 0, toAttacker.z).normalize();

        if (flatLook.lengthSqr() < 0.001 || flatToAttacker.lengthSqr() < 0.001) {
            return false;
        }

        double dot = flatLook.dot(flatToAttacker);
        double angle = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
        return angle > backstabAngle();
    }

    private static boolean handleSuccessfulParry(LivingEntity defender, LivingEntity attacker, BlockingState state) {
        state.stance = BlockingStance.PARRY_SUCCESS;
        state.parryWindowTicks = 0;

        if (defender instanceof Player player) {
            player.displayClientMessage(
                    Component.literal("Perfect Parry!")
                            .withStyle(style -> style.withColor(0x00FF00).withBold(true)),
                    true);
        }

        if (defender.hasEffect(NichirinEffectRegistry.stunned())) {
            defender.removeEffect(NichirinEffectRegistry.stunned());
        }

        // Defender parry animation
        if (defender instanceof ServerPlayer serverPlayer) {
            NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer,
                    new PlayerAnimationPacket(serverPlayer.getId(), parryAnimation(defender)));
        } else if (defender instanceof BaseBreathingTrainerEntity trainer) {
            trainer.setAnimation("sword.parry", 1.0f);
        }

        // Interrupt + stun the attacker
        if (attacker != null) {
            interruptAttacker(attacker);
        }

        return true;
    }

    private static void interruptAttacker(LivingEntity attacker) {
        try {
            MoveExecutor.clearAttacks(attacker);
            if (attacker instanceof Player p) {
                AbstractBreathingAttack.clearSelfTickingAttacks(p);
            }

            if (attacker instanceof ServerPlayer serverPlayer) {
                DefaultKatanaMoveset.interruptPlayerAttack(serverPlayer);
                DualKatanaMoveset.interruptPlayerAttack(serverPlayer);
                NichirinPacketRegistry.clearInputBlock(serverPlayer);
                sendParriedCooldown(serverPlayer, "Move (Parried)", PARRIED_ATTACK_COOLDOWN);
            }

            int parryStunTicks = 20;
            attacker.addEffect(new MobEffectInstance(
                    NichirinEffectRegistry.stunned(),
                    parryStunTicks, 1, false, false, true));
            ComboTracker.markParryStunned(attacker, parryStunTicks);

            if (attacker instanceof Player player) {
                player.displayClientMessage(
                        Component.literal("Parried!")
                                .withStyle(style -> style.withColor(0xFF5555).withBold(true)),
                        true);
            }

            attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f);

        } catch (Exception e) {
            if (attacker instanceof ServerPlayer serverPlayer) {
                sendParriedCooldown(serverPlayer, "Move (Parried)", PARRIED_ATTACK_COOLDOWN);
            }
        }
    }

    private static void sendParriedCooldown(ServerPlayer player, String moveName, int cooldownTicks) {
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(moveName);
            buf.writeInt(cooldownTicks);
            NetworkManager.sendToPlayer(player, COOLDOWN_PACKET_ID, NetworkBufferUtils.server(buf, player));
        } catch (Exception ignored) {
        }
    }

    private static boolean handleSuccessfulBlock(LivingEntity defender, BlockingState state, float damage) {
        // Stance cost only for players
        if (defender instanceof Player player) {
            boolean glassStance = PlayerDataProvider.getData(player).getPerkData().hasFlaw("glass_stance");
            float stanceCost = glassStance ? Float.MAX_VALUE : guardLossFor(defender, state);
            if (!StanceManager.consume(player, stanceCost)) {
                MobEffectInstance stunEffect = new MobEffectInstance(
                        NichirinEffectRegistry.stunned(),
                        60, 0, false, true, true);
                player.addEffect(stunEffect);
                stopBlocking(player);
                player.displayClientMessage(
                        Component.literal("Stance broken!")
                                .withStyle(style -> style.withColor(0xFF5555).withBold(true)),
                        true);
                return false;
            }
        }

        // Clang feedback
        if (defender instanceof ServerPlayer serverPlayer) {
            NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer,
                    new PlayerAnimationPacket(serverPlayer.getId(), blockAnimation(defender)));
        } else if (defender instanceof BaseBreathingTrainerEntity trainer) {
            trainer.setAnimation("sword.block", 1.0f);
        }
        defender.level().playSound(null, defender.getX(), defender.getY(), defender.getZ(),
                NichirinSoundRegistry.BLOCK_CLANG.get(), SoundSource.PLAYERS, 0.9f, 1.0f);

        if (defender instanceof Player player) {
            player.displayClientMessage(
                    Component.literal("Blocked!")
                            .withStyle(style -> style.withColor(0xAAAAAA)),
                    true);
        }

        return true;
    }

    private static void applyBlockingEffect(LivingEntity entity) {
        MobEffectInstance blockingEffect = new MobEffectInstance(
                NichirinEffectRegistry.blocking(),
                Integer.MAX_VALUE, 0, false, false, true);
        entity.addEffect(blockingEffect);

        MobEffectInstance resistanceEffect = new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                Integer.MAX_VALUE, resistanceAmplifier(entity), false, false, true);
        entity.addEffect(resistanceEffect);
    }

    private static void removeBlockingEffect(LivingEntity entity) {
        entity.removeEffect(NichirinEffectRegistry.blocking());
        entity.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }


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

    public static void load(Player player, CompoundTag tag) {
        if (tag.contains("BlockingData")) {
            CompoundTag blockingTag = tag.getCompound("BlockingData");
            BlockingState state = getOrCreateState(player);
            try {
                state.stance = BlockingStance.valueOf(blockingTag.getString("stance"));
                state.blockTicks = blockingTag.getInt("blockTicks");
                state.parryWindowTicks = blockingTag.getInt("parryWindowTicks");
                state.blockCooldownUntil = blockingTag.getLong("blockCooldownUntil");
                if (state.stance == BlockingStance.BLOCKING || state.stance == BlockingStance.PARRY_READY) {
                    applyBlockingEffect(player);
                }
            } catch (IllegalArgumentException e) {
                state.reset();
            }
        }
    }

    // Legacy Player-only overloads
    public static boolean startBlocking(Player player) { return startBlocking((LivingEntity) player); }
    public static void stopBlocking(Player player) { stopBlocking((LivingEntity) player); }
    public static BlockingStance getStance(Player player) { return getStance((LivingEntity) player); }
    public static boolean isBlocking(Player player) { return isBlocking((LivingEntity) player); }

    public static boolean attemptParry(Player player) { return false; }
}
