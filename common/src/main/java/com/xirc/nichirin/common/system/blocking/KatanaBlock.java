package com.xirc.nichirin.common.system.blocking;

import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.system.StanceManager;
import com.xirc.nichirin.common.util.InputHandler;
import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.moveset.DefaultKatanaMoveset;
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

    private static final float BLOCK_STANCE_DRAIN = 0.8f;
    private static final float PARRY_STANCE_COST = 15.0f;
    private static final int PARRY_WINDOW_TICKS = 10;
    private static final int BLOCK_COOLDOWN_TICKS = 15;
    private static final int EARLY_RELEASE_STUN_TICKS = 4;
    private static final float BACKSTAB_ANGLE = 90.0f;
    private static final int PARRIED_ATTACK_COOLDOWN = 100;

    private static final ResourceLocation COOLDOWN_PACKET_ID = new ResourceLocation("nichirin", "cooldown_display");

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

    // ── Start blocking ──────────────────────────────────────────────

    public static boolean startBlocking(LivingEntity entity) {
        if (entity.level().isClientSide) return false;

        BlockingState state = getOrCreateState(entity);

        if (!canStartBlocking(entity, state)) return false;

        state.blockTicks = 0;
        if (NichirinModConfig.get().combat.enableParrySystem) {
            state.stance = BlockingStance.PARRY_READY;
            state.parryWindowTicks = NichirinModConfig.get().combat.parryWindowTicks;
            if (entity instanceof Player player) {
                player.displayClientMessage(
                        Component.literal("⚔ Guard Up!")
                                .withStyle(style -> style.withColor(0xFFAA00).withBold(true)),
                        true);
            }
        } else {
            state.stance = BlockingStance.BLOCKING;
            state.parryWindowTicks = 0;
            if (entity instanceof Player player) {
                player.displayClientMessage(
                        Component.literal("⚔ Blocking")
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
                    new PlayerAnimationPacket(serverPlayer.getId(), "sword.block"));
        } else if (entity instanceof com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity trainer) {
            trainer.setAnimation("sword.block", 1.0f);
        }

        return true;
    }

    // ── Stop blocking ───────────────────────────────────────────────

    public static void stopBlocking(LivingEntity entity) {
        if (entity.level().isClientSide) return;

        BlockingState state = BLOCKING_STATES.get(entity.getUUID());
        if (state == null || state.stance == BlockingStance.NONE) return;

        long currentTime = entity.level().getGameTime();
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
            if (entity instanceof com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity trainer) {
                trainer.setAnimation("", 1.0f);
            }
        }

        state.reset();
    }

    // ── Incoming damage ─────────────────────────────────────────────

    public static boolean handleIncomingDamage(LivingEntity defender, LivingEntity attacker, float damage) {
        if (defender.level().isClientSide) return false;

        BlockingState state = BLOCKING_STATES.get(defender.getUUID());
        if (state == null || state.stance == BlockingStance.NONE) return false;

        if (attacker != null && isBackstab(defender, attacker)) {
            stopBlocking(defender);
            return false;
        }

        if (state.stance == BlockingStance.PARRY_READY && state.parryWindowTicks > 0
                && NichirinModConfig.get().combat.enableParrySystem) {
            return handleSuccessfulParry(defender, attacker, state);
        }

        if (state.stance == BlockingStance.BLOCKING) {
            return handleSuccessfulBlock(defender, state, damage);
        }

        return false;
    }

    // Keep old signature for backwards compat
    public static boolean handleIncomingDamage(Player player, Player attacker, float damage) {
        return handleIncomingDamage((LivingEntity) player, (LivingEntity) attacker, damage);
    }

    // ── Tick ─────────────────────────────────────────────────────────

    public static void tick(LivingEntity entity) {
        if (entity.level().isClientSide) return;

        BlockingState state = BLOCKING_STATES.get(entity.getUUID());
        if (state == null) return;

        boolean currentlyBlocking = (state.stance == BlockingStance.BLOCKING ||
                state.stance == BlockingStance.PARRY_READY);

        if (currentlyBlocking) {
            state.blockTicks++;

            if (state.stance == BlockingStance.PARRY_READY) {
                state.parryWindowTicks--;
                if (state.parryWindowTicks <= 0) {
                    state.stance = BlockingStance.BLOCKING;
                }
            }

            if (!entity.hasEffect(NichirinEffectRegistry.BLOCKING.get())) {
                applyBlockingEffect(entity);
            }
        } else {
            if (state.wasBlockingLastTick) {
                removeBlockingEffect(entity);
            }
        }

        state.wasBlockingLastTick = currentlyBlocking;
    }

    // Keep old Player signature
    public static void tick(Player player) { tick((LivingEntity) player); }

    // ── Queries ──────────────────────────────────────────────────────

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

    // ── Private helpers ──────────────────────────────────────────────

    private static BlockingState getOrCreateState(LivingEntity entity) {
        return BLOCKING_STATES.computeIfAbsent(entity.getUUID(), k -> new BlockingState());
    }

    private static boolean canStartBlocking(LivingEntity entity, BlockingState state) {
        if (state.stance != BlockingStance.NONE) return false;

        if (entity.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
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
            if (!StanceManager.hasStance(player, BLOCK_STANCE_DRAIN * 25)) {
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
                NichirinEffectRegistry.STUNNED.get(),
                EARLY_RELEASE_STUN_TICKS, 0, false, false, true);
        player.addEffect(stunEffect);

        player.displayClientMessage(
                Component.literal("✗ Early release!")
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
        return angle > BACKSTAB_ANGLE;
    }

    private static boolean handleSuccessfulParry(LivingEntity defender, LivingEntity attacker, BlockingState state) {
        state.stance = BlockingStance.PARRY_SUCCESS;
        state.parryWindowTicks = 0;

        if (defender instanceof Player player) {
            player.displayClientMessage(
                    Component.literal("✦ Perfect Parry!")
                            .withStyle(style -> style.withColor(0x00FF00).withBold(true)),
                    true);
        }

        if (defender.hasEffect(NichirinEffectRegistry.STUNNED.get())) {
            defender.removeEffect(NichirinEffectRegistry.STUNNED.get());
        }

        // Defender parry animation
        if (defender instanceof ServerPlayer serverPlayer) {
            NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer,
                    new PlayerAnimationPacket(serverPlayer.getId(), "sword.parry"));
        } else if (defender instanceof com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity trainer) {
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
                NichirinPacketRegistry.clearInputBlock(serverPlayer);
                sendParriedCooldown(serverPlayer, "Move (Parried)", PARRIED_ATTACK_COOLDOWN);
            }

            attacker.addEffect(new MobEffectInstance(
                    NichirinEffectRegistry.STUNNED.get(),
                    20, 1, false, false, true));

            if (attacker instanceof Player player) {
                player.displayClientMessage(
                        Component.literal("✗ Parried!")
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
            NetworkManager.sendToPlayer(player, COOLDOWN_PACKET_ID, buf);
        } catch (Exception ignored) {
        }
    }

    private static boolean handleSuccessfulBlock(LivingEntity defender, BlockingState state, float damage) {
        // Stance cost only for players
        if (defender instanceof Player player) {
            boolean glassStance = PlayerDataProvider.getData(player).getPerkData().hasFlaw("glass_stance");
            float stanceCost = glassStance ? Float.MAX_VALUE : 10.0f;
            if (!StanceManager.consume(player, stanceCost)) {
                MobEffectInstance stunEffect = new MobEffectInstance(
                        NichirinEffectRegistry.STUNNED.get(),
                        60, 0, false, true, true);
                player.addEffect(stunEffect);
                stopBlocking(player);
                player.displayClientMessage(
                        Component.literal("✗ Stance broken!")
                                .withStyle(style -> style.withColor(0xFF5555).withBold(true)),
                        true);
                return false;
            }
        }
        // NPCs don't use stance — they always block successfully (AI controls guard duration)

        // Clang feedback
        if (defender instanceof ServerPlayer serverPlayer) {
            NichirinPacketRegistry.broadcastPlayerAnimation(serverPlayer,
                    new PlayerAnimationPacket(serverPlayer.getId(), "sword.block"));
        } else if (defender instanceof com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity trainer) {
            trainer.setAnimation("sword.block", 1.0f);
        }
        defender.level().playSound(null, defender.getX(), defender.getY(), defender.getZ(),
                NicirinSoundRegistry.BLOCK_CLANG.get(), SoundSource.PLAYERS, 0.9f, 1.0f);

        if (defender instanceof Player player) {
            player.displayClientMessage(
                    Component.literal("🛡 Blocked!")
                            .withStyle(style -> style.withColor(0xAAAAAA)),
                    true);
        }

        return true;
    }

    private static void applyBlockingEffect(LivingEntity entity) {
        MobEffectInstance blockingEffect = new MobEffectInstance(
                NichirinEffectRegistry.BLOCKING.get(),
                Integer.MAX_VALUE, 0, false, false, true);
        entity.addEffect(blockingEffect);

        MobEffectInstance resistanceEffect = new MobEffectInstance(
                MobEffects.DAMAGE_RESISTANCE,
                Integer.MAX_VALUE, 3, false, false, true);
        entity.addEffect(resistanceEffect);
    }

    private static void removeBlockingEffect(LivingEntity entity) {
        entity.removeEffect(NichirinEffectRegistry.BLOCKING.get());
        entity.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }

    // ── NBT persistence (Player only) ────────────────────────────────

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
