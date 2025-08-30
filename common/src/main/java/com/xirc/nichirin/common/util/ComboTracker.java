package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.network.ComboCounterPacket;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Central combo tracking logic
 */
public class ComboTracker {

    /**
     * Handle combo logic when a player hits an entity
     * Should be called after successful hit and stun application
     *
     * @param attacker The player performing the attack
     * @param victim The entity being attacked
     * @param stunDurationTicks Duration of stun applied to victim
     * @param damage Damage dealt by this hit
     */
    public static void handleHit(Player attacker, LivingEntity victim, int stunDurationTicks, float damage) {
        if (!(attacker instanceof ServerPlayer serverPlayer)) {
            return; // Server-side only
        }

        if (victim == null || !victim.isAlive()) {
            return;
        }

        // Safety check - make sure mixin is applied
        if (!(attacker instanceof IComboCounter)) {
            System.err.println("ERROR: PlayerMixin not applied! Player cannot be cast to IComboCounter");
            System.err.println("Make sure PlayerMixin is registered in your mixins.nichirin.json file");
            return;
        }

        IComboCounter comboCounter = (IComboCounter) attacker;
        LivingEntity lastAttacked = comboCounter.nichirin$getLastAttacked();

        if (lastAttacked != victim) {
            // New target - reset combo to 1
            comboCounter.nichirin$setComboCount(1);
            comboCounter.nichirin$setLastAttacked(victim);
        } else {
            // Same target - check if they're still stunned from previous hit
            MobEffectInstance stunEffect = victim.getEffect(NichirinEffectRegistry.STUNNED.get());

            if (stunEffect != null && stunEffect.getDuration() > 0) {
                // Target is still stunned - increment combo
                comboCounter.nichirin$incrementComboCount();
            } else {
                // Target broke free from stun - reset combo
                comboCounter.nichirin$setComboCount(1);
            }

            // Update last attacked regardless
            comboCounter.nichirin$setLastAttacked(victim);
        }

        // Send combo update to client using packet registry
        int comboCount = comboCounter.nichirin$getComboCount();
        ComboCounterPacket packet = new ComboCounterPacket(comboCount, stunDurationTicks, damage);

        // Use the registry's sendToPlayer method which handles encoding
        try {
            net.minecraft.network.FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            packet.toBytes(buf);
            dev.architectury.networking.NetworkManager.sendToPlayer(serverPlayer,
                    com.xirc.nichirin.registry.NichirinPacketRegistry.COMBO_COUNTER_ID, buf);
        } catch (Exception e) {
            System.err.println("Failed to send combo counter packet: " + e.getMessage());
        }
    }

    /**
     * Overloaded method for backward compatibility (no damage tracking)
     */
    public static void handleHit(Player attacker, LivingEntity victim, int stunDurationTicks) {
        handleHit(attacker, victim, stunDurationTicks, 0.0f);
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

            // Send reset to client if server player
            if (player instanceof ServerPlayer serverPlayer) {
                ComboCounterPacket packet = new ComboCounterPacket(0, 0, 0.0f);
                try {
                    net.minecraft.network.FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                    packet.toBytes(buf);
                    dev.architectury.networking.NetworkManager.sendToPlayer(serverPlayer,
                            com.xirc.nichirin.registry.NichirinPacketRegistry.COMBO_COUNTER_ID, buf);
                } catch (Exception e) {
                    System.err.println("Failed to send combo reset packet: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Called when a victim's stun effect ends
     * Should reset combos of all players who were attacking this victim
     *
     * @param victim The entity whose stun ended
     */
    public static void handleStunExpired(LivingEntity victim) {
        if (victim == null || victim.level().isClientSide) {
            return;
        }

        // Find all players who were combo-ing this victim and reset them
        victim.level().players().forEach(player -> {
            if (player instanceof IComboCounter comboCounter) {
                if (comboCounter.nichirin$getLastAttacked() == victim) {
                    resetCombo(player);
                }
            }
        });
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
}