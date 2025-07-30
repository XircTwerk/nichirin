package com.xirc.nichirin.common.system.movement;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Dash system with global tracking
 */
public class Dash {

    // Global dash map for all active dashes
    public static final Map<LivingEntity, DashData> activeDashes = new WeakHashMap<>();

    private static final double DASH_SPEED = 0.75; // Base dash speed

    /**
     * Try to start a dash
     */
    public static void execute(Player player, MovementContext.DashInput input) {
        if (player == null || player.level().isClientSide) {
            return;
        }

        // Check if already dashing
        if (isDashing(player)) {
            System.out.println("DEBUG: Player already dashing");
            return;
        }

        // Calculate forward/side components
        int forward = 0;
        int side = 0;

        if (input.forward && !input.backward) forward = 1;
        else if (input.backward && !input.forward) forward = -1;

        if (input.right && !input.left) side = 1;
        else if (input.left && !input.right) side = -1;

        // Execute dash with calculated direction
        tryDash(forward, side, player);
    }

    /**
     * Main dash execution method
     */
    public static void tryDash(int forward, int side, LivingEntity entity) {
        // Check conditions
        if (!entity.onGround() || isDashing(entity)) {
            return;
        }

        double dashSpeed = DASH_SPEED;
        Vec3 rotVec = Vec3.directionFromRotation(entity.getXRot(), entity.getYRot());
        rotVec = rotVec.yRot(1.57079632679f * side); // L/R rotation

        if (side != 0) {
            dashSpeed *= 0.75; // Sideways speed nerf
            if (forward == 1) {
                rotVec = rotVec.yRot(-0.785398163397f * side); // Forward diagonals
            }
        }
        if (forward == -1) {
            rotVec = rotVec.yRot(side == 0 ? 3.14159265359f : 0.785398163397f * side); // Back diagonals
            dashSpeed *= 0.75; // Backwards speed nerf
        }

        // Add to global map
        activeDashes.put(entity, new DashData(rotVec.normalize().scale(dashSpeed), entity));

        // Play sound
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f, 0.8f);

        System.out.println("DEBUG: Started dash - forward: " + forward + ", side: " + side + ", vector: " + rotVec.normalize().scale(dashSpeed));
    }

    /**
     * Global tick method for all dashes
     * This should be called from server tick
     */
    public static void tickAllDashes() {
        if (!activeDashes.isEmpty()) {
            System.out.println("DEBUG: Ticking " + activeDashes.size() + " active dashes");
        }

        // Tick all active dashes
        for (Map.Entry<LivingEntity, DashData> entry : activeDashes.entrySet()) {
            DashData dash = entry.getValue();
            dash.tickDash();

            // Add particles for players
            if (dash.entity instanceof Player) {
                addDashParticles((Player) dash.entity);
            }
        }

        // Remove finished dashes
        activeDashes.entrySet().removeIf(entry -> entry.getValue().finished);
    }

    /**
     * Add particle trail during dash
     */
    private static void addDashParticles(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            Vec3 playerPos = player.position();
            Vec3 velocity = player.getDeltaMovement();

            // Particles behind the player
            Vec3 particlePos = playerPos.add(velocity.scale(-0.3));

            serverLevel.sendParticles(
                    ParticleTypes.CLOUD,
                    particlePos.x, particlePos.y + 0.1, particlePos.z,
                    2, // particle count
                    0.1, 0.05, 0.1, // spread
                    0.02 // speed
            );
        }
    }

    /**
     * Check if entity is dashing
     */
    public static boolean isDashing(LivingEntity entity) {
        return activeDashes.containsKey(entity);
    }

    /**
     * Get dash data
     */
    public static DashData getDash(LivingEntity entity) {
        return activeDashes.get(entity);
    }

    /**
     * Dash data container
     */
    public static class DashData {
        public final Vec3 dashVector;
        public final LivingEntity entity;
        public boolean finished = false;
        private int duration = 10;

        public DashData(Vec3 dashVector, LivingEntity entity) {
            this.dashVector = dashVector;
            this.entity = entity;
        }

        public void tickDash() {
            duration--;

            // Check if stunned (stops dashes)
            if (entity.hasEffect(com.xirc.nichirin.registry.NichirinEffectRegistry.STUNNED.get())) {
                finished = true;
                return;
            }

            if (duration <= 5) { // 5 ticks of movement, then recovery
                if (duration <= 0) {
                    finished = true;
                }
                return;
            }

            // Apply movement
            entity.setDeltaMovement(entity.getDeltaMovement().add(dashVector).scale(0.5));
            entity.hurtMarked = true;

            System.out.println("DEBUG: Dash tick - duration: " + duration + ", applied vector: " + dashVector + ", new velocity: " + entity.getDeltaMovement());
        }
    }

    /**
     * Legacy method for compatibility - not used in new system
     */
    public static void tick() {
        // This method is kept for compatibility but does nothing
        // The real ticking happens in tickAllDashes()
    }
}