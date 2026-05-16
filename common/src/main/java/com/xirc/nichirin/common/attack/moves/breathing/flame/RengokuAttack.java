package com.xirc.nichirin.common.attack.moves.breathing.flame;

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

// Ninth Form: Rengoku. High-stance windup into a flaming dragon dash with massive damage.
public class RengokuAttack extends FlameBreathingAttackBase {

    private static final int DASH_DURATION = 20; // Fast dragon dash - 5 ticks
    private static final float DASH_DISTANCE = 24.0f; // 24 block dash distance

    private boolean hasExecuted = false;
    private boolean isDashing = false;
    private int dashTicks = 0;
    private Vec3 dashDirection;
    private final Set<LivingEntity> hitEntities = new HashSet<>();

    public RengokuAttack() {}

    @Override
    protected void onStart() {
        hasExecuted = false;
        isDashing = false;
        dashTicks = 0;
        hitEntities.clear();

        // Set dash direction in onStart, not during perform
        dashDirection = user.getLookAngle().normalize();

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

        // Start the SUPER FAST dragon dash once after windup completes
        if (!hasExecuted && tickCount == windup + 1) {
            // Remove invulnerability now that dash begins
            user.setInvulnerable(false);

            startFastDash();
            hasExecuted = true;
            isDashing = true;
        }

        // Continue fast dashing and hitting enemies along the path
        if (isDashing && dashTicks < DASH_DURATION) {
            continueFastDash();
            dashTicks++;
        }

        // End dash with massive explosion
        if (isDashing && dashTicks >= DASH_DURATION) {
            endFastDash();
            isDashing = false;
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

    private void startFastDash() {
        // Create dragon emergence effect
        createDragonEmergenceEffect();

        // Dragon roar
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 3.0f, 0.4f);

        // Fast dash movement - cover 24 blocks in 5 ticks
        Vec3 dashVelocity = dashDirection.scale(DASH_DISTANCE);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;
    }

    private void continueFastDash() {
        // Maintain fast dash velocity
        Vec3 dashVelocity = dashDirection.scale(DASH_DISTANCE);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;

        // Create intense dragon trail effects
        createIntenseDragonTrailEffect();

        // Hit enemies along the dragon's path with large hitbox
        List<LivingEntity> targets = getTargetsInCustomHitbox(
                user.position().add(0, user.getBbHeight() / 2, 0),
                8.0, 4.0, 8.0); // Large hitbox for ultimate attack

        for (LivingEntity target : targets) {
            if (!hitEntities.contains(target)) {
                hitTargetUltimate(target);
                hitEntities.add(target);
            }
        }

        // Create ground carving effect at current position
        createGroundCarvingEffect();
    }

    private void endFastDash() {
        // Massive explosion at end
        createMassiveExplosion();

        // Hit all enemies in final blast area
        List<LivingEntity> finalTargets = getTargetsInHitbox(user.position());
        for (LivingEntity target : finalTargets) {
            if (!hitEntities.contains(target)) {
                hitTargetUltimate(target);
                hitEntities.add(target);
            }
        }

        // Stop movement
        user.setDeltaMovement(Vec3.ZERO);
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

    private void createIntenseDragonTrailEffect() {
        if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // INTENSE dragon flame trail - much more dramatic than normal attacks
        for (int i = 1; i <= 15; i++) { // Longer trail for dragon
            Vec3 trailPos = userPos.subtract(dashDirection.scale(i * 0.8));

            // Main dragon body flames - much more intense
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    trailPos.x, trailPos.y, trailPos.z,
                    12, 0.8, 0.8, 0.8, 0.3);

            serverLevel.sendParticles(ParticleTypes.FLAME,
                    trailPos.x, trailPos.y + 1, trailPos.z,
                    6, 0.5, 0.5, 0.5, 0.2);

            // Explosion particles for dragon power
            if (i % 3 == 0) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        trailPos.x, trailPos.y, trailPos.z,
                        1, 0, 0, 0, 0);
            }
        }

        // Dragon wings effect
        Vec3 rightDir = dashDirection.cross(new Vec3(0, 1, 0)).normalize();
        for (int side = -1; side <= 1; side += 2) {
            Vec3 wingPos = userPos.add(rightDir.scale(side * 3.0));
            serverLevel.sendParticles(ParticleTypes.FLAME,
                    wingPos.x, wingPos.y, wingPos.z,
                    10, 0.6, 0.6, 0.6, 0.25);
        }
    }

    private void createGroundCarvingEffect() {
        if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        Vec3 userPos = user.position();

        // Create carved ground effect at current position
        serverLevel.sendParticles(ParticleTypes.FLAME,
                userPos.x, userPos.y, userPos.z,
                15, 1.5, 0.5, 1.5, 0.2);

        // Lava particles for carved ground
        serverLevel.sendParticles(ParticleTypes.LAVA,
                userPos.x, userPos.y, userPos.z,
                8, 0.8, 0.3, 0.8, 0.1);

        // Ground explosion
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                userPos.x, userPos.y, userPos.z,
                1, 0, 0, 0, 0);
    }

    private void createMassiveExplosion() {
        if (!(world instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        Vec3 pos = user.position();

        // Multiple explosion particles
        for (int i = 0; i < 8; i++) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    pos.x, pos.y + 1 + i, pos.z,
                    1, 0, 0, 0, 0);
        }

        // Massive flame burst
        createFlameExplosion(pos.add(0, 2, 0), 5.0f); // Bigger explosion

        // Ground shockwave
        for (int radius = 1; radius <= 15; radius++) {
            createFlameCircle(pos, radius * 2, radius * 6);
        }

        // Explosion sound
        playFlameExplosionSound(pos);
        world.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 3.0f, 0.5f);
    }

    private void hitTargetUltimate(LivingEntity target) {
        // Use base hit method for damage and fire effect - RESPECTS IMMUNITY FRAMES
        hitTarget(target);

        // Ultimate-specific effects
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, false));

        // Massive knockback
        Vec3 knockbackDir = target.position().subtract(user.position()).normalize();
        target.push(knockbackDir.x * knockback, 1.0, knockbackDir.z * knockback);

        // Set on fire for much longer
        target.setSecondsOnFire(8); // Longer fire for ultimate

        // Massive particle explosion per target
        createFlameExplosion(target.position().add(0, 1, 0), 3.0f);
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