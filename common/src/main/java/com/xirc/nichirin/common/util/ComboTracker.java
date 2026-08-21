package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.network.s2c.ComboCounterPacket;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Central combo tracking logic with automatic reset when stun expires
 * Tracks combo scaling and anti-spam penalties for repeated moves.
 */
public class ComboTracker {

    // Track which players are combo-ing which entities
    // Map: Victim UUID -> Set of Player UUIDs who are combo-ing this victim
    private static final Map<UUID, Set<UUID>> victimToAttackers = new HashMap<>();

    // Track last move used by each player for anti-spam detection
    // Map: Player UUID -> Last move ID used
    private static final Map<UUID, String> playerLastMove = new HashMap<>();

    // Entities stunned specifically by a parry. Normal combo stuns also use STUNNED, so this
    // side-channel lets counterattacks distinguish a real parry punish window.
    private static final Map<UUID, Long> parryStunnedUntil = new HashMap<>();
    // Game tick of each attacker's last landed combo hit — drives the idle-timeout reset so the
    // combo bar always clears even when the victim's stun-expiry event is missed (e.g. CQC's
    // rapid re-stuns overwriting each other).
    private static final Map<UUID, Long> lastComboHitTick = new HashMap<>();
    private static final int COMBO_IDLE_RESET_TICKS = 50;
    private static final Map<UUID, AttackStyleContext> currentAttacks = new HashMap<>();
    private static final Map<UUID, Integer> playerStyleScores = new HashMap<>();
    private static final Map<UUID, String> playerLastScoredMove = new HashMap<>();
    private static final Map<UUID, Long> playerAttackSequences = new HashMap<>();
    private static final Map<UUID, AttackHitContext> lastAttackHits = new HashMap<>();

    /** Falloff never drives a hit below this fraction of its base damage (so combos can't deal ~0). */
    private static final float COMBO_FALLOFF_FLOOR = 0.25f;

    private record AttackStyleContext(long sequence, String moveId, boolean awarded) {}
    private record AttackHitContext(long sequence, UUID victimId, int comboCount, boolean comboEligible) {}

    /**
     * Check if a move should have reduced hitstun due to spam detection
     * Call this before executing a move to modify hitstun accordingly
     *
     * @param player The player performing the move
     * @param moveId The ID of the move being performed
     * @param originalHitStun The original hitstun value
     * @return Modified hitstun value (0 if spamming same move)
     */
    public static int getModifiedHitStun(Player player, String moveId, int originalHitStun) {
        String lastMove = playerLastMove.get(player.getUUID());
        boolean inCombo = player instanceof IComboCounter comboCounter
                && comboCounter.nichirin$getComboCount() > 0;

        if (inCombo && moveId != null && moveId.equals(lastMove)) {
            return 0;
        }

        // Update last move
        playerLastMove.put(player.getUUID(), moveId);
        return originalHitStun; // Normal hitstun for non-repeated moves
    }

    /**
     * Damage and stun multiplier applied once when an attack starts.
     * Multi-hit attacks keep this single multiplier for their whole lifetime.
     *
     * <p>Controlled by the {@code combo_damage_mode} config: flat (no scaling), falloff (each combo hit
     * deals progressively less, down to {@link #COMBO_FALLOFF_FLOOR}), or rampup (each hit deals more).
     * The per-count magnitude is {@code combo_damage_rate_percent}.</p>
     */
    public static float getAttackComboMultiplier(Player player) {
        if (!(player instanceof IComboCounter comboCounter)) {
            return 1.0f;
        }
        int comboCount = Math.max(0, comboCounter.nichirin$getComboCount());
        if (comboCount <= 0) {
            return 1.0f;
        }
        int mode = com.xirc.nichirin.common.config.NichirinConfig.getInt(
                com.xirc.nichirin.common.config.NichirinConfig.COMBO_DAMAGE_MODE);
        float rate = com.xirc.nichirin.common.config.NichirinConfig.getInt(
                com.xirc.nichirin.common.config.NichirinConfig.COMBO_DAMAGE_RATE_PERCENT) / 100.0f;
        return switch (mode) {
            case 1 -> Math.max(COMBO_FALLOFF_FLOOR, 1.0f - comboCount * rate); // falloff
            case 2 -> 1.0f + comboCount * rate;                                // rampup
            default -> 1.0f;                                                   // flat rate
        };
    }

