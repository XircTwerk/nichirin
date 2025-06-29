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
        System.out.println("=== TRY DOUBLE JUMP ===");
        System.out.println("Player: " + player.getName().getString());
        System.out.println("On ground: " + player.onGround());
        System.out.println("Side: " + (player.level().isClientSide() ? "CLIENT" : "SERVER"));

        // STRICT: Only allow double jump when NOT on ground
        if (player.onGround()) {
            System.out.println("FAILED - Player is on ground, double jump not allowed");
            return;
        }

        if (canDoubleJump(player)) {
            performDoubleJump(player);
        } else {
            System.out.println("FAILED - Cannot double jump");
        }
    }

    /**
     * Check if the player can double jump
     */
    public static boolean canDoubleJump(Player player) {
        // NEVER allow double jump on ground
        if (player.onGround()) {
            System.out.println("DEBUG: Cannot double jump - player is on ground");
            return false;
        }

        // Check stamina requirement
        if (!StaminaManager.hasStamina(player, STAMINA_COST)) {
            System.out.println("DEBUG: Cannot double jump - insufficient stamina (need " + STAMINA_COST + ", have " + StaminaManager.getStamina(player) + ")");
            return false;
        }

        JumpState state = getOrCreateState(player);

        // Can only double jump if:
        // 1. Player has left the ground at least once (hasLeftGround = true)
        // 2. Haven't used double jump yet (hasDoubleJumped = false)
        // 3. Player has been in air for at least 5 ticks (prevents immediate double jump)
        // 4. Has enough stamina (checked above)
        boolean canJump = state.hasLeftGround && !state.hasDoubleJumped && state.airTicks >= 5;

        System.out.println("DEBUG: Can double jump = " + canJump + " (hasLeftGround = " + state.hasLeftGround + ", hasDoubleJumped = " + state.hasDoubleJumped + ", airTicks = " + state.airTicks + ", stamina = " + StaminaManager.getStamina(player) + "/" + StaminaManager.getMaxStamina(player) + ")");
        return canJump;
    }

    // Add this method to PlayerDoubleJump.java after the existing performDoubleJump method:

    /**
     * Perform the double jump WITHOUT consuming stamina (for server-side use after stamina is already consumed)
     */
    public static void performDoubleJumpWithoutStamina(Player player) {
        System.out.println("=== PERFORMING DOUBLE JUMP (NO STAMINA CHECK) ===");

        // Safety check - never double jump on ground
        if (player.onGround()) {
            System.out.println("ABORTED - Player on ground!");
            return;
        }

        JumpState state = getOrCreateState(player);

        // Double check we can still double jump
        if (state.hasDoubleJumped) {
            System.out.println("ABORTED - Already used double jump!");
            return;
        }

        if (!state.hasLeftGround) {
            System.out.println("ABORTED - Player hasn't left ground yet!");
            return;
        }

        if (state.airTicks < 5) {
            System.out.println("ABORTED - Player hasn't been in air long enough! (airTicks = " + state.airTicks + ")");
            return;
        }

        // Mark as used
        state.hasDoubleJumped = true;
        state.fallDistanceAtDoubleJump = player.fallDistance;
        System.out.println("MARKED as used - hasDoubleJumped = true, recorded fall distance: " + state.fallDistanceAtDoubleJump);

        // Apply jump velocity
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, DOUBLE_JUMP_VELOCITY * 1.5, velocity.z);

        // Sync to client if on server
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player));
        }

        // Effects
        playDoubleJumpEffects(player);

        System.out.println("SUCCESS - Double jump completed! Velocity set to: " + (DOUBLE_JUMP_VELOCITY * 1.5));
    }

    /**
     * Perform the double jump (CLIENT SIDE ONLY - for immediate feedback)
     */
    /**
     * Perform the double jump
     */
    private static void performDoubleJump(Player player) {
        System.out.println("=== PERFORMING DOUBLE JUMP ===");

        // Safety check - never double jump on ground
        if (player.onGround()) {
            System.out.println("ABORTED - Player on ground!");
            return;
        }

        // Check stamina again before consuming
        if (!StaminaManager.hasStamina(player, STAMINA_COST)) {
            System.out.println("ABORTED - Insufficient stamina!");
            return;
        }

        JumpState state = getOrCreateState(player);

        // Double check we can still double jump
        if (state.hasDoubleJumped) {
            System.out.println("ABORTED - Already used double jump!");
            return;
        }

        if (!state.hasLeftGround) {
            System.out.println("ABORTED - Player hasn't left ground yet!");
            return;
        }

        if (state.airTicks < 5) {
            System.out.println("ABORTED - Player hasn't been in air long enough! (airTicks = " + state.airTicks + ")");
            return;
        }

        // ONLY CONSUME STAMINA ON SERVER
        if (!player.level().isClientSide) {
            // Consume stamina ONLY on server
            if (!StaminaManager.consume(player, STAMINA_COST)) {
                System.out.println("ABORTED - Failed to consume stamina!");
                return;
            }
            System.out.println("SUCCESS - Consumed " + STAMINA_COST + " stamina. Remaining: " + StaminaManager.getStamina(player));
        } else {
            System.out.println("CLIENT - Skipping stamina consumption (will be handled by server)");
        }

        // Mark as used
        state.hasDoubleJumped = true;
        state.fallDistanceAtDoubleJump = player.fallDistance; // Record fall distance when double jumping
        System.out.println("MARKED as used - hasDoubleJumped = true, recorded fall distance: " + state.fallDistanceAtDoubleJump);

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

        System.out.println("SUCCESS - Double jump completed! Velocity set to: " + (DOUBLE_JUMP_VELOCITY * 1.5));
    }

    /**
     * Create visual and audio effects for double jump
     */
    private static void playDoubleJumpEffects(Player player) {
        Level world = player.level();

        System.out.println("=== PLAYING DOUBLE JUMP EFFECTS ===");
        System.out.println("Side: " + (world.isClientSide() ? "CLIENT" : "SERVER"));

        // Sound effect - play on both sides
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.5f, 1.8f);

        // Particles - create on both client and server for immediate feedback
        if (world instanceof ServerLevel serverLevel) {
            System.out.println("Creating server-side particles");
            createParticleEffects(serverLevel, player);
        } else {
            System.out.println("Creating client-side particles");
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
            System.out.println("=== INITIALIZING PLAYER STATE ===");
            System.out.println("Player: " + player.getName().getString());
            System.out.println("Initial ground state: " + isOnGround);
            return; // Skip first tick to establish baseline
        }

        // Track previous ground state
        boolean wasOnGround = state.wasOnGround;

        // Detect when player leaves the ground (transitions from ground to air)
        if (wasOnGround && !isOnGround) {
            System.out.println("=== PLAYER LEFT GROUND ===");
            System.out.println("Player: " + player.getName().getString());
            System.out.println("Side: " + (player.level().isClientSide() ? "CLIENT" : "SERVER"));
            state.hasLeftGround = true;
            System.out.println("hasLeftGround set to true - double jump now available (if stamina sufficient)");
        }

        // Reset everything when player lands (transitions from air to ground)
        if (!wasOnGround && isOnGround) {
            System.out.println("=== PLAYER LANDED - RESETTING STATES ===");
            System.out.println("Player: " + player.getName().getString());
            System.out.println("Previous ground state: " + wasOnGround);
            System.out.println("Current ground state: " + isOnGround);
            System.out.println("Side: " + (player.level().isClientSide() ? "CLIENT" : "SERVER"));

            // Reset all flags
            state.hasDoubleJumped = false;
            state.hasLeftGround = false;
            state.airTicks = 0;
            state.fallDistanceAtDoubleJump = 0;

            System.out.println("Double jump and left ground states reset");
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
        System.out.println("Force reset double jump for: " + player.getName().getString());
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

            System.out.println("DEBUG: Reducing fall damage - Original: " + currentFallDistance +
                    ", Reduced: " + reducedDistance +
                    " (reduction: " + FALL_DAMAGE_REDUCTION + ")");

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