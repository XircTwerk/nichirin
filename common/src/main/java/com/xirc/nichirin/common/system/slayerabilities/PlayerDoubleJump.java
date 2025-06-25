package com.xirc.nichirin.common.system.slayerabilities;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
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
    private static final int PARTICLE_COUNT = 20;
    private static final float PARTICLE_SPREAD = 0.5f;

    /**
     * Call this when a player attempts to jump
     */
    public static void tryDoubleJump(Player player) {
        System.out.println("=== TRY DOUBLE JUMP ===");
        System.out.println("Player: " + player.getName().getString());
        System.out.println("On ground: " + player.onGround());
        System.out.println("Can double jump: " + canDoubleJump(player));

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
        System.out.println("=== CAN DOUBLE JUMP CHECK ===");
        System.out.println("Player on ground: " + player.onGround());

        // NEVER allow double jump on ground
        if (player.onGround()) {
            System.out.println("DENIED - Player is on ground");
            return false;
        }

        JumpState state = getOrCreateState(player);
        System.out.println("Has already double jumped: " + state.hasDoubleJumped);

        boolean canJump = !state.hasDoubleJumped;
        System.out.println("Final result: " + canJump);
        return canJump;
    }

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

        JumpState state = getOrCreateState(player);

        // Double check we can still double jump
        if (state.hasDoubleJumped) {
            System.out.println("ABORTED - Already used double jump!");
            return;
        }

        // Mark as used FIRST
        state.hasDoubleJumped = true;
        System.out.println("MARKED as used - hasDoubleJumped = true");

        // Apply jump velocity
        Vec3 velocity = player.getDeltaMovement();
        player.setDeltaMovement(velocity.x, DOUBLE_JUMP_VELOCITY, velocity.z);

        // Reset fall distance
        player.fallDistance = 0;

        // Sync to client if on server
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(player));
        }

        // Effects
        playDoubleJumpEffects(player);

        System.out.println("SUCCESS - Double jump completed!");
    }

    /**
     * Create visual and audio effects for double jump
     */
    private static void playDoubleJumpEffects(Player player) {
        Level world = player.level();

        // Sound effect
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.3f, 1.8f);

        // Particle effects
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * PARTICLE_SPREAD;
            double offsetZ = (world.random.nextDouble() - 0.5) * PARTICLE_SPREAD;
            double velocityX = offsetX * 0.2;
            double velocityY = -0.1;
            double velocityZ = offsetZ * 0.2;

            world.addParticle(ParticleTypes.CLOUD,
                    player.getX() + offsetX,
                    player.getY() - 0.1,
                    player.getZ() + offsetZ,
                    velocityX, velocityY, velocityZ);
        }

        // Sweep particles
        for (int i = 0; i < 5; i++) {
            double angle = (i / 5.0) * Math.PI * 2;
            double x = player.getX() + Math.cos(angle) * 0.8;
            double z = player.getZ() + Math.sin(angle) * 0.8;

            world.addParticle(ParticleTypes.SWEEP_ATTACK,
                    x, player.getY(), z,
                    0, 0, 0);
        }
    }

    /**
     * Call this every tick for each player to update jump states
     */
    public static void tickPlayer(Player player) {
        JumpState state = getOrCreateState(player);

        // Track previous ground state
        boolean wasOnGround = state.wasOnGround;
        state.wasOnGround = player.onGround();

        // Reset double jump when player lands (transitions from air to ground)
        if (!wasOnGround && player.onGround() && state.hasDoubleJumped) {
            System.out.println("=== PLAYER LANDED - RESETTING DOUBLE JUMP ===");
            System.out.println("Player: " + player.getName().getString());
            System.out.println("Side: " + (player.level().isClientSide ? "CLIENT" : "SERVER"));
            state.hasDoubleJumped = false;
            System.out.println("Double jump reset complete");
        }

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
        System.out.println("Force reset double jump for: " + player.getName().getString());
    }

    /**
     * Check if player has double jumped
     */
    public static boolean hasDoubleJumped(Player player) {
        return getOrCreateState(player).hasDoubleJumped;
    }

    /**
     * Jump state for each player
     */
    private static class JumpState {
        boolean hasDoubleJumped = false;
        boolean wasOnGround = true; // Start assuming on ground
    }
}