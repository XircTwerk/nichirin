package com.xirc.nichirin.common.attack.moves.thunder;

import com.xirc.nichirin.common.util.TeleportUtil;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * First Form: Thunderclap and Flash
 * Instant teleport dash that hits all enemies in path with slight upward knockback
 *
 * All configuration now comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class ThunderClapFlashAttack extends ThunderBreathingAttackBase {

    // Store the starting position to look back at
    private Vec3 startPosition = null;

    // Static map to track crouch state for each player
    private static final Map<UUID, Boolean> CROUCH_DASH_MAP = new HashMap<>();

    public ThunderClapFlashAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    /**
     * Static method to set whether this attack should turn backwards
     */
    public static void setCrouchDash(Player player, boolean crouchDash) {
        if (crouchDash) {
            CROUCH_DASH_MAP.put(player.getUUID(), true);
        } else {
            CROUCH_DASH_MAP.remove(player.getUUID());
        }
    }

    @Override
    protected void onStart() {
        // Store the starting position (eye position for accurate looking back)
        startPosition = user.getEyePosition();

        // Thunder sound on start
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.PLAYERS,
                1f, 2.0f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Only execute once (on first perform tick)
        if (tickCount == windup + 1) {
            executeTeleportDash();

            // Check if we should turn backwards IMMEDIATELY after the dash
            Boolean shouldTurnBackwards = CROUCH_DASH_MAP.get(user.getUUID());

            if (shouldTurnBackwards != null && shouldTurnBackwards && startPosition != null) {
                // Make the player look back at their starting position RIGHT NOW
                user.lookAt(EntityAnchorArgument.Anchor.EYES, startPosition);

                // Force sync to client for immediate rotation
                if (user instanceof ServerPlayer serverPlayer) {
                    // Get the new rotation values after lookAt
                    float newYaw = user.getYRot();
                    float newPitch = user.getXRot();

                    // Send position update with new rotation
                    serverPlayer.connection.teleport(user.getX(), user.getY(), user.getZ(),
                            newYaw, newPitch);
                }
            }
        }
    }

    private void executeTeleportDash() {
        // Use teleportDistance from configuration (set by moveset)
        float dashDistance = teleportDistance != null ? teleportDistance : range;

        // Configure teleport with thunder effects using custom thunder particles
        TeleportUtil.TeleportOptions options = new TeleportUtil.TeleportOptions()
                .withParticles(NichirinParticleRegistry.THUNDER.get(), NichirinParticleRegistry.THUNDER.get())
                .withTrail(NichirinParticleRegistry.THUNDER.get(), 1.0f) // Denser lightning trail with more particles
                .withSounds(SoundEvents.LIGHTNING_BOLT_THUNDER, null)
                .withDamage(damage) // Use damage from configuration
                .withDamageCallback(target -> {
                    // Use our custom hit method that removes immunity frames
                    hitTargetNoImmunity(target);

                    // Add slight upward knockback
                    Vec3 currentVelocity = target.getDeltaMovement();
                    Vec3 upwardKnockback = new Vec3(
                            currentVelocity.x * 0.1, // Reduce horizontal momentum slightly
                            0.1, // Slight upward boost (about 1 block high)
                            currentVelocity.z * 0.1  // Reduce horizontal momentum slightly
                    );
                    target.setDeltaMovement(upwardKnockback);
                    target.hurtMarked = true;
                    target.hasImpulse = true;

                    // Sync velocity for players
                    if (target instanceof ServerPlayer serverPlayer) {
                        serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(target));
                    }

                });

        // Set custom sound properties
        options.soundVolume = 1f;
        options.soundPitch = 2.0f;

        // Perform the teleport dash
        TeleportUtil.teleportInDirection(user, dashDistance, options);
    }

    @Override
    protected void onStop() {
        // Always clean up the map entry
        CROUCH_DASH_MAP.remove(user.getUUID());
    }
}