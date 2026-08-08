package com.xirc.nichirin.common.attack.moves.breathing.water;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.xirc.nichirin.common.vfx.VfxIds;

import java.util.List;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fifth Form: Blessed Rain After the Drought
 * Ultimate level single hit dash that drops ½ a healthbar on hit
 * Very small 1.0 block hitbox - precision attack that can be angled downwards
 * Similar to Butterfly Attack but with teleport instead of dash after leap
 */
public class BlessedRainAttack extends WaterBreathingAttackBase {

    private boolean leapStarted = false;
    private boolean teleportExecuted = false;
    private Vec3 leapDirection;
    private Vec3 teleportDirection;
    private Vec3 startPosition;

    // Invulnerability during attack
    private boolean wasInvulnerable = false;

    public BlessedRainAttack() {
    }

    @Override
    protected void onStart() {
        leapStarted = false;
        teleportExecuted = false;
        startPosition = user.position();

        // Capture leap and teleport directions at start
        leapDirection = user.getLookAngle().normalize();
        teleportDirection = leapDirection; // Same direction for both

        // Check if user is looking down for angled attack
        if (user.getXRot() > 10) { // Looking down
            teleportDirection = new Vec3(leapDirection.x, -0.5, leapDirection.z).normalize();
        }

        // Make user invulnerable during attack
        wasInvulnerable = user.isInvulnerable();
        user.setInvulnerable(true);

        // Blessed rain startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 0.8f, 1.5f);

        // Create rain gathering effect
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute initial leap after windup (similar to Butterfly)
        if (!leapStarted && tickCount == windup + 1) {
            executeInitialLeap();
            leapStarted = true;
        }

        // Execute teleport strike 15 ticks after leap (0.75 seconds)
        if (leapStarted && !teleportExecuted && tickCount >= windup + 16) {
            executeTeleportStrike();
            teleportExecuted = true;
        }
    }


    private void executeInitialLeap() {
        playWaterVfx(VfxIds.BLESSED_RAIN_LEAP, user.position(), leapDirection, 0.88f);
        // Leap upward with slight forward movement (like Butterfly)
        double upwardVelocity = 0.9;
        double forwardVelocity = 0.15;

        Vec3 horizontalDirection = new Vec3(leapDirection.x, 0, leapDirection.z).normalize();
        Vec3 leapVelocity = horizontalDirection.scale(forwardVelocity).add(0, upwardVelocity, 0);

        user.setDeltaMovement(leapVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        // Sync to client
        if (user instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(user));
        }

        // Leap sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.PLAYERS, 1.0f, 1.8f);

        // Create leap effect
    }

    private void executeTeleportStrike() {
        // Calculate teleport destination
        Vec3 teleportDestination = user.position().add(teleportDirection.scale(range * 0.8));

        // Instant teleport, stopping at block collision instead of failing or clipping.
        teleportSafe(teleportDestination);
        Vec3 strikePosition = user.position();
        playWaterVfxAt(VfxIds.BLESSED_RAIN, strikePosition, Vec3.ZERO, 1.25f);

        // Create massive rain strike effect

        // Hit enemies in very small precise hitbox
        List<LivingEntity> targets = getTargetsInCustomHitbox(
                strikePosition.add(0, user.getBbHeight() / 2, 0),
                hitboxSize, // Very small 1.0 block hitbox
                hitboxSize + 0.5,
                hitboxSize
        );

        for (LivingEntity target : targets) {

            hitTarget(target);

            // Strong knockback
            Vec3 strikeKnockback = teleportDirection.scale(knockback);
            target.push(strikeKnockback.x, 0.5, strikeKnockback.z);

            // Create massive impact effect

            // Critical hit sound
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5f, 0.8f);
        }

        // Teleport strike sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 1.5f);
    }


    @Override
    public boolean isDashAttack() {
        return true; // This is a teleport dash attack
    }

    @Override
    protected void onStop() {
        // Restore original invulnerability state
        user.setInvulnerable(wasInvulnerable);

        // Reset velocity
        user.setDeltaMovement(Vec3.ZERO);

        // Peaceful rain ending sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.WEATHER_RAIN, SoundSource.PLAYERS, 0.8f, 1.2f);

        // Clear state
        leapStarted = false;
        teleportExecuted = false;
    }
}
