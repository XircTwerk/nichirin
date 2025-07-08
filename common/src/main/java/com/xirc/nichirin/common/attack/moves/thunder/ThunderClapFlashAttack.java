package com.xirc.nichirin.common.attack.moves.thunder;

import com.xirc.nichirin.common.util.TeleportUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;

/**
 * First Form: Thunderclap and Flash
 * Instant teleport dash that hits all enemies in path
 */
public class ThunderClapFlashAttack extends ThunderBreathingAttackBase {

    public ThunderClapFlashAttack() {
        // Configure the attack
        withTiming(30, 1, 15) // cooldown, windup, duration
                .withDamage(15.0f)
                .withRange(15.0f) // 15 block dash
                .withKnockback(0.1f)
                .withBreathCost(20.0f)
                .withHitStun(20); // 1 second stun
    }

    @Override
    protected void onStart() {
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
                    // Use our custom hit method that applies shocked effect
                    hitTarget(target);
                });

        // Set custom sound properties
        options.soundVolume = 0.5f;
        options.soundPitch = 2.0f;

        // Perform the teleport dash
        TeleportUtil.teleportInDirection(user, range, options);
    }
}