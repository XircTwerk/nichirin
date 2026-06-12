package com.xirc.nichirin.common.util;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.common.data.MovesetHelper;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Integration helper for adding combo tracking to existing attack systems
 * This provides easy hooks for your katana attacks and breathing moves
 */
public class ComboIntegration {

    /**
     * Call this ONLY after a successful hit that dealt damage
     * Handles both stun application and combo tracking with damage
     *
     * @param attacker The player performing the attack
     * @param victim The entity being hit
     * @param hitStunTicks Duration of stun to apply (from moveset config or default)
     * @param damage Damage dealt by this attack
     */
    public static void handleSuccessfulHit(Player attacker, LivingEntity victim, int hitStunTicks, float damage) {
        if (attacker == null || victim == null || attacker.level().isClientSide) {
            return; // Server-side only
        }

        // Prefer the measured post-armor damage from this tick's hurt() call so the combo bar
        // reflects what the victim actually lost, not the attack's raw damage stat.
        float realDamage = NichirinArmorDamage.actualDamageOr(victim, damage);

        // Check for existing stun BEFORE applying new stun
        boolean wasAlreadyStunned = ComboTracker.canContinueCombo(victim);

        // Apply new stun effect
        ComboTracker.applyStun(victim, hitStunTicks);

        // Report the stun that actually landed (immune/already-stunned targets may differ).
        MobEffectInstance applied = victim.getEffect(NichirinEffectRegistry.stunned());
        int realStun = applied != null ? applied.getDuration() : 0;

        // Handle combo tracking with the previous stun status
        ComboTracker.handleHit(attacker, victim, realStun, realDamage, wasAlreadyStunned);
    }

    /**
     * Backward compatibility - without damage tracking
     */
    public static void handleSuccessfulHit(Player attacker, LivingEntity victim, int hitStunTicks) {
        handleSuccessfulHit(attacker, victim, hitStunTicks, 0.0f);
    }

    /**
     * For katana basic attacks - use default stun duration with damage
     * ONLY CALL THIS WHEN AN ATTACK ACTUALLY HITS A TARGET
     *
     * @param attacker Player performing katana attack
     * @param victim Target entity that was actually hit
     * @param baseStunTicks Base stun duration for this attack type
     * @param damage Damage dealt
     */
    public static void handleKatanaHit(Player attacker, LivingEntity victim, int baseStunTicks, float damage) {
        handleSuccessfulHit(attacker, victim, baseStunTicks, damage);
    }

    /**
     * Backward compatibility for katana hits without damage
     */
    public static void handleKatanaHit(Player attacker, LivingEntity victim, int baseStunTicks) {
        handleKatanaHit(attacker, victim, baseStunTicks, 0.0f);
    }

    /**
     * For breathing moves - extract stun duration from moveset configuration with damage
     *
     * @param attacker Player performing breathing move
     * @param victim Target entity
     * @param moveIndex Index of the move in the moveset
     * @param damage Damage dealt by the move
     */
    public static void handleBreathingMoveHit(Player attacker, LivingEntity victim, int moveIndex, float damage) {
        // Get the moveset and move config
        AbstractMoveset moveset = MovesetHelper.getMoveset(attacker);
        if (moveset == null) {
            return;
        }

        AbstractMoveset.MoveConfiguration config = moveset.getMove(moveIndex);
        if (config == null) {
            return;
        }

        // Get hit stun from config, default to 20 ticks if not specified
        int hitStunTicks = config.getHitStunOrDefault(20);
        handleSuccessfulHit(attacker, victim, hitStunTicks, damage);
    }

    /**
     * Backward compatibility for breathing moves without damage
     */
    public static void handleBreathingMoveHit(Player attacker, LivingEntity victim, int moveIndex) {
        handleBreathingMoveHit(attacker, victim, moveIndex, 0.0f);
    }

    /**
     * For breathing moves with explicit config and damage
     *
     * @param attacker Player performing breathing move
     * @param victim Target entity
     * @param config Move configuration containing stun duration
     * @param damage Damage dealt by the move
     */
    public static void handleBreathingMoveHit(Player attacker, LivingEntity victim, AbstractMoveset.MoveConfiguration config, float damage) {
        if (config == null) {
            return;
        }

        int hitStunTicks = config.getHitStunOrDefault(20);
        handleSuccessfulHit(attacker, victim, hitStunTicks, damage);
    }

    /**
     * Backward compatibility for breathing moves with config but no damage
     */
    public static void handleBreathingMoveHit(Player attacker, LivingEntity victim, AbstractMoveset.MoveConfiguration config) {
        handleBreathingMoveHit(attacker, victim, config, 0.0f);
    }

    /**
     * For custom attacks with specific stun values and damage
     *
     * @param attacker Player performing attack
     * @param victim Target entity
     * @param attackName Name of the attack (for debugging)
     * @param hitStunTicks Specific stun duration for this attack
     * @param damage Damage dealt
     */
    public static void handleCustomAttackHit(Player attacker, LivingEntity victim, String attackName, int hitStunTicks, float damage) {
        handleSuccessfulHit(attacker, victim, hitStunTicks, damage);
    }

    /**
     * Backward compatibility for custom attacks without damage
     */
    public static void handleCustomAttackHit(Player attacker, LivingEntity victim, String attackName, int hitStunTicks) {
        handleCustomAttackHit(attacker, victim, attackName, hitStunTicks, 0.0f);
    }

    /**
     * Check if a hit should register for combo purposes
     * Call this before applying damage to see if the combo should continue
     *
     * @param attacker Player performing attack
     * @param victim Target entity
     * @return True if this hit will continue/start a combo
     */
    public static boolean shouldRegisterHit(Player attacker, LivingEntity victim) {
        if (attacker == null || victim == null || !victim.isAlive()) {
            return false;
        }

        // Always register hits - combo logic handles target switching internally
        return true;
    }

    /**
     * Get current combo count for a player
     *
     * @param player Player to check
     * @return Current combo count, 0 if no combo
     */
    public static int getCurrentCombo(Player player) {
        if (player instanceof IComboCounter comboCounter) {
            return comboCounter.nichirin$getComboCount();
        }
        return 0;
    }

    /**
     * Check if player is currently in a combo
     *
     * @param player Player to check
     * @return True if player has an active combo (count > 1)
     */
    public static boolean isInCombo(Player player) {
        return getCurrentCombo(player) > 1;
    }

    /**
     * Reset combo for a player (for cleanup, death, etc.)
     *
     * @param player Player to reset
     */
    public static void resetPlayerCombo(Player player) {
        ComboTracker.resetCombo(player);
    }

    /**
     * Get the entity a player is currently comboing
     *
     * @param player Player to check
     * @return The entity being comboed, or null if no active combo target
     */
    public static LivingEntity getComboTarget(Player player) {
        if (player instanceof IComboCounter comboCounter) {
            LivingEntity target = comboCounter.nichirin$getLastAttacked();
            if (target != null && ComboTracker.canContinueCombo(target)) {
                return target;
            }
        }
        return null;
    }
}