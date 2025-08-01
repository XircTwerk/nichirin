package com.xirc.nichirin.common.attack.moves.flame;

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
 * Ninth Form: Rengoku
 * The most powerful Flame Breathing technique. The user assumes a high stance before
 * performing an extremely high-speed dash towards the target and unleashing a singular,
 * devastating slash. The technique is powerful enough to completely carve out the ground
 * in its wake. This technique seemingly takes the form of a flaming Japanese dragon
 * that envelopes the user as they are charging towards the target.
 *
 * Mechanics:
 * - Summons a flaming dragon effect during the dash
 * - Creates massive ground damage in its wake
 * - Ultimate technique with massive damage and range
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class RengokuAttack extends FlameBreathingAttackBase {

    private boolean hasExecuted = false;
    private Set<LivingEntity> hitEntities = new HashSet<>();

    public RengokuAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        hasExecuted = false;
        hitEntities.clear();

        // Epic windup effects
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 2.0f, 0.3f);

        // Give user invulnerability and effects during windup
        user.setInvulnerable(true);
        user.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
        user.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, false, false));

        // Create charging flame aura
        createChargeUpEffect();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Keep invulnerability during windup
        if (tickCount <= windup) {
            // Still in windup phase, create charging effects
            if (tickCount % 10 == 0) {
                createChargeUpEffect();
                // Rumbling sound during charge
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.5f);
            }
            return;
        }

        // Execute the ultimate dragon dash once after windup completes
        if (!hasExecuted && tickCount == windup + 1) {
            // Remove invulnerability now that dash begins
            user.setInvulnerable(false);

            executeRengokuDash();
            hasExecuted = true;
        }

        // Continue hitting enemies along the path during the entire dash duration
        if (hasExecuted && tickCount > windup && tickCount < windup + duration) {
            continueFlameWake();
        }
    }

    private void createChargeUpEffect() {
        if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create expanding rings of flame
        for (int ring = 1; ring <= 5; ring++) {
            float ringRadius = ring * 1.5f;
            int particlesInRing = ring * 8;

            for (int i = 0; i < particlesInRing; i++) {
                double angle = (2 * Math.PI * i) / particlesInRing;
                double x = userPos.x + Math.cos(angle) * ringRadius;
                double z = userPos.z + Math.sin(angle) * ringRadius;
                double y = userPos.y + ring * 0.3;

                serverLevel.sendParticles(ParticleTypes.FLAME,
                        x, y, z, 2, 0.1, 0.1, 0.1, 0.05);
            }
        }

        // Central flame pillar
        serverLevel.sendParticles(ParticleTypes.FLAME,
                userPos.x, userPos.y, userPos.z, 20, 0.5, 2.0, 0.5, 0.2);
    }

    private void executeRengokuDash() {
        // Store initial position for wake effect
        Vec3 startPos = user.position();

        // Use dashSpeed from configuration (set by moveset)
        float dashDistance = range; // Use full range for ultimate

        // Configure ultimate dash with dragon effects
        TeleportUtil.TeleportOptions options = new TeleportUtil.TeleportOptions()
                .withParticles(ParticleTypes.FLAME, ParticleTypes.EXPLOSION)
                .withTrail(ParticleTypes.FLAME, 8.0f) // Very dense dragon trail
                .withSounds(SoundEvents.ENDER_DRAGON_FLAP, SoundEvents.GENERIC_EXPLODE)
                .withDamageCallback(target -> {
                    // Hit targets along the dragon's path
                    if (!hitEntities.contains(target)) {
                        hitTargetUltimate(target);
                        hitEntities.add(target);
                    }
                });

        // Custom sound properties
        options.soundVolume = 2.5f;
        options.soundPitch = 0.4f;
        options.departureParticleCount = 200;
        options.arrivalParticleCount = 200;

        // Pre-dash: Create dragon emergence effect
        options.preTeleport = entity -> {
            createDragonEmergenceEffect();
            // Dragon roar
            world.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 3.0f, 0.4f);
        };

        // Post-dash: Massive explosion and ground carving
        options.postTeleport = entity -> {
            createGroundCarvingEffect();
            createMassiveExplosion();

            // Hit all enemies in the large area at destination
            List<LivingEntity> finalTargets = getTargetsInHitbox(entity.position());
            for (LivingEntity target : finalTargets) {
                if (!hitEntities.contains(target)) {
                    hitTargetUltimate(target);
                    hitEntities.add(target);
                }
            }
        };

        // Perform the ultimate dragon dash
        boolean success = TeleportUtil.teleportInDirection(user, dashDistance, options);

        // If dash was blocked, still create effects at current position
        if (!success) {
            createGroundCarvingEffect();
            createMassiveExplosion();
        }
    }

    private void continueFlameWake() {
        // Create continuous flame wake behind the user
        Vec3 userPos = user.position();
        Vec3 behind = userPos.subtract(user.getDeltaMovement().normalize().scale(2));

        createFlameTrail(behind, userPos);

        // Continuous area damage around user
        List<LivingEntity> nearbyTargets = getTargetsInCustomHitbox(
                userPos.add(0, 1, 0), 6.0, 3.0, 6.0);

        for (LivingEntity target : nearbyTargets) {
            if (!hitEntities.contains(target)) {
                hitTargetUltimate(target);
                hitEntities.add(target);
            }
        }
    }

    private void createDragonEmergenceEffect() {
        if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        Vec3 userPos = user.position();
        Vec3 lookDir = user.getLookAngle();

        // Create dragon head shape emerging from user
        for (int i = 0; i < 100; i++) {
            double progress = i / 100.0;
            double dragonWidth = Math.sin(progress * Math.PI) * 3; // Dragon head shape
            double dragonHeight = Math.sin(progress * Math.PI * 0.5) * 2;

            Vec3 spinePos = userPos.add(lookDir.scale(progress * 8))
                    .add(0, 2 + dragonHeight, 0);

            // Dragon spine
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    spinePos.x, spinePos.y, spinePos.z,
                    5, 0.2, 0.2, 0.2, 0.1);

            // Dragon sides
            Vec3 sideDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();
            for (int side = -1; side <= 1; side += 2) {
                Vec3 sidePos = spinePos.add(sideDir.scale(side * dragonWidth));
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        sidePos.x, sidePos.y, sidePos.z,
                        3, 0.3, 0.3, 0.3, 0.1);
            }
        }
    }

    private void createGroundCarvingEffect() {
        if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        Vec3 userPos = user.position();
        Vec3 lookDir = user.getLookAngle();

        // Create carved ground effect behind the user
        for (int i = 0; i < 20; i++) {
            Vec3 groundPos = userPos.subtract(lookDir.scale(i));

            // Ground fire
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    groundPos.x, groundPos.y, groundPos.z,
                    10, 1.0, 0.5, 1.0, 0.1);

            // Lava particles for carved ground
            serverLevel.sendParticles(ParticleTypes.LAVA,
                    groundPos.x, groundPos.y, groundPos.z,
                    5, 0.5, 0.2, 0.5, 0.05);
        }
    }

    private void createMassiveExplosion() {
        if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        Vec3 pos = user.position();

        // Multiple explosion particles
        for (int i = 0; i < 5; i++) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    pos.x, pos.y + 1 + i, pos.z,
                    1, 0, 0, 0, 0);
        }

        // Massive flame burst
        createFlameExplosion(pos.add(0, 2, 0), 4.0f);

        // Ground shockwave
        for (int radius = 1; radius <= 10; radius++) {
            createFlameCircle(pos, radius * 2, radius * 4);
        }

        // Explosion sound
        playFlameExplosionSound(pos);
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0f, 0.5f);
    }

    private void hitTargetUltimate(LivingEntity target) {
        // Use base hit method for damage and fire effect
        hitTarget(target);

        // Ultimate-specific effects
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, false));

        // Massive knockback
        Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
        target.push(knockbackDir.x * knockback, 1.0, knockbackDir.z * knockback);

        // Set on fire for much longer
        target.setSecondsOnFire(6);

        // Massive particle explosion per target
        createFlameExplosion(target.position().add(0, 1, 0), 2.0f);
    }

    @Override
    public boolean isExplosiveAttack() {
        return true; // This attack creates massive explosions
    }

    @Override
    protected void onStop() {
        // Ensure invulnerability is removed
        user.setInvulnerable(false);

        // Clear hit entities
        hitEntities.clear();

        // Give user temporary benefits after using ultimate
        user.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2, false, true));
        user.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0, false, true));
    }
}