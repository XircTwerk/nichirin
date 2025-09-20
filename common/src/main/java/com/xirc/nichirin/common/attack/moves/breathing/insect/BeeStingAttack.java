package com.xirc.nichirin.common.attack.moves.breathing.insect;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Second Form: Dance of the Bee Sting – True Flutter
 * The user dashes through enemies in a straight line, piercing each one
 * with quick, successive stabs while leaving a trail of butterflies.
 *
 * Mechanics:
 * - Dash forward up to 12 blocks
 * - Pierces all enemies along the way
 * - Each enemy hit receives light damage
 * - Short cooldown, good mobility + crowd poke
 *
 * All configuration comes from the moveset builder.
 * This class handles only the behavior and visual/audio effects.
 */
public class BeeStingAttack extends InsectBreathingAttackBase {

    private static final int DASH_DURATION = 13;
    private static final float DASH_SPEED_MULTIPLIER = 12.0f;

    private boolean dashStarted = false;
    private Vec3 dashDirection;
    private Vec3 startPosition;
    private Set<LivingEntity> hitEntities = new HashSet<>();

    // Trail effect tracking
    private int trailEffectCounter = 0;

    public BeeStingAttack() {
        // No configuration here - everything comes from moveset
        // All values will be set via configure() method
    }

    @Override
    protected void onStart() {
        dashStarted = false;
        hitEntities.clear();
        trailEffectCounter = 0;

        dashDirection = user.getLookAngle().normalize();
        startPosition = user.position();

        // Bee startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.PLAYERS, 0.8f, 1.5f);

        // Create initial swarm effect around user
        createBeeSwarmEffect();

        // Light charging sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 1.8f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Start dash after windup
        if (!dashStarted && tickCount == windup + 1) {
            startDash();
            dashStarted = true;
        }

        // Continue dash with piercing attacks
        if (dashStarted && tickCount > windup && tickCount <= windup + DASH_DURATION) {
            continueDash();
        }
    }

    private void createBeeSwarmEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Create buzzing bee particles around user
        for (int i = 0; i < 15; i++) {
            double angle = (i / 15.0) * 2 * Math.PI;
            double radius = 1.2 + Math.sin(angle * 4) * 0.3; // Buzzing pattern
            double height = Math.sin(angle * 2) * 0.8;

            double x = userPos.x + Math.cos(angle) * radius;
            double z = userPos.z + Math.sin(angle) * radius;
            double y = userPos.y + height;

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    x, y, z, 1, 0.05, 0.05, 0.05, 0.02);

