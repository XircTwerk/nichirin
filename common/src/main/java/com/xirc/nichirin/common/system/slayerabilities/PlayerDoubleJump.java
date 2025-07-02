package com.xirc.nichirin.common.system.slayerabilities;

import com.xirc.nichirin.common.util.StaminaManager;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDoubleJump {

    // Track jump state for each player
    private static final Map<UUID, JumpState> jumpStates = new HashMap<>();

    // Configuration
    private static final double DOUBLE_JUMP_VELOCITY = 0.42;
    private static final int PARTICLE_COUNT = 40;
    private static final float PARTICLE_SPREAD = 0.25f;
    private static final float FALL_DAMAGE_REDUCTION = 4.0f; // Reduces fall damage by 4 blocks worth
    private static final float STAMINA_COST = 10.0f; // Stamina required for double jump

    /**
     * Call this when a player attempts to jump
     */
    public static void tryDoubleJump(Player player) {

        // STRICT: Only allow double jump when NOT on ground
        if (player.onGround()) {
            return;
        }

        if (canDoubleJump(player)) {
            performDoubleJump(player);
        } else {
        }
    }

    /**
     * Check if the player can double jump
     */
    public static boolean canDoubleJump(Player player) {
        // NEVER allow double jump on ground
        if (player.onGround()) {
            return false;
        }

        // Check stamina requirement
        if (!StaminaManager.hasStamina(player, STAMINA_COST)) {
            return false;
        }

        JumpState state = getOrCreateState(player);

        // Can only double jump if:
        // 1. Player has left the ground at least once (hasLeftGround = true)
        // 2. Haven't used double jump yet (hasDoubleJumped = false)
        // 3. Player has been in air for at least 5 ticks (prevents immediate double jump)
        // 4. Has enough stamina (checked above)
        boolean canJump = state.hasLeftGround && !state.hasDoubleJumped && state.airTicks >= 5;
        return canJump;
    }

    // Add this method to PlayerDoubleJump.java after the existing performDoubleJump method:

    /**
     * Perform the double jump WITHOUT consuming stamina (for server-side use after stamina is already consumed)
     */
    public static void performDoubleJumpWithoutStamina(Player player) {

        // Safety check - never double jump on ground
        if (player.onGround()) {
            return;
        }

        JumpState state = getOrCreateState(player);

        // Double check we can still double jump
        if (state.hasDoubleJumped) {
            return;
        }

        if (!state.hasLeftGround) {
            return;
        }

        if (state.airTicks < 5) {
            return;
        }

        // Mark as used
        state.hasDoubleJumped = true;
        state.fallDistanceAtDoubleJump = player.fallDistance;

        // Apply jump velocity
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, DOUBLE_JUMP_VELOCITY * 1.5, velocity.z);

        // Sync to client if on server
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player));
        }

        // Effects
        playDoubleJumpEffects(player);
    }

    /**
     * Perform the double jump (CLIENT SIDE ONLY - for immediate feedback)
     */
    /**
     * Perform the double jump
     */
    private static void performDoubleJump(Player player) {

        // Safety check - never double jump on ground
        if (player.onGround()) {
            return;
        }

        // Check stamina again before consuming
        if (!StaminaManager.hasStamina(player, STAMINA_COST)) {
            return;
        }

        JumpState state = getOrCreateState(player);

        // Double check we can still double jump
        if (state.hasDoubleJumped) {
            return;
        }

        if (!state.hasLeftGround) {
            return;
        }

        if (state.airTicks < 5) {
            return;
        }

        // ONLY CONSUME STAMINA ON SERVER
        if (!player.level().isClientSide) {
            // Consume stamina ONLY on server
            if (!StaminaManager.consume(player, STAMINA_COST)) {
                return;
            }
        } else {
        }

        // Mark as used
        state.hasDoubleJumped = true;
        state.fallDistanceAtDoubleJump = player.fallDistance; // Record fall distance when double jumping

        // Apply jump velocity - increased for more noticeable effect
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, DOUBLE_JUMP_VELOCITY * 1.5, velocity.z);

        // Don't reset fall distance here - we'll reduce it when they land
        // player.fallDistance = 0; // REMOVED - we track it instead

        // Sync to client if on server
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player));
        }

        // Effects - always play, both client and server
        playDoubleJumpEffects(player);
    }

    /**
     * Create visual and audio effects for double jump
     */
    private static void playDoubleJumpEffects(Player player) {
        Level world = player.level();

        // Sound effect - play on both sides
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5f, 1.8f);

        // Particles - create on both client and server for immediate feedback
        if (world instanceof ServerLevel serverLevel) {;
            createParticleEffects(serverLevel, player);
        } else {
            // For client side, we need to spawn particles differently
            createClientParticles(world, player);
        }
    }

    /**
     * Create server-side particle effects
     */
    private static void createParticleEffects(ServerLevel serverLevel, Player player) {
        // Radial white particle burst effect
        int particleCount = 30;
        double radius = 1;

        for (int i = 0; i < particleCount; i++) {
            // Calculate angle for radial distribution
            double angle = (2.0 * Math.PI * i) / particleCount;

            // Create horizontal ring
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY();

            // Velocity pointing outward from center
            double velocityX = Math.cos(angle) * 0.4;
            double velocityZ = Math.sin(angle) * 0.4;
            double velocityY = 0.2;

            // White cloud particles for the radial effect
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    x, y, z,
                    1, velocityX, velocityY, velocityZ, 0.1);
        }

        // Additional vertical burst effect
        for (int i = 0; i < 20; i++) {
            double angle = serverLevel.random.nextDouble() * Math.PI * 2;
            double distance = serverLevel.random.nextDouble() * 1.0;

            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;
            double y = player.getY() - 0.2;

            // Upward velocity with slight spread
            double velocityX = (serverLevel.random.nextDouble() - 0.5) * 0.3;
            double velocityY = 0.5 + serverLevel.random.nextDouble() * 0.3;
            double velocityZ = (serverLevel.random.nextDouble() - 0.5) * 0.3;

            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    x, y, z,
                    10, velocityX, velocityY, velocityZ, 0.1);
        }
    }

    /**
     * Create client-side particle effects
     */
    private static void createClientParticles(Level world, Player player) {
        // For client-side immediate feedback, spawn fewer particles
        for (int i = 0; i < 15; i++) {
            double angle = (2.0 * Math.PI * i) / 15;
            double radius = 1.2;

            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY();

            double velocityX = Math.cos(angle) * 0.3;
            double velocityZ = Math.sin(angle) * 0.3;
            double velocityY = 0.1;

            // Add particles to the world directly
            world.addParticle(ParticleTypes.CLOUD, x, y, z, velocityX, velocityY, velocityZ);
            world.addParticle(ParticleTypes.CLOUD,
                    player.getX() + (world.random.nextDouble() - 0.5) * 0.5,
                    player.getY(),
                    player.getZ() + (world.random.nextDouble() - 0.5) * 0.5,
                    (world.random.nextDouble() - 0.5) * 0.2,
                    0.3,
                    (world.random.nextDouble() - 0.5) * 0.2);
        }
    }

    /**
     * Call this every tick for each player to update jump states
     */
    public static void tickPlayer(Player player) {
        JumpState state = getOrCreateState(player);

        // Get current ground state
        boolean isOnGround = player.onGround();

        // Update air tick counter
        if (!isOnGround) {
            state.airTicks++;
        } else {
            state.airTicks = 0;
        }

        // Initialize wasOnGround on first tick if needed
        if (!state.initialized) {
            state.wasOnGround = isOnGround;
            state.initialized = true;
            return; // Skip first tick to establish baseline
        }

        // Track previous ground state
        boolean wasOnGround = state.wasOnGround;

        // Detect when player leaves the ground (transitions from ground to air)
        if (wasOnGround && !isOnGround) {
            state.hasLeftGround = true;
        }

        // Reset everything when player lands (transitions from air to ground)
        if (!wasOnGround && isOnGround) {

            // Reset all flags
            state.hasDoubleJumped = false;
            state.hasLeftGround = false;
            state.airTicks = 0;
            state.fallDistanceAtDoubleJump = 0;
        }

        // Update current ground state for next tick
        state.wasOnGround = isOnGround;

        // Clean up old states periodically
        if (player.tickCount % 400 == 0) {
            cleanupOldStates();
        }
    }

    /**
     * Get or create jump state for a player
     */
    private static JumpState getOrCreateState(Player player) {
        return jumpStates.computeIfAbsent(player.getUUID(), uuid -> new JumpState());
    }

    /**
     * Remove states for players who are no longer online
     */
    private static void cleanupOldStates() {
        if (jumpStates.size() > 100) {
            jumpStates.clear();
        }
    }

    /**
     * Force reset a player's double jump
     */
    public static void resetDoubleJump(Player player) {
        JumpState state = getOrCreateState(player);
        state.hasDoubleJumped = false;
        state.hasLeftGround = false;
        state.airTicks = 0;
        state.fallDistanceAtDoubleJump = 0;
    }

    /**
     * Check if player has double jumped
     */
    public static boolean hasDoubleJumped(Player player) {
        return getOrCreateState(player).hasDoubleJumped;
    }

    /**
     * Calculate the reduced fall damage for a player who double jumped
     * Call this before fall damage is applied
     */
    public static float getReducedFallDistance(Player player) {
        JumpState state = getOrCreateState(player);

        // If player double jumped, reduce the effective fall distance
        if (state.hasDoubleJumped && state.fallDistanceAtDoubleJump > 0) {
            float currentFallDistance = player.fallDistance;
            float reducedDistance = Math.max(0, currentFallDistance - FALL_DAMAGE_REDUCTION);
            return reducedDistance;
        }

        return player.fallDistance;
    }

    /**
     * Gets the stamina cost for double jumping
     */
    public static float getStaminaCost() {
        return STAMINA_COST;
    }

    /**
     * Jump state for each player
     */
    private static class JumpState {
        boolean hasDoubleJumped = false;
        boolean wasOnGround = false;
        boolean initialized = false;
        boolean hasLeftGround = false; // Track if player has left ground since last landing
        int airTicks = 0; // Track how many ticks player has been in air
        float fallDistanceAtDoubleJump = 0; // Track fall distance when double jump was performed
    }
}