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
 * Unified dodge system handling both ground and air dodges
 */
public class Dodge {

    private static final int IMMUNITY_FRAMES = 6; // 6 ticks of immunity
    private static final int STUN_DURATION = 20; // 1 second stun on whiff
    private static final float GROUND_DODGE_DISTANCE = 1.5f;
    private static final float AIR_DODGE_DISTANCE = 2.0f;
    private static final float AIR_DODGE_VERTICAL_COMPONENT = 0.3f;

    // Track active dodges for whiff detection
    private static final Map<UUID, DodgeState> activeDodges = new HashMap<>();

    /**
     * Execute ground dodge
     */
    public static void executeGroundDodge(Player player) {
        if (player == null || player.level().isClientSide) {
            return;
        }

        // Start immunity frames
        grantImmunityFrames(player);

        // Perform ground dodge movement
        performGroundDodgeMovement(player);

        // Track dodge for whiff detection
        startDodgeTracking(player, DodgeType.GROUND);

        // Play dodge sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.3f, 1.5f);
    }

    /**
     * Execute air dodge
     */
    public static void executeAirDodge(Player player) {
        if (player == null || player.level().isClientSide) {
            return;
        }

        // Start immunity frames
        grantImmunityFrames(player);

        // Perform air dodge movement
        performAirDodgeMovement(player);

        // Track dodge for whiff detection
        startDodgeTracking(player, DodgeType.AIR);

        // Play air dodge sound (higher pitch)
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4f, 1.8f);
    }

    /**
     * Legacy method for ground dodge (for compatibility)
     */
    public static void execute(Player player) {
        executeGroundDodge(player);
    }

    /**
     * Grants temporary immunity frames using Minecraft's built-in system
     */
    private static void grantImmunityFrames(Player player) {
        player.invulnerableTime = IMMUNITY_FRAMES;
    }

    /**
     * Performs ground dodge movement
     */
    private static void performGroundDodgeMovement(Player player) {
        Vec3 lookDirection = player.getLookAngle();
        Vec3 dodgeDirection = new Vec3(lookDirection.x, 0, lookDirection.z).normalize();
        Vec3 dodgeVelocity = dodgeDirection.scale(GROUND_DODGE_DISTANCE);

        // Add to current velocity
        Vec3 currentVelocity = player.getDeltaMovement();
        Vec3 newVelocity = new Vec3(
                currentVelocity.x + dodgeVelocity.x,
                currentVelocity.y,
                currentVelocity.z + dodgeVelocity.z
        );

        player.setDeltaMovement(newVelocity);
    }

    /**
     * Performs air dodge movement
     */
    private static void performAirDodgeMovement(Player player) {
        Vec3 lookDirection = player.getLookAngle();

        // For air dodge, use 3D direction but limit vertical component
        Vec3 dodgeDirection = new Vec3(
                lookDirection.x,
                Math.max(-0.2, Math.min(0.2, lookDirection.y)),
                lookDirection.z
        ).normalize();

        Vec3 dodgeVelocity = dodgeDirection.scale(AIR_DODGE_DISTANCE);
        dodgeVelocity = dodgeVelocity.add(0, AIR_DODGE_VERTICAL_COMPONENT, 0);

        // Replace current velocity for air dodge
        player.setDeltaMovement(dodgeVelocity);
        player.fallDistance = 0; // Prevent fall damage
    }

    /**
     * Start tracking dodge for whiff detection
     */
    private static void startDodgeTracking(Player player, DodgeType type) {
        DodgeState dodgeState = new DodgeState();
        dodgeState.startTime = player.level().getGameTime();
        dodgeState.dodgedAttack = false;
        dodgeState.type = type;

        activeDodges.put(player.getUUID(), dodgeState);
    }

    /**
     * Check if dodge was successful or whiffed
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
                if (dodgeState.type == DodgeType.AIR && !player.onGround()) {
                    // Air dodge - schedule stun for when player lands
                    dodgeState.shouldStunOnLanding = true;
                } else if (dodgeState.type == DodgeType.GROUND || player.onGround()) {
                    // Ground dodge or air dodge that landed - apply stun immediately
                    applyDodgeStun(player);
                    activeDodges.remove(player.getUUID());
                }
            } else {
                // Successful dodge
                activeDodges.remove(player.getUUID());
            }
        }
    }

    /**
     * Check if player should be stunned when they land (for whiffed air dodges)
     */
    public static void checkStunOnLanding(Player player) {
        DodgeState dodgeState = activeDodges.get(player.getUUID());
        if (dodgeState != null && dodgeState.shouldStunOnLanding && player.onGround()) {
            applyDodgeStun(player);
            activeDodges.remove(player.getUUID());
        }
    }

    /**
     * Mark that this player's dodge successfully avoided an attack
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
        MobEffectInstance stunEffect = new MobEffectInstance(
                NichirinEffectRegistry.STUNNED.get(),
                STUN_DURATION,
                0,
                false,
                true,
                true
        );

        player.addEffect(stunEffect);

        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.3f, 0.8f);
    }

    /**
     * Check if player is currently dodging
     */
    public static boolean isPlayerDodging(Player player) {
        if (player.invulnerableTime > 0) {
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
     * Check if player is air dodging specifically
     */
    public static boolean isPlayerAirDodging(Player player) {
        DodgeState dodgeState = activeDodges.get(player.getUUID());
        if (dodgeState != null && dodgeState.type == DodgeType.AIR) {
            long currentTime = player.level().getGameTime();
            long timeSinceDodge = currentTime - dodgeState.startTime;
            return timeSinceDodge < IMMUNITY_FRAMES;
        }
        return false;
    }

    /**
     * Tick method to handle ongoing dodge checks
     */
    public static void tick() {
        activeDodges.entrySet().removeIf(entry -> {
            DodgeState state = entry.getValue();
            // Simple cleanup - remove very old entries
            long age = System.currentTimeMillis() - state.startTime;
            return age > 5000; // Remove after 5 seconds
        });
    }

    /**
     * Dodge type enum
     */
    private enum DodgeType {
        GROUND,
        AIR
    }

    /**
     * Internal class to track dodge state
     */
    private static class DodgeState {
        long startTime;
        boolean dodgedAttack;
        boolean shouldStunOnLanding;
        DodgeType type;
    }
}