            if (i % 4 == 0) {
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                        x, y, z, 1, 0.02, 0.02, 0.02, 0.01);
            }
        }

        // Direction indicator
        for (int i = 1; i <= 6; i++) {
            Vec3 dirPos = userPos.add(dashDirection.scale(i * 0.5));
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    dirPos.x, dirPos.y, dirPos.z,
                    2, 0.1, 0.1, 0.1, 0.05);
        }
    }

    private void startDash() {
        // Set user velocity for dash
        Vec3 dashVelocity = dashDirection.scale(dashSpeed);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;
        user.hasImpulse = true;

        // Dash start sound
        playDashSound();

        // Additional bee sounds
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEE_LOOP, SoundSource.PLAYERS, 1.0f, 2.0f);

        // Create initial dash burst
        createDashBurst();
    }

    private void continueDash() {
        // Maintain dash velocity
        Vec3 dashVelocity = dashDirection.scale(dashSpeed);
        user.setDeltaMovement(dashVelocity);
        user.hurtMarked = true;

        // Create continuous trail effects
        createBeeTrail();

        // Increment trail counter for varying effects
        trailEffectCounter++;

        // Pierce enemies along the dash path
        List<LivingEntity> dashTargets = getTargetsInCustomHitbox(
                user.position().add(0, user.getBbHeight() / 2, 0),
                hitboxSize, 2.0, hitboxSize);

        for (LivingEntity target : dashTargets) {
            if (!hitEntities.contains(target)) {
                // Light piercing damage - respects immunity frames
                hitTarget(target);
                hitEntities.add(target);

                // Light forward knockback to keep targets moving
                Vec3 pierceKnockback = dashDirection.scale(knockback * 0.5);
                target.push(pierceKnockback.x, 0.1, pierceKnockback.z);

                // Create piercing impact particles
                createPierceImpactEffect(target.position());

                // Piercing sound
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_WEAK, SoundSource.PLAYERS, 0.6f, 1.8f);
            }
        }
    }

    private void createDashBurst() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Explosion of bee particles at dash start
        serverLevel.sendParticles(ParticleTypes.WITCH,
                userPos.x, userPos.y, userPos.z,
                20, 0.8, 0.8, 0.8, 0.25);

        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                userPos.x, userPos.y, userPos.z,
                10, 0.5, 0.5, 0.5, 0.15);

        // Portal particles for speed effect
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                userPos.x, userPos.y, userPos.z,
                15, 0.6, 0.6, 0.6, 0.2);
    }

    private void createBeeTrail() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        for (int i = 1; i <= 6; i++) {
            Vec3 trailPos = userPos.subtract(dashDirection.scale(i * 0.4));

            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                    trailPos.x, trailPos.y, trailPos.z,
                    2, 0.3, 0.3, 0.3, 0.1);

            serverLevel.sendParticles(NichirinParticleRegistry.BUTTERFLY.get(),
                    trailPos.x, trailPos.y, trailPos.z,
                    1, 0.2, 0.2, 0.2, 0.08);
        }

        // Side bee particles for width effect
        Vec3 rightDir = dashDirection.cross(new Vec3(0, 1, 0)).normalize();
        for (int side = -1; side <= 1; side += 2) {
            Vec3 sidePos = userPos.add(rightDir.scale(side * 0.8));
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    sidePos.x, sidePos.y, sidePos.z,
                    2, 0.15, 0.15, 0.15, 0.06);
        }

        // Occasional butterfly flutter bursts
        if (trailEffectCounter % 5 == 0) {
            createButterflyFlutter(userPos);
        }
    }

    private void createPierceImpactEffect(Vec3 impactPos) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 targetPos = impactPos.add(0, 1, 0);

        // Light piercing effect - not too intense since this hits multiple enemies
        serverLevel.sendParticles(ParticleTypes.CRIT,
                targetPos.x, targetPos.y, targetPos.z,
                8, 0.2, 0.2, 0.2, 0.1);

        // Poison effect
        serverLevel.sendParticles(ParticleTypes.WITCH,
                targetPos.x, targetPos.y, targetPos.z,
                12, 0.3, 0.3, 0.3, 0.15);

        // Small butterfly scatter
        for (int i = 0; i < 4; i++) {
            double angle = (i / 4.0) * 2 * Math.PI;
            double distance = 1.0;

            double x = targetPos.x + Math.cos(angle) * distance;
            double z = targetPos.z + Math.sin(angle) * distance;
            double y = targetPos.y;

            serverLevel.sendParticles(ParticleTypes.PORTAL,
                    x, y, z, 1, 0.1, 0.1, 0.1, 0.05);
        }
    }

    @Override
    public boolean isPiercingAttack() {
        return true; // This attack pierces through multiple enemies
    }

    @Override
    public boolean isDashAttack() {
        return true; // This is a dash attack
    }

    @Override
    protected void onStop() {
        // Stop user movement
        user.setDeltaMovement(Vec3.ZERO);

        // Final bee swarm effect
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

            // Final butterfly cloud
            createButterflyFlutter(userPos);

            // Lingering bee particles
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    userPos.x, userPos.y, userPos.z,
                    25, 1.0, 1.0, 1.0, 0.2);

            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    userPos.x, userPos.y, userPos.z,
                    15, 0.8, 0.8, 0.8, 0.15);
        }

        // Final bee sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.BEE_LOOP, SoundSource.PLAYERS, 0.6f, 1.0f);

        // Clear state
        dashStarted = false;
        hitEntities.clear();
        trailEffectCounter = 0;
    }
}