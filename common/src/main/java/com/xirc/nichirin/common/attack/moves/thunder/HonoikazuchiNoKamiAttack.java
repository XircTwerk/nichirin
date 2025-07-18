package com.xirc.nichirin.common.attack.moves.thunder;

import com.xirc.nichirin.common.util.TeleportUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Seventh Form: Honoikazuchi no Kami (Flaming Thunder God)
 * Zenitsu's personal ultimate technique - massive damage teleport dash
 */
public class HonoikazuchiNoKamiAttack extends ThunderBreathingAttackBase {

    private boolean hasExecuted = false;
    private Set<LivingEntity> hitEntities = new HashSet<>(); // Track hit entities to avoid double hits

    public HonoikazuchiNoKamiAttack() {
        withTiming(300, 20, 60) // 15 second cooldown, longer windup and duration
                .withDamage(50.0f) // Massive damage
                .withRange(30.0f) // Very long dash
                .withKnockback(2.0f) // High knockback
                .withBreathCost(50.0f) // Half breath cost
                .withHitStun(40) // 2 second stun
                .withHitboxSize(4.0f); // Larger hitbox for ultimate
    }

    @Override
    protected void onStart() {
        hasExecuted = false;
        hitEntities.clear();

        // Epic charge-up effects
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0f, 0.5f);

        // Give user invulnerability during entire windup
        user.setInvulnerable(true);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Keep invulnerability during windup
        if (tickCount <= windup) {
            // Still in windup phase, maintain invulnerability
            if (!user.isInvulnerable()) {
                user.setInvulnerable(true);
            }
            return;
        }

        // Execute the ultimate dash once after windup completes
        if (!hasExecuted && tickCount == windup + 1) {
            // Remove invulnerability now that windup is complete
            user.setInvulnerable(false);

            executeUltimateDash();
            hasExecuted = true;
        }

        // Check for hits in the area around the user during the entire duration
        if (hasExecuted && tickCount > windup && tickCount < windup + duration) {
            checkAreaDamage();
        }

        // Apply speed boost after dash completes
        if (tickCount == windup + duration - 1) {
            applySpeedBoost();
        }
    }

    private void executeUltimateDash() {
        // Store initial position for hit detection
        Vec3 startPos = user.position();

        // Configure ultimate teleport with massive effects
        TeleportUtil.TeleportOptions options = new TeleportUtil.TeleportOptions()
                .withParticles(ParticleTypes.ELECTRIC_SPARK, ParticleTypes.EXPLOSION)
                .withTrail(ParticleTypes.ELECTRIC_SPARK, 16.0f) // Very dense trail
                .withSounds(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundEvents.GENERIC_EXPLODE)
                .withDamageCallback(target -> {
                    // Hit targets along the path
                    if (!hitEntities.contains(target)) {
                        hitTargetUltimate(target);
                        hitEntities.add(target);
                    }
                });
        // REMOVED .unsafe() - now respects blocks like Thunder Clap and Flash

        // Custom sound properties
        options.soundVolume = 2.0f;
        options.soundPitch = 0.5f;
        options.departureParticleCount = 100;
        options.arrivalParticleCount = 100;

        // Pre-teleport: Create dragon-like lightning effect
        options.preTeleport = entity -> {
            createLightningDragonEffect();
        };

        // Post-teleport: Explosion effect and area damage
        options.postTeleport = entity -> {
            createExplosionEffect();
            // Hit all enemies in the large hitbox at destination
            checkAreaDamageAtPosition(entity.position());
        };

        // Perform the ultimate dash
        boolean success = TeleportUtil.teleportInDirection(user, range, options);

        // If teleport was blocked, still do damage in current area
        if (!success) {
            checkAreaDamageAtPosition(startPos);
            createExplosionEffect();
        }
    }

    private void checkAreaDamage() {
        // Check for enemies in the hitbox around current position
        checkAreaDamageAtPosition(user.position());
    }

    private void checkAreaDamageAtPosition(Vec3 position) {
        // Get all targets in the large hitbox
        List<LivingEntity> targets = getTargetsInHitbox(position);

        for (LivingEntity target : targets) {
            // Only hit each entity once
            if (!hitEntities.contains(target)) {
                hitTargetUltimate(target);
                hitEntities.add(target);
            }
        }
    }

    private void hitTargetUltimate(LivingEntity target) {
        // Use base hit method for damage and shock
        hitTarget(target);

        // Additional effects for ultimate
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 2, false, false));

        // Massive knockback
        Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
        target.push(knockbackDir.x * knockback, 0.5, knockbackDir.z * knockback);

        // Extra particle explosion per target
        if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    target.getX(), target.getY() + 1, target.getZ(),
                    1, 0, 0, 0, 0);

            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + 1, target.getZ(),
                    100, 1.0, 1.0, 1.0, 0.5);
        }
    }

    private void createLightningDragonEffect() {
        if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        Vec3 userPos = user.position();
        Vec3 lookDir = user.getLookAngle();

        // Create dragon-shaped particle trail
        for (int i = 0; i < 50; i++) {
            double progress = i / 50.0;
            double wave = Math.sin(progress * Math.PI * 4) * 2; // Serpentine motion

            Vec3 basePos = userPos.add(lookDir.scale(progress * range));
            Vec3 offset = lookDir.cross(new Vec3(0, 1, 0)).normalize().scale(wave);
            Vec3 particlePos = basePos.add(offset).add(0, 1 + progress * 2, 0);

            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    particlePos.x, particlePos.y, particlePos.z,
                    5, 0.2, 0.2, 0.2, 0.1);

            if (i % 5 == 0) {
                serverLevel.sendParticles(ParticleTypes.END_ROD,
                        particlePos.x, particlePos.y, particlePos.z,
                        3, 0.3, 0.3, 0.3, 0.05);
            }
        }

        // Thunder roar sound
        world.playSound(null, userPos.x, userPos.y, userPos.z,
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5f, 2.0f);
    }

    private void createExplosionEffect() {
        if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        Vec3 pos = user.position();

        // Massive explosion particles
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y + 1, pos.z,
                3, 0, 0, 0, 0);

        // Ring of electric particles
        for (int angle = 0; angle < 360; angle += 10) {
            double rad = Math.toRadians(angle);
            for (double r = 2; r < 10; r += 0.5) {
                Vec3 ringPos = pos.add(Math.cos(rad) * r, 0.5, Math.sin(rad) * r);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        ringPos.x, ringPos.y, ringPos.z,
                        1, 0, 0, 0, 0);
            }
        }
    }

    private void applySpeedBoost() {
        // Speed 1 for 8 seconds (160 ticks)
        user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0, false, true));

        // Also give brief regeneration as a bonus
        user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, true));

        // Notification
        user.displayClientMessage(
                net.minecraft.network.chat.Component.literal("Thunder God's blessing grants you speed!")
                        .withStyle(style -> style.withColor(0xFFFF00).withBold(true)),
                true
        );
    }

    @Override
    protected void onStop() {
        // Ensure invulnerability is removed
        user.setInvulnerable(false);

        // Clear hit entities set
        hitEntities.clear();
    }
}