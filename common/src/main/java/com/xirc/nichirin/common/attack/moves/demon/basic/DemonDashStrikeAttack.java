package com.xirc.nichirin.common.attack.moves.demon.basic;

import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.component.IDemonAttacker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Demon dashing strike: charge forward, clipping enemies along the path for moderate damage,
 * then plant a heavy finishing hitbox at the user's stopping position for big damage.
 *
 * <p>Each path-sample tick hits enemies once per move (tracked by UUID) so an enemy who gets
 * clipped during the dash isn't double-dipped by the finisher unless they line up in it.</p>
 */
public class DemonDashStrikeAttack extends AbstractDemonAttack<DemonDashStrikeAttack, IDemonAttacker> {

    /** Multiplier applied to base damage for per-tick path clips (each is a glancing blow). */
    private static final float PATH_HIT_DAMAGE_MULT = 0.35f;
    /** Multiplier for the finisher hitbox at the dash endpoint (this is the payoff). */
    private static final float FINISHER_DAMAGE_MULT = 2.0f;
    /** How wide the path hitbox is relative to the configured hitbox size. */
    private static final float PATH_RADIUS_MULT = 0.65f;
    /** How wide the finisher hitbox is relative to the configured hitbox size. */
    private static final float FINISHER_RADIUS_MULT = 1.0f;
    /** How many ticks the dash sustains velocity before the finisher fires. */
    private static final int DASH_DURATION_TICKS = 12;

    private boolean dashExecuted = false;
    private boolean punchExecuted = false;
    private Vec3 dashDirection;
    private Vec3 startPosition;
    private final Set<UUID> pathHitTargets = new HashSet<>();

    public DemonDashStrikeAttack() {
        // Configuration comes from moveset
    }

    @Override
    protected void onStart() {
        dashExecuted = false;
        punchExecuted = false;
        pathHitTargets.clear();

        if (user != null) {
            dashDirection = user.getLookAngle().normalize();
            startPosition = user.position();
        }

        // Dash startup sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.6f, 1.5f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide || user == null) return;

        // Execute dash after windup
        if (!dashExecuted && tickCount >= windup) {
            executeDash();
            dashExecuted = true;
        }

        // Sustain dash velocity until the finisher fires, and clip enemies along the path each tick.
        if (dashExecuted && !punchExecuted && dashDirection != null) {
            double dashStrength = 2.4 * statMultiplier;
            Vec3 current = user.getDeltaMovement();
            user.setDeltaMovement(dashDirection.x * dashStrength, current.y, dashDirection.z * dashStrength);
            user.hurtMarked = true;
            sweepPathHitbox();
        }

        // Finisher fires once the dash duration is up.
        if (dashExecuted && !punchExecuted && tickCount >= windup + dashDurationTicks()) {
            executeFinisher();
            punchExecuted = true;
        }
    }

    /**
     * Per-tick path sample: hit anything currently in the user's reach with reduced damage.
     * Each target is recorded so the dash doesn't re-hit them again on subsequent ticks.
     */
    private void sweepPathHitbox() {
        if (user == null) return;

        float pathRadius = Math.max(0.75f, hitboxSize * PATH_RADIUS_MULT);
        List<LivingEntity> targets = getTargetsInCircle(pathRadius, 2);
        if (targets.isEmpty()) return;

        float originalDamage = damage;
        damage = originalDamage * PATH_HIT_DAMAGE_MULT;
        try {
            for (LivingEntity target : targets) {
                if (target == null || !target.isAlive()) continue;
                if (!pathHitTargets.add(target.getUUID())) continue;

                hitTarget(target);

                // Carry-along nudge: gives the visual that the dash "drags" them.
                Vec3 nudge = dashDirection.scale(knockback * 0.3);
                target.setDeltaMovement(target.getDeltaMovement().add(nudge.x, 0.1, nudge.z));
                target.hurtMarked = true;
            }
        } finally {
            damage = originalDamage;
        }

        // Subtle trail particles to mark the swept volume
        createDashTrail();
    }

