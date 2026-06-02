package com.xirc.nichirin.common.attack.moves.breathing.thunder;

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
import net.minecraft.server.level.ServerLevel;

/**
 * Seventh Form: Honoikazuchi no Kami (Flaming Thunder God)
 * Zenitsu's personal ultimate technique - massive damage teleport dash
 */
public class HonoikazuchiNoKamiAttack extends ThunderBreathingAttackBase {

    private boolean hasExecuted = false;
    private Set<LivingEntity> hitEntities = new HashSet<>(); // Track hit entities to avoid double hits

    public HonoikazuchiNoKamiAttack() {
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

        // Add glowing effect
        user.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0, false, false));
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
            // Crackle a lightning aura around the user every half-second of the windup so the
            // charge isn't free positioning — anyone caught in melee range eats chip damage.
            if (tickCount > 0 && tickCount % 10 == 0) {
                dealWindupAura();
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

    /**
     * Windup damage tick — small lightning AOE around the user during the long charge so the
     * 6-second windup doesn't just hand the opponent a free reposition. Damage per pulse is
     * a small fraction of the full strike so chip-pressure < single-hit ult damage.
     */
    private void dealWindupAura() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        float auraRange = 4.0f;
        List<LivingEntity> targets = getTargetsInCustomHitbox(userPos, auraRange, 3.0, auraRange);
        float originalDamage = damage;
        damage = Math.max(3.0f, originalDamage * 0.08f);
        try {
            for (LivingEntity target : targets) {
                hitTarget(target);
            }
        } finally {
            damage = originalDamage;
        }
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    userPos.x, userPos.y, userPos.z,
                    25, auraRange, 1.0, auraRange, 0.2);
        }
        if (!targets.isEmpty()) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5f, 1.6f);
        }
    }

    private void executeUltimateDash() {
        // Store initial position for hit detection
        Vec3 startPos = user.position();

        // Use teleportDistance from configuration (set by moveset)
        float dashDistance = teleportDistance != null ? teleportDistance : range;

        // Configure ultimate teleport with massive effects
        TeleportUtil.TeleportOptions options = new TeleportUtil.TeleportOptions()
                .withParticles(ParticleTypes.ELECTRIC_SPARK, ParticleTypes.EXPLOSION)
                .withTrail(ParticleTypes.ELECTRIC_SPARK, 16.0f) // Very dense trail
                .withSounds(SoundEvents.LIGHTNING_BOLT_THUNDER, SoundEvents.GENERIC_EXPLODE.value())
                .withDamageCallback(target -> {
                    // Hit targets along the path
                    if (!hitEntities.contains(target)) {
                        hitTargetUltimate(target);
                        hitEntities.add(target);
                    }
                });

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
        boolean success = TeleportUtil.teleportInDirection(user, dashDistance, options);

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
        // Get all targets in the large hitbox (using configured hitboxSize)
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
        // Use base hit method for damage and shock (using configured damage, hitStun)
        hitTarget(target);

        // Additional effects for ultimate
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 2, false, false));

        // Massive knockback (using configured knockback)
        Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
        target.push(knockbackDir.x * knockback, 0.5, knockbackDir.z * knockback);

        // Extra particle explosion per target
        if (world instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    target.getX(), target.getY() + 1, target.getZ(),
                    1, 0, 0, 0, 0);

            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + 1, target.getZ(),
                    100, 1.0, 1.0, 1.0, 0.5);
        }
    }

    private void createLightningDragonEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position();
        Vec3 lookDir = user.getLookAngle();
        float effectRange = teleportDistance != null ? teleportDistance : range;

        // Create dragon-shaped particle trail
        for (int i = 0; i < 50; i++) {
            double progress = i / 50.0;
            double wave = Math.sin(progress * Math.PI * 4) * 2; // Serpentine motion

            Vec3 basePos = userPos.add(lookDir.scale(progress * effectRange));
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
        if (!(world instanceof ServerLevel serverLevel)) return;

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
    }

    @Override
    protected void onStop() {
        user.setInvulnerable(false);

        hitEntities.clear();
    }
}