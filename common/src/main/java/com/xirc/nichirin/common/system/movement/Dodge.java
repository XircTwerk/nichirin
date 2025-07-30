package com.xirc.nichirin.common.system.movement;

import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ground dodge system with immunity frames and stun on whiff
 */
public class Dodge {

    private static final int IMMUNITY_FRAMES = 6; // 6 ticks of immunity
    private static final int STUN_DURATION = 20; // 1 second stun on whiff
    private static final float DODGE_DISTANCE = 1.5f; // How far to dodge

    // Track active dodges for whiff detection
    private static final Map<UUID, DodgeState> activeDodges = new HashMap<>();

    /**
     * Execute ground dodge
     */
    public static void execute(Player player) {
        if (player == null || player.level().isClientSide) {
            return;
        }

        // Start immunity frames
        grantImmunityFrames(player);

        // Perform dodge movement (small movement in look direction)
        performDodgeMovement(player);

        // Track dodge for whiff detection
        startDodgeTracking(player);

        // Play dodge sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.3f, 1.5f);

        System.out.println("DEBUG: Player " + player.getName().getString() + " performed ground dodge");
    }

    /**
     * Grants temporary immunity frames using Minecraft's built-in system
     */
    private static void grantImmunityFrames(Player player) {
        // Use Minecraft's built-in invulnerability system
        player.invulnerableTime = IMMUNITY_FRAMES;

        // Optional: Add visual indicator that player is dodging
        // You could add particles, glowing effect, etc. here

        System.out.println("DEBUG: Granted " + IMMUNITY_FRAMES + " immunity frames to " + player.getName().getString());
    }

    /**
     * Performs the actual dodge movement
     */
    private static void performDodgeMovement(Player player) {
        // Get player's look direction for dodge direction
        Vec3 lookDirection = player.getLookAngle();
        Vec3 dodgeDirection = new Vec3(lookDirection.x, 0, lookDirection.z).normalize();

        // Apply dodge velocity
        Vec3 dodgeVelocity = dodgeDirection.scale(DODGE_DISTANCE);

        // Add to current velocity (don't replace, add to existing movement)
        Vec3 currentVelocity = player.getDeltaMovement();
        Vec3 newVelocity = new Vec3(
                currentVelocity.x + dodgeVelocity.x,
                currentVelocity.y, // Keep Y velocity unchanged
                currentVelocity.z + dodgeVelocity.z
        );

        player.setDeltaMovement(newVelocity);
    }

    /**
     * Start tracking dodge for whiff detection
     */
    private static void startDodgeTracking(Player player) {
        DodgeState dodgeState = new DodgeState();
        dodgeState.startTime = player.level().getGameTime();
        dodgeState.dodgedAttack = false;

        activeDodges.put(player.getUUID(), dodgeState);
    }

    /**
     * Check if dodge was successful or whiffed (called after immunity frames)
     * This should be called from your tick handler
     */
    public static void checkDodgeResult(Player player) {
        DodgeState dodgeState = activeDodges.get(player.getUUID());
        if (dodgeState == null) return;

        long currentTime = player.level().getGameTime();
        long timeSinceDodge = currentTime - dodgeState.startTime;

        // Only check after immunity frames have ended
        if (timeSinceDodge >= IMMUNITY_FRAMES) {
            if (!dodgeState.dodgedAttack) {
                // Dodge whiffed - apply stun
                applyDodgeStun(player);
                System.out.println("DEBUG: Player " + player.getName().getString() + " whiffed dodge - applying stun");
            } else {
                System.out.println("DEBUG: Player " + player.getName().getString() + " successfully dodged an attack");
            }

            // Remove from tracking
            activeDodges.remove(player.getUUID());
        }
    }

    /**
     * Mark that this player's dodge successfully avoided an attack
     * This should be called from your damage/attack system when an attack is dodged
     */
    public static void markDodgeSuccessful(Player player) {
        DodgeState dodgeState = activeDodges.get(player.getUUID());
        if (dodgeState != null) {
            dodgeState.dodgedAttack = true;
        }
    }

    /**
     * Apply stun effect for whiffed dodge
     */
    private static void applyDodgeStun(Player player) {
        // Apply stun effect
        MobEffectInstance stunEffect = new MobEffectInstance(
                NichirinEffectRegistry.STUNNED.get(),
                STUN_DURATION,
                0, // Amplifier
                false, // Ambient
                true,  // Visible
                true   // Show icon
        );

        player.addEffect(stunEffect);

        // Play stun sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3f, 0.8f);
    }

    /**
     * Check if player is currently in dodge immunity frames
     */
    public static boolean isPlayerDodging(Player player) {
        // Check if player is in invulnerability frames
        if (player.invulnerableTime > 0) {
            // Also check if they're in our dodge tracking (to distinguish from other invulnerability)
            DodgeState dodgeState = activeDodges.get(player.getUUID());
            if (dodgeState != null) {
                long currentTime = player.level().getGameTime();
                long timeSinceDodge = currentTime - dodgeState.startTime;
                return timeSinceDodge < IMMUNITY_FRAMES;
            }
        }
        return false;
    }

    /**
     * Tick method to handle ongoing dodge checks
     * This should be called from your mod's tick handler
     */
    public static void tick() {
        // Check all active dodges for whiff detection
        activeDodges.entrySet().removeIf(entry -> {
            // This would need proper player lookup in a real implementation
            // For now, just clean up old entries
            return false; // You'll need to implement cleanup logic
        });
    }

    /**
     * Internal class to track dodge state
     */
    private static class DodgeState {
        long startTime;
        boolean dodgedAttack;
    }
}