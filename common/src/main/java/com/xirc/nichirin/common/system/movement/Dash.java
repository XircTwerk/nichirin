package com.xirc.nichirin.common.system.movement;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * Dash system with directional input support - prevents jump launching
 */
public class Dash {

    private static final float DASH_FORCE = 2.35f;
    private static final int DASH_DURATION = 14; // ticks
    private static final float PARTICLE_HEIGHT_OFFSET = 1.0f; // Move particles up

    // Track active dashes with player references instead of UUIDs
    private static final Map<Player, DashState> activeDashes = new HashMap<>();

    /**
     * Execute dash with input direction
     */
    public static void execute(Player player, MovementContext.DashInput input) {
        if (player == null || player.level().isClientSide) {
            return;
        }

        // Start dash — direction is recomputed each tick from live look (see tickAllDashes),
        // so the input snapshot is what gets stored, not a baked direction.
        startDash(player, input);

        // Play dash sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f, 1.3f);

        // Add initial particles at player position (moved up)
        addDashParticles(player, player.position().add(0, PARTICLE_HEIGHT_OFFSET, 0));
    }

    /**
     * Calculate dash direction based on input
     */
    private static Vec3 calculateDashDirection(Player player, MovementContext.DashInput input) {
        Vec3 lookDirection = player.getLookAngle();
        Vec3 rightDirection = lookDirection.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 forwardDirection = new Vec3(lookDirection.x, 0, lookDirection.z).normalize();

        double forward = 0;
        double side = 0;

        if (input.forward) forward += 1;
        if (input.backward) forward -= 1;
        if (input.right) side += 1;
        if (input.left) side -= 1;

        // Combine directions
        Vec3 dashDirection = forwardDirection.scale(forward).add(rightDirection.scale(side));

        if (dashDirection.length() > 0) {
            dashDirection = dashDirection.normalize();
        }

        return dashDirection;
    }

    /**
     * Start a new dash. Stores the input snapshot — the actual world-space direction is
     * recomputed each tick in {@link #tickAllDashes()} from the live look angle, so the
     * player can STEER the dash by turning the camera.
     */
    private static void startDash(Player player, MovementContext.DashInput input) {
        DashState dashState = new DashState();
        dashState.input = input;
        dashState.remainingTicks = DASH_DURATION;
        dashState.force = DASH_FORCE;

        activeDashes.put(player, dashState);
    }

    /**
     * Tick all active dashes
     */
    public static void tickAllDashes() {
        activeDashes.entrySet().removeIf(entry -> {
            Player player = entry.getKey();
            DashState dashState = entry.getValue();

            // Check if player is still valid
            if (player == null || player.level().isClientSide) {
                return true; // Remove if player not valid
            }

            // REDUCED AIR ACCELERATION: Much gentler when in air
            Vec3 currentVelocity = player.getDeltaMovement();

            // Recompute direction LIVE from the current look angle so the player can steer
            // mid-dash. Falls back to pure look-forward if the original input was directionless.
            Vec3 liveDirection = calculateDashDirection(player, dashState.input);
            if (liveDirection.equals(Vec3.ZERO)) {
                liveDirection = player.getLookAngle().normalize();
                liveDirection = new Vec3(liveDirection.x, 0, liveDirection.z).normalize();
            }

            double forceMultiplier = player.onGround() ? 0.1 : 0.05; // Half acceleration in air
            Vec3 dashVelocity = liveDirection.scale(dashState.force * forceMultiplier);

            // Always additive - preserves natural momentum
            Vec3 newVelocity = new Vec3(
                    currentVelocity.x + dashVelocity.x,
                    currentVelocity.y, // Never modify Y to prevent jump launching
                    currentVelocity.z + dashVelocity.z
            );

            player.setDeltaMovement(newVelocity);
            player.hurtMarked = true;

            // Add particles during dash (moved up)
            if (dashState.remainingTicks % 2 == 0) { // Every other tick
                addDashParticles(player, player.position().add(0, PARTICLE_HEIGHT_OFFSET, 0));
            }

            dashState.remainingTicks--;

            return dashState.remainingTicks <= 0;
        });
    }

    /**
     * Add dash particles
     */
    private static void addDashParticles(Player player, Vec3 position) {
        if (player.level().isClientSide) return;

        // Use regular minecraft wind particles
        if (player.level() instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 5; i++) {
                double offsetX = (player.getRandom().nextDouble() - 0.5) * 0.8;
                double offsetY = (player.getRandom().nextDouble() - 0.5) * 0.8;
                double offsetZ = (player.getRandom().nextDouble() - 0.5) * 0.8;

                serverLevel.sendParticles(
                        ParticleTypes.CLOUD,
                        position.x + offsetX,
                        position.y + offsetY,
                        position.z + offsetZ,
                        1, 0, 0, 0, 0.05
                );
            }
        }
    }

    /**
     * Check if player is currently dashing
     */
    public static boolean isPlayerDashing(Player player) {
        return activeDashes.containsKey(player);
    }

    /**
     * Get remaining dash ticks for player
     */
    public static int getRemainingDashTicks(Player player) {
        DashState dashState = activeDashes.get(player);
        return dashState != null ? dashState.remainingTicks : 0;
    }

    /**
     * Internal dash state tracking. {@code input} is the snapshot of WASD-relative direction
     * the player held when the dash started; the look angle is read live per tick.
     */
    private static class DashState {
        MovementContext.DashInput input;
        int remainingTicks;
        float force;
    }
}
