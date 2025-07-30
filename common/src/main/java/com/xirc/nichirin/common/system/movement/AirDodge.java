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
 * Air dodge system with immunity frames and stun on whiff
 * Same mechanics as ground dodge but for aerial movement
 */
public class AirDodge {

    private static final int IMMUNITY_FRAMES = 6; // 6 ticks of immunity
    private static final int STUN_DURATION = 20; // 1 second stun on whiff
    private static final float AIR_DODGE_DISTANCE = 2.0f; // Slightly further than ground dodge
    private static final float AIR_DODGE_VERTICAL_COMPONENT = 0.3f; // Small upward boost

    // Track active air dodges for whiff detection
    private static final Map<UUID, AirDodgeState> activeAirDodges = new HashMap<>();

    /**
     * Execute air dodge
     */
    public static void execute(Player player) {
        if (player == null || player.level().isClientSide) {
            return;
        }

        // Start immunity frames
        grantImmunityFrames(player);

        // Perform air dodge movement
        performAirDodgeMovement(player);

        // Track air dodge for whiff detection
        startAirDodgeTracking(player);

        // Play air dodge sound (slightly different pitch than ground dodge)
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4f, 1.8f);

        System.out.println("DEBUG: Player " + player.getName().getString() + " performed air dodge");
    }

    /**
     * Grants temporary immunity frames using Minecraft's built-in system
     */
    private static void grantImmunityFrames(Player player) {
        // Use Minecraft's built-in invulnerability system
        player.invulnerableTime = IMMUNITY_FRAMES;

        // Optional: Add visual indicator that player is air dodging
        // You could add different particles than ground dodge here

        System.out.println("DEBUG: Granted " + IMMUNITY_FRAMES + " air dodge immunity frames to " + player.getName().getString());
    }

    /**
     * Performs the actual air dodge movement
     */
    private static void performAirDodgeMovement(Player player) {
        // Get player's look direction for dodge direction
        Vec3 lookDirection = player.getLookAngle();

        // For air dodge, use full 3D direction but with limited vertical component
        Vec3 dodgeDirection = new Vec3(
                lookDirection.x,
                Math.max(-0.2, Math.min(0.2, lookDirection.y)), // Limit vertical component
                lookDirection.z
        ).normalize();

        // Apply air dodge velocity
        Vec3 dodgeVelocity = dodgeDirection.scale(AIR_DODGE_DISTANCE);

        // Add small upward component to help with aerial mobility
        dodgeVelocity = dodgeVelocity.add(0, AIR_DODGE_VERTICAL_COMPONENT, 0);

        // Set new velocity (replace current velocity for air dodge)
        player.setDeltaMovement(dodgeVelocity);

        // Reset fall distance to prevent fall damage from the dodge
        player.fallDistance = 0;
    }

    /**
     * Start tracking air dodge for whiff detection
     */
    private static void startAirDodgeTracking(Player player) {
        AirDodgeState airDodgeState = new AirDodgeState();
        airDodgeState.startTime = player.level().getGameTime();
        airDodgeState.dodgedAttack = false;

        activeAirDodges.put(player.getUUID(), airDodgeState);
    }

    /**
     * Check if air dodge was successful or whiffed (called after immunity frames)
     */
    public static void checkAirDodgeResult(Player player) {
        AirDodgeState airDodgeState = activeAirDodges.get(player.getUUID());
        if (airDodgeState == null) return;

        long currentTime = player.level().getGameTime();
        long timeSinceAirDodge = currentTime - airDodgeState.startTime;

        // Only check after immunity frames have ended
        if (timeSinceAirDodge >= IMMUNITY_FRAMES) {
            if (!airDodgeState.dodgedAttack) {
                // Air dodge whiffed - apply stun when player lands
                scheduleStunOnLanding(player);
                System.out.println("DEBUG: Player " + player.getName().getString() + " whiffed air dodge - will apply stun on landing");
            } else {
                System.out.println("DEBUG: Player " + player.getName().getString() + " successfully air dodged an attack");
            }

            // Remove from tracking
            activeAirDodges.remove(player.getUUID());
        }
    }

    /**
     * Mark that this player's air dodge successfully avoided an attack
     * This should be called from your damage/attack system when an attack is dodged
     */
    public static void markAirDodgeSuccessful(Player player) {
        AirDodgeState airDodgeState = activeAirDodges.get(player.getUUID());
        if (airDodgeState != null) {
            airDodgeState.dodgedAttack = true;
        }
    }

    /**
     * Schedule stun to be applied when player lands (for whiffed air dodges)
     */
    private static void scheduleStunOnLanding(Player player) {
        // Mark player for stun on landing
        AirDodgeState airDodgeState = activeAirDodges.get(player.getUUID());
        if (airDodgeState != null) {
            airDodgeState.shouldStunOnLanding = true;
        }
    }

    /**
     * Apply stun effect for whiffed air dodge (called when player lands)
     */
    public static void applyAirDodgeStun(Player player) {
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
     * Check if player is currently in air dodge immunity frames
     */
    public static boolean isPlayerAirDodging(Player player) {
        // Check if player is in invulnerability frames
        if (player.invulnerableTime > 0) {
            // Also check if they're in our air dodge tracking
            AirDodgeState airDodgeState = activeAirDodges.get(player.getUUID());
            if (airDodgeState != null) {
                long currentTime = player.level().getGameTime();
                long timeSinceAirDodge = currentTime - airDodgeState.startTime;
                return timeSinceAirDodge < IMMUNITY_FRAMES;
            }
        }
        return false;
    }

    /**
     * Check if player should be stunned when they land
     * This should be called from a player landing event
     */
    public static void checkStunOnLanding(Player player) {
        AirDodgeState airDodgeState = activeAirDodges.get(player.getUUID());
        if (airDodgeState != null && airDodgeState.shouldStunOnLanding) {
            if (player.onGround()) {
                applyAirDodgeStun(player);
                activeAirDodges.remove(player.getUUID());
            }
        }
    }

    /**
     * Tick method to handle ongoing air dodge checks
     * This should be called from your mod's tick handler
     */
    public static void tick() {
        // Check all active air dodges and clean up expired ones
        activeAirDodges.entrySet().removeIf(entry -> {
            // This would need proper player lookup in a real implementation
            // For now, just clean up old entries
            return false; // You'll need to implement cleanup logic
        });
    }

    /**
     * Internal class to track air dodge state
     */
    private static class AirDodgeState {
        long startTime;
        boolean dodgedAttack;
        boolean shouldStunOnLanding;
    }
}