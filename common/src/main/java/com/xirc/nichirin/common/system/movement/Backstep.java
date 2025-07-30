package com.xirc.nichirin.common.system.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Backstep system - teleports player 5 blocks backward
 */
public class Backstep {

    private static final double BACKSTEP_DISTANCE = 3.0;
    private static final double SAFETY_CHECK_RADIUS = 0.5; // Player collision box consideration

    /**
     * Execute backstep teleport
     */
    public static void execute(Player player) {
        if (player == null || player.level().isClientSide) {
            return;
        }

        // Calculate backstep destination
        Vec3 backstepDestination = calculateBackstepDestination(player);

        if (backstepDestination == null) {
            // Backstep blocked - play fail sound
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.5f, 0.5f);
            System.out.println("DEBUG: Backstep blocked for player " + player.getName().getString());
            return;
        }

        // Perform the teleport
        performBackstepTeleport(player, backstepDestination);

        // Play backstep sound
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.2f);

        System.out.println("DEBUG: Player " + player.getName().getString() + " backstepped to " + backstepDestination);
    }

    /**
     * Calculate the backstep destination, checking for obstacles
     */
    private static Vec3 calculateBackstepDestination(Player player) {
        Level level = player.level();
        Vec3 playerPos = player.position();

        // Get player's look direction and reverse it for backstep
        Vec3 lookDirection = player.getLookAngle();
        Vec3 backstepDirection = new Vec3(-lookDirection.x, 0, -lookDirection.z).normalize();

        // Calculate target position
        Vec3 targetPos = playerPos.add(backstepDirection.scale(BACKSTEP_DISTANCE));

        // Perform raycast to check for obstacles
        Vec3 rayStart = playerPos.add(0, 0.1, 0); // Start slightly above ground
        Vec3 rayEnd = targetPos.add(0, 0.1, 0);

        BlockHitResult hitResult = level.clip(new ClipContext(
                rayStart,
                rayEnd,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        ));

        Vec3 finalDestination;

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            // Hit an obstacle - place player just before the obstacle
            Vec3 hitPos = hitResult.getLocation();
            Vec3 safeDirection = rayStart.vectorTo(hitPos).normalize();
            double safeDistance = rayStart.distanceTo(hitPos) - SAFETY_CHECK_RADIUS;
            finalDestination = rayStart.add(safeDirection.scale(Math.max(0.5, safeDistance)));
        } else {
            // No obstacle - use full distance
            finalDestination = targetPos;
        }

        // Ensure landing position is safe
        Vec3 safeDestination = findSafeLandingPosition(level, finalDestination);

        return safeDestination;
    }

    /**
     * Find a safe landing position (solid ground, no suffocation)
     */
    private static Vec3 findSafeLandingPosition(Level level, Vec3 targetPos) {
        // Check vertically for a safe landing spot
        for (int yOffset = 2; yOffset >= -3; yOffset--) {
            Vec3 checkPos = targetPos.add(0, yOffset, 0);
            BlockPos blockPos = new BlockPos((int)checkPos.x, (int)checkPos.y, (int)checkPos.z);

            // Check if position is safe
            if (isSafeLandingPosition(level, blockPos)) {
                return checkPos;
            }
        }

        // If no safe position found, return null (backstep will fail)
        return null;
    }

    /**
     * Check if a position is safe for landing
     */
    private static boolean isSafeLandingPosition(Level level, BlockPos pos) {
        // Check ground block (should be solid)
        BlockState groundBlock = level.getBlockState(pos.below());
        if (!groundBlock.isSolid()) {
            return false;
        }

        // Check feet position (should be air or passable)
        BlockState feetBlock = level.getBlockState(pos);
        if (!feetBlock.isAir() && feetBlock.isSolid()) {
            return false;
        }

        // Check head position (should be air or passable)
        BlockState headBlock = level.getBlockState(pos.above());
        if (!headBlock.isAir() && headBlock.isSolid()) {
            return false;
        }

        return true;
    }

    /**
     * Perform the actual teleport
     */
    private static void performBackstepTeleport(Player player, Vec3 destination) {
        // Set player position
        player.teleportTo(destination.x, destination.y, destination.z);

        // Reset fall distance to prevent fall damage
        player.fallDistance = 0;

        // Stop current movement
        player.setDeltaMovement(Vec3.ZERO);

        // Add teleport particles at both locations
        addTeleportEffects(player, player.position(), destination);
    }

    /**
     * Add visual effects for the teleport
     */
    private static void addTeleportEffects(Player player, Vec3 startPos, Vec3 endPos) {
        Level level = player.level();

        // TODO: Add particle effects at start and end positions
        // For now, just play sounds at both locations

        // Sound at start position
        level.playSound(null, startPos.x, startPos.y, startPos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.3f, 0.8f);

        // Sound at end position
        level.playSound(null, endPos.x, endPos.y, endPos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.3f, 1.4f);
    }

    /**
     * Check if backstep is possible from current position
     * This can be used for UI feedback or preventing impossible backsteps
     */
    public static boolean canBackstep(Player player) {
        if (player == null) return false;

        Vec3 destination = calculateBackstepDestination(player);
        return destination != null;
    }

    /**
     * Get the distance that would be covered by a backstep
     * Useful for UI display or game balance
     */
    public static double getBackstepDistance(Player player) {
        if (player == null) return 0;

        Vec3 destination = calculateBackstepDestination(player);
        if (destination == null) return 0;

        return player.position().distanceTo(destination);
    }
}