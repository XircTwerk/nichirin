package com.xirc.nichirin.common.attack.moves.thunder;

import com.xirc.nichirin.common.network.CooldownDisplayPacket;
import com.xirc.nichirin.common.util.TeleportUtil;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * First Form: Thunderclap and Flash
 * Instant teleport dash that hits all enemies in path
 */
public class ThunderClapFlashAttack extends ThunderBreathingAttackBase {

    // Store the starting position to look back at
    private Vec3 startPosition = null;

    // Static map to track crouch state for each player
    private static final Map<UUID, Boolean> CROUCH_DASH_MAP = new HashMap<>();

    public ThunderClapFlashAttack() {
        // Configure the attack
        withTiming(30, 1, 15) // cooldown, windup, duration
                .withDamage(15.0f)
                .withRange(15.0f) // 15 block dash
                .withKnockback(0.1f)
                .withBreathCost(20.0f)
                .withHitStun(20); // 1 second stun
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
                0.5f, 2.0f);
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
        // Configure teleport with thunder effects
        TeleportUtil.TeleportOptions options = new TeleportUtil.TeleportOptions()
                .withParticles(ParticleTypes.ELECTRIC_SPARK, ParticleTypes.ELECTRIC_SPARK)
                .withTrail(ParticleTypes.ELECTRIC_SPARK, 8.0f) // Dense lightning trail
                .withSounds(SoundEvents.LIGHTNING_BOLT_THUNDER, null)
                .withDamage(damage)
                .withDamageCallback(target -> {
                    // Use our custom hit method that removes immunity frames
                    hitTargetNoImmunity(target);
                });

        // Set custom sound properties
        options.soundVolume = 0.5f;
        options.soundPitch = 2.0f;

        // Perform the teleport dash
        TeleportUtil.teleportInDirection(user, range, options);
    }

    @Override
    protected void onStop() {
        // Always clean up the map entry
        CROUCH_DASH_MAP.remove(user.getUUID());
    }
}