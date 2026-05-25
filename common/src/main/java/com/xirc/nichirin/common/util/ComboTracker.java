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
 * Now includes anti-spam detection for repeated moves
 */
public class ComboTracker {

    // Track which players are combo-ing which entities
    // Map: Victim UUID -> Set of Player UUIDs who are combo-ing this victim
    private static final Map<UUID, Set<UUID>> victimToAttackers = new HashMap<>();

    // Track last move used by each player for anti-spam detection
    // Map: Player UUID -> Last move ID used
    private static final Map<UUID, String> playerLastMove = new HashMap<>();

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

        if (moveId != null && moveId.equals(lastMove)) {
            return 0;
        }

        // Update last move
        playerLastMove.put(player.getUUID(), moveId);
        return originalHitStun; // Normal hitstun for non-repeated moves
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

        if (lastAttacked != victim) {
            // New target - reset combo to 1
            comboCounter.nichirin$setComboCount(1);
            comboCounter.nichirin$setLastAttacked(victim);
        } else {
            // Same target - check if they were already stunned from previous hit AND if this hit actually applies stun
            if (wasAlreadyStunned && stunDurationTicks > 0) {
                // Target was still stunned AND this hit applies stun - increment combo
                comboCounter.nichirin$incrementComboCount();
            } else {
                // Target broke free from stun OR this hit has no stun (spam detection) - reset combo
                comboCounter.nichirin$setComboCount(1);
            }

            // Update last attacked regardless
            comboCounter.nichirin$setLastAttacked(victim);
        }

        // Track this attacker-victim relationship for automatic reset
        registerAttackerVictimPair(attacker.getUUID(), victim.getUUID());

        // Send combo update to client using packet registry
        int comboCount = comboCounter.nichirin$getComboCount();
        ComboCounterPacket packet = new ComboCounterPacket(comboCount, stunDurationTicks, damage);

        // Use the registry's sendToPlayer method which handles encoding
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            packet.toBytes(buf);
            NetworkManager.sendToPlayer(serverPlayer,
                    NichirinPacketRegistry.COMBO_COUNTER_ID, buf);
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

        // Apply STUNNED effect
        MobEffectInstance stunEffect = new MobEffectInstance(
                NichirinEffectRegistry.STUNNED.get(),
                durationTicks,
                0, // Amplifier 0
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

        MobEffectInstance stunEffect = victim.getEffect(NichirinEffectRegistry.STUNNED.get());
        return stunEffect != null && stunEffect.getDuration() > 0;
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

            // Clear spam tracking
            playerLastMove.remove(player.getUUID());

            // Send reset to client if server player
            if (player instanceof ServerPlayer serverPlayer) {
                ComboCounterPacket packet = new ComboCounterPacket(0, 0, 0.0f);
                try {
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                    packet.toBytes(buf);
                    NetworkManager.sendToPlayer(serverPlayer,
                            NichirinPacketRegistry.COMBO_COUNTER_ID, buf);
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * NEW METHOD: Called when a victim's stun effect ends
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
            // Reset combo for all players who were combo-ing this victim
            for (UUID attackerUUID : new HashSet<>(attackerUUIDs)) { // Copy to avoid concurrent modification
                Player attacker = victim.level().getPlayerByUUID(attackerUUID);
                if (attacker != null && attacker instanceof IComboCounter comboCounter) {
                    // Only reset if this victim is their current target
                    if (comboCounter.nichirin$getLastAttacked() == victim) {  

                        // Reset combo to 0
                        comboCounter.nichirin$setComboCount(0);
                        comboCounter.nichirin$setLastAttacked(null);

                        // Send reset packet to client
                        if (attacker instanceof ServerPlayer serverPlayer) {
                            ComboCounterPacket packet = new ComboCounterPacket(0, 0, 0.0f);
                            try {
                                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                                packet.toBytes(buf);
                                NetworkManager.sendToPlayer(serverPlayer,
                                        NichirinPacketRegistry.COMBO_COUNTER_ID, buf);
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }

            // Clean up tracking for this victim
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

        MobEffectInstance stunEffect = entity.getEffect(NichirinEffectRegistry.STUNNED.get());
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
        // Clean up empty sets
        victimToAttackers.entrySet().removeIf(entry -> entry.getValue().isEmpty());
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