    private void executeDash() {
        if (user == null || dashDirection == null) return;

        // Apply strong forward momentum for dash
        double dashStrength = 2.4 * statMultiplier; // Strong dash velocity
        Vec3 dashVelocity = dashDirection.scale(dashStrength);

        // Maintain some vertical component if jumping
        double yVelocity = Math.max(user.getDeltaMovement().y, 0.1);
        user.setDeltaMovement(dashVelocity.x, yVelocity, dashVelocity.z);
        user.hasImpulse = true;
        user.hurtMarked = true;

        // Create dash trail effect
        createDashTrail();

        // Dash sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.8f);
    }

    private int dashDurationTicks() {
        return Math.max(1, Math.min(DASH_DURATION_TICKS, duration));
    }

    /**
     * Finisher: a single big hitbox at the user's CURRENT position (i.e. wherever the dash
     * ended), with significantly amplified damage. Enemies caught only by the finisher take
     * the full hit; enemies who already took a path-clip can still get re-hit here because the
     * finisher is the explicit endpoint payoff.
     */
    private void executeFinisher() {
        if (user == null) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        // Bigger, more dramatic impact effect at the stopping point.
        createPunchImpact(userPos);

        float finisherRadius = Math.max(1.0f, hitboxSize * FINISHER_RADIUS_MULT);
        List<LivingEntity> targets = getTargetsInCircle(finisherRadius, 3);

        float originalDamage = damage;
        damage = originalDamage * FINISHER_DAMAGE_MULT;
        try {
            for (LivingEntity target : targets) {
                if (target == null || !target.isAlive()) continue;

                hitTarget(target);

                // Strong knockback in the dash direction.
                Vec3 kb = dashDirection.scale(knockback * 1.4);
                target.setDeltaMovement(target.getDeltaMovement().add(kb.x, 0.4, kb.z));
                target.hurtMarked = true;
                target.hasImpulse = true;

                // Per-target impact sound.
                world.playSound(null, target.getX(), target.getY(), target.getZ(),
                        SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 0.9f);
            }
        } finally {
            damage = originalDamage;
        }

        // Master impact sounds (independent of how many targets we caught).
        world.playSound(null, userPos.x, userPos.y, userPos.z,
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.3f, 0.7f);
        world.playSound(null, userPos.x, userPos.y, userPos.z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private void createDashTrail() {
        if (!(world instanceof ServerLevel sl) || user == null) return;
        Vec3 pos = user.position().add(0, user.getBbHeight() * 0.5, 0);
        sl.sendParticles(ParticleTypes.CLOUD,
                pos.x, pos.y, pos.z, 3, 0.2, 0.2, 0.2, 0.05);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                pos.x, pos.y, pos.z, 2, 0.15, 0.15, 0.15, 0.02);
    }

    private void createPunchImpact(Vec3 punchPos) {
        if (!(world instanceof ServerLevel sl) || dashDirection == null) return;

        // Central explosion
        sl.sendParticles(ParticleTypes.EXPLOSION,
                punchPos.x, punchPos.y, punchPos.z, 5, 0.3, 0.3, 0.3, 0.05);

        // Radial shockwave ring (8 directions, every 45°)
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45.0);
            Vec3 radial = new Vec3(Math.cos(angle) * 1.5, 0, Math.sin(angle) * 1.5);
            Vec3 pos = punchPos.add(radial);
            sl.sendParticles(ParticleTypes.CRIT,
                    pos.x, pos.y, pos.z, 2, 0.1, 0.1, 0.1, 0.05);
        }

        // Impact plane sweep (damage indicator along right vector)
        Vec3 right = new Vec3(-dashDirection.z, 0, dashDirection.x).normalize();
        for (int i = -3; i <= 3; i++) {
            Vec3 pos = punchPos.add(right.scale(i * 0.4));
            sl.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.02);
        }

        // Ground poof
        sl.sendParticles(ParticleTypes.POOF,
                punchPos.x, punchPos.y - 1.0, punchPos.z, 8, 0.5, 0.05, 0.5, 0.05);
    }

    @Override
    protected void onStop() {
        if (world instanceof ServerLevel sl && user != null) {
            sl.sendParticles(ParticleTypes.CLOUD,
                    user.getX(), user.getY() + user.getBbHeight() * 0.5, user.getZ(),
                    6, 0.4, 0.2, 0.4, 0.1);
        }
        dashExecuted = false;
        punchExecuted = false;
        dashDirection = null;
        startPosition = null;
        pathHitTargets.clear();
    }
}
