package com.xirc.nichirin.common.system.movement;

import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.util.StaminaManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.network.chat.Component;
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

    private static final int IMMUNITY_FRAMES = 20; // 1 second of immunity (20 ticks)
    private static final int STUN_DURATION = 20; // 1 second stun on whiff
    private static final float GROUND_DODGE_DISTANCE = 1.5f; //lmao this is so pointless
    private static final float AIR_DODGE_DISTANCE = 0.7f;

    // Track active dodges for whiff detection
    private static final Map<UUID, DodgeState> activeDodges = new HashMap<>();

    /**
     * Execute ground dodge
     */
    public static void executeGroundDodge(Player player) {
        if (player == null || player.level().isClientSide) {
            return;
        }
        if (isAnchored(player)) {
            notifyAnchored(player);
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
     * Execute air dodge with directional input (teleport-like dash)
     */
    public static void executeAirDodge(Player player, MovementContext.DashInput input) {
        if (player == null || player.level().isClientSide) {
            return;
        }
        if (isAnchored(player)) {
            notifyAnchored(player);
            return;
        }

        // Start immunity frames
        grantImmunityFrames(player);

        // Perform air dodge movement with input
        performAirDodgeMovement(player, input);

        // Track dodge for whiff detection
        startDodgeTracking(player, DodgeType.AIR);

        // Play air dodge sound (higher pitch)
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.4f, 1.8f);
    }

    /**
     * Execute air dodge without input (for compatibility)
     */
    public static void executeAirDodge(Player player) {
        // Create neutral input (no direction)
        MovementContext.DashInput neutralInput = new MovementContext.DashInput();
        executeAirDodge(player, neutralInput);
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
     * Performs air dodge movement with directional input (teleport-like)
     */
    private static void performAirDodgeMovement(Player player, MovementContext.DashInput input) {
        // Calculate air dodge direction based on WASD input
        Vec3 dodgeDirection = calculateAirDodgeDirection(player, input);

        if (dodgeDirection.equals(Vec3.ZERO)) {
            // Neutral air dodge - no movement, just immunity
            return;
        }

        // Moderate movement (not too far)
        Vec3 airDodgeVelocity = dodgeDirection.scale(AIR_DODGE_DISTANCE);

        // Replace current velocity completely for instant effect
        player.setDeltaMovement(airDodgeVelocity);
        player.fallDistance = 0; // Prevent fall damage
        player.hurtMarked = true; // Force client sync
    }

    /**
     * Calculate air dodge direction based on WASD input
     */
    private static Vec3 calculateAirDodgeDirection(Player player, MovementContext.DashInput input) {
        Vec3 lookDirection = player.getLookAngle();
        Vec3 rightDirection = lookDirection.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 forwardDirection = new Vec3(lookDirection.x, 0, lookDirection.z).normalize();

        double forward = 0;
        double side = 0;

        // WASD input - NO vertical component
        if (input.forward) forward += 1;
        if (input.backward) forward -= 1;
        if (input.right) side += 1;
        if (input.left) side -= 1;

        // Only horizontal movement - no up/down
        Vec3 dodgeDirection = forwardDirection.scale(forward).add(rightDirection.scale(side));

        if (dodgeDirection.length() > 0) {
            dodgeDirection = dodgeDirection.normalize();
        }

        return dodgeDirection;
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

            // Show "Move Dodged!" message
            player.displayClientMessage(
                    Component.literal("Move Dodged!")
                            .withStyle(style -> style.withColor(0x00FF00).withBold(true)),
                    true
            );
        }
    }

    /**
     * Apply stun effect for whiffed dodge
     */
    private static void applyDodgeStun(Player player) {
        MobEffectInstance stunEffect = new MobEffectInstance(
                NichirinEffectRegistry.stunned(),
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
     * Simple tick method for each player - call this from your PlayerTickHandler
     */
    public static void tickForPlayer(Player player) {
        if (player.level().isClientSide) return;

        DodgeState dodgeState = activeDodges.get(player.getUUID());
        if (dodgeState == null) return;

        long currentTime = player.level().getGameTime();
        long timeSinceDodge = currentTime - dodgeState.startTime;

        // Check if immunity frames just expired
        if (timeSinceDodge == IMMUNITY_FRAMES) {
            player.displayClientMessage(
                    Component.literal("Immunity Frames Removed!")
                            .withStyle(style -> style.withColor(0xFF6600)),
                    true
            );
        }
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

    /** Anchored flaw: cannot dodge while stamina > 50%. */
    private static boolean isAnchored(Player player) {
        if (!PlayerDataProvider.getData(player).getPerkData().hasFlaw("anchored")) {
            return false;
        }
        return StaminaManager.getStaminaPercentage(player) > 0.5f;
    }

    private static void notifyAnchored(Player player) {
        player.displayClientMessage(
                Component.literal("Anchored — too rested to dodge")
                        .withStyle(s -> s.withColor(0xFF5555)),
                true);
    }
}