    public static int scaleHitStunForCombo(Player player, int baseHitStun) {
        if (baseHitStun <= 0) {
            return baseHitStun;
        }
        return Math.max(1, Math.round(baseHitStun * getAttackComboMultiplier(player)));
    }

    public static void markParryStunned(LivingEntity entity, int durationTicks) {
        if (entity == null || entity.level().isClientSide || durationTicks <= 0) {
            return;
        }
        parryStunnedUntil.put(entity.getUUID(), entity.level().getGameTime() + durationTicks);
    }

    public static boolean isParryStunned(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) {
            return false;
        }
        Long until = parryStunnedUntil.get(entity.getUUID());
        if (until == null) {
            return false;
        }
        if (entity.level().getGameTime() > until || !canContinueCombo(entity)) {
            parryStunnedUntil.remove(entity.getUUID());
            return false;
        }
        return true;
    }

    public static long registerAttackStart(Player player, String moveId) {
        if (player == null || player.level().isClientSide) {
            return 0L;
        }
        UUID playerId = player.getUUID();
        long sequence = playerAttackSequences.getOrDefault(playerId, 0L) + 1L;
        playerAttackSequences.put(playerId, sequence);
        currentAttacks.put(playerId, new AttackStyleContext(sequence, moveId != null ? moveId : "", false));
        return sequence;
    }

    /**
     * Handle combo logic when a player hits an entity
     * Should be called after successful hit and stun application
     *
     * @param attacker The player performing the attack
     * @param victim The entity being attacked
     * @param stunDurationTicks Duration of stun applied to victim
     * @param damage Damage dealt by this hit
     * @param wasAlreadyStunned Whether the victim was stunned before this hit
     */
    public static void handleHit(Player attacker, LivingEntity victim, int stunDurationTicks, float damage, boolean wasAlreadyStunned) {
        handleHit(attacker, victim, stunDurationTicks, damage, wasAlreadyStunned, 0L);
    }

    public static void handleHit(Player attacker, LivingEntity victim, int stunDurationTicks, float damage,
                                 boolean wasAlreadyStunned, long attackSequence) {
        if (!(attacker instanceof ServerPlayer serverPlayer)) {
            return; // Server-side only
        }

        if (victim == null || !victim.isAlive()) {
            return;
        }

        // Safety check - make sure mixin is applied
        if (!(attacker instanceof IComboCounter)) {
            return;
        }

        IComboCounter comboCounter = (IComboCounter) attacker;
        LivingEntity lastAttacked = comboCounter.nichirin$getLastAttacked();
        UUID attackerId = attacker.getUUID();
        AttackHitContext previousHit = lastAttackHits.get(attackerId);
        boolean sameAttackContinuation = attackSequence > 0L
                && stunDurationTicks > 0
                && previousHit != null
                && previousHit.comboEligible
                && previousHit.sequence == attackSequence
                && previousHit.victimId.equals(victim.getUUID());

        boolean startedFreshCombo = false;
        if (sameAttackContinuation) {
            comboCounter.nichirin$setComboCount(Math.max(
                    comboCounter.nichirin$getComboCount(), previousHit.comboCount) + 1);
            comboCounter.nichirin$setLastAttacked(victim);
        } else if (lastAttacked != victim) {
            comboCounter.nichirin$setComboCount(1);
            comboCounter.nichirin$setLastAttacked(victim);
            startedFreshCombo = true;
        } else {
            if (wasAlreadyStunned && stunDurationTicks > 0) {
                comboCounter.nichirin$incrementComboCount();
            } else {
                comboCounter.nichirin$setComboCount(1);
                startedFreshCombo = true;
            }

            comboCounter.nichirin$setLastAttacked(victim);
        }

        registerAttackerVictimPair(attackerId, victim.getUUID());
        lastComboHitTick.put(attackerId, attacker.level().getGameTime());

        int comboCount = comboCounter.nichirin$getComboCount();
        lastAttackHits.put(attackerId, new AttackHitContext(
                attackSequence, victim.getUUID(), comboCount, stunDurationTicks > 0));
        if (startedFreshCombo) {
            resetStyle(attackerId);
        }
        int styleScore = awardStyleForAttack(attacker, victim, comboCount);
        ComboCounterPacket packet = new ComboCounterPacket(comboCount, stunDurationTicks, damage, styleScore, rankFor(styleScore));

        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            packet.toBytes(buf);
            NetworkManager.sendToPlayer(serverPlayer,
                    NichirinPacketRegistry.COMBO_COUNTER_ID, NetworkBufferUtils.server(buf, serverPlayer));
        } catch (Exception e) {
        }
    }

    /**
     * Overloaded method for backward compatibility (assumes not previously stunned)
     */
    public static void handleHit(Player attacker, LivingEntity victim, int stunDurationTicks, float damage) {
        handleHit(attacker, victim, stunDurationTicks, damage, false);
    }

    /**
     * Overloaded method for backward compatibility (no damage tracking)
     */
    public static void handleHit(Player attacker, LivingEntity victim, int stunDurationTicks) {
        handleHit(attacker, victim, stunDurationTicks, 0.0f, false);
    }

    /**
     * Apply stun effect to entity with specified duration
     *
     * @param entity Entity to stun
     * @param durationTicks Duration in ticks
     */
    public static void applyStun(LivingEntity entity, int durationTicks) {
        if (entity == null || durationTicks <= 0) {
            return;
        }
        // Never stun creative/spectator players.
        if (com.xirc.nichirin.common.util.NichirinDamageHandler.isImmune(entity)) {
            return;
        }

        // Apply STUNNED effect
        MobEffectInstance stunEffect = new MobEffectInstance(
                NichirinEffectRegistry.stunned(),
                durationTicks,
                1, // Amplifier 1 — full lockdown (movement + AI disabled)
                false, // Not ambient
                false, // Not visible (optional)
                true   // Show icon
        );

        entity.addEffect(stunEffect);
    }

    /**
     * Check if combo should continue based on target's stun status
     *
     * @param victim The target entity
     * @return True if combo can continue (target is stunned)
     */
    public static boolean canContinueCombo(LivingEntity victim) {
        if (victim == null || !victim.isAlive()) {
            return false;
        }

        MobEffectInstance stunEffect = victim.getEffect(NichirinEffectRegistry.stunned());
        return stunEffect != null && stunEffect.getDuration() > 0;
    }

    /**
     * Server tick per player: resets a stale combo (and its bar) after {@link #COMBO_IDLE_RESET_TICKS}
     * with no landed hit. Backstop for when the stun-expiry reset is missed.
     */
    public static void tickPlayer(Player player) {
        if (player.level().isClientSide || !(player instanceof IComboCounter counter)) return;
        if (counter.nichirin$getComboCount() <= 0) return;
        Long last = lastComboHitTick.get(player.getUUID());
        if (last == null) return;
        if (player.level().getGameTime() - last >= COMBO_IDLE_RESET_TICKS) {
            resetCombo(player);
        }
    }

    /**
     * Reset combo for player (called on death, disconnect, etc.)
     *
     * @param player Player to reset
     */
    public static void resetCombo(Player player) {
        if (player instanceof IComboCounter comboCounter) {
            comboCounter.nichirin$resetCombo();

            // Remove from tracking maps
            unregisterPlayerFromAllVictims(player.getUUID());

            playerLastMove.remove(player.getUUID());
            parryStunnedUntil.remove(player.getUUID());
            lastAttackHits.remove(player.getUUID());
            resetStyle(player.getUUID());
            lastComboHitTick.remove(player.getUUID());

            if (player instanceof ServerPlayer serverPlayer) {
                ComboCounterPacket packet = new ComboCounterPacket(0, 0, 0.0f);
                try {
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                    packet.toBytes(buf);
                    NetworkManager.sendToPlayer(serverPlayer,
                            NichirinPacketRegistry.COMBO_COUNTER_ID, NetworkBufferUtils.server(buf, serverPlayer));
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Called when a victim's stun effect ends.
     * This automatically resets combos of all players who were attacking this victim
     *
     * @param victim The entity whose stun ended
     */
    public static void handleStunExpired(LivingEntity victim) {
        if (victim == null || victim.level().isClientSide) {
            return;
        }

        UUID victimUUID = victim.getUUID();
        Set<UUID> attackerUUIDs = victimToAttackers.get(victimUUID);

        if (attackerUUIDs != null && !attackerUUIDs.isEmpty()) {
            for (UUID attackerUUID : new HashSet<>(attackerUUIDs)) {
                Player attacker = victim.level().getPlayerByUUID(attackerUUID);
                if (attacker != null && attacker instanceof IComboCounter comboCounter) {
                    if (comboCounter.nichirin$getLastAttacked() == victim) {

                        comboCounter.nichirin$setComboCount(0);
                        comboCounter.nichirin$setLastAttacked(null);
                        resetStyle(attackerUUID);

                        if (attacker instanceof ServerPlayer serverPlayer) {
                            ComboCounterPacket packet = new ComboCounterPacket(0, 0, 0.0f);
                            try {
                                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                                packet.toBytes(buf);
                                NetworkManager.sendToPlayer(serverPlayer,
                                        NichirinPacketRegistry.COMBO_COUNTER_ID, NetworkBufferUtils.server(buf, serverPlayer));
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
            victimToAttackers.remove(victimUUID);
        }
    }

    /**
     * Get remaining stun duration for an entity
     *
     * @param entity Entity to check
     * @return Remaining stun duration in ticks, 0 if not stunned
     */
    public static int getRemainingStunDuration(LivingEntity entity) {
        if (entity == null) {
            return 0;
        }

        MobEffectInstance stunEffect = entity.getEffect(NichirinEffectRegistry.stunned());
        return stunEffect != null ? stunEffect.getDuration() : 0;
    }

    /**
     * Register an attacker-victim pair for tracking
     */
    private static void registerAttackerVictimPair(UUID attackerUUID, UUID victimUUID) {
        victimToAttackers.computeIfAbsent(victimUUID, k -> new HashSet<>()).add(attackerUUID);
    }

    /**
     * Remove a player from all victim tracking
     */
    private static void unregisterPlayerFromAllVictims(UUID playerUUID) {
        victimToAttackers.values().forEach(attackerSet -> attackerSet.remove(playerUUID));
        victimToAttackers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private static int awardStyleForAttack(Player attacker, LivingEntity victim, int comboCount) {
        UUID playerId = attacker.getUUID();
        AttackStyleContext context = currentAttacks.get(playerId);
        if (context == null || context.awarded) {
            return playerStyleScores.getOrDefault(playerId, 0);
        }

        int gained = 10 + Math.max(0, comboCount - 1) * 3;
        String previousMove = playerLastScoredMove.get(playerId);
        if (!context.moveId.isEmpty() && !context.moveId.equals(previousMove)) {
            gained += 8;
        }
        if (isParryStunned(victim)) {
            gained += 15;
        }

        int score = Math.min(999, playerStyleScores.getOrDefault(playerId, 0) + gained);
        playerStyleScores.put(playerId, score);
        playerLastScoredMove.put(playerId, context.moveId);
        currentAttacks.put(playerId, new AttackStyleContext(context.sequence, context.moveId, true));
        return score;
    }

    private static void resetStyle(UUID playerId) {
        playerStyleScores.remove(playerId);
        playerLastScoredMove.remove(playerId);
        currentAttacks.remove(playerId);
    }

    private static String rankFor(int score) {
        if (score >= 240) return "S";
        if (score >= 160) return "A";
        if (score >= 100) return "B";
        if (score >= 50) return "C";
        return "";
    }

    /**
     * Clean up tracking for dead/invalid entities (call this periodically)
     */
    public static void cleanupTracking() {
        victimToAttackers.entrySet().removeIf(entry -> {
            Set<UUID> attackers = entry.getValue();
            return attackers.isEmpty();
        });
    }
}
