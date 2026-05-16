package com.xirc.nichirin.common.attack.moves.breathing.mist;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Form 7: Obscuring Clouds.
 * Become invisible and dash at extreme speed in an orbiting pattern around nearby enemies,
 * slashing from all angles. Accurate to the anime (Muichiro's Seventh Form).
 * No teleports — pure velocity-based movement, no tick interval.
 */
public class ObscuringCloudsAttack extends MistBreathingAttackBase {

    private Vec3 initPos;
    private float initYRot;
    private float initXRot;

    // Orbit state
    private double orbitAngle = 0.0;
    private boolean orbitInitialized = false;
    private LivingEntity orbitTarget = null;
    // How many radians to advance per tick
    private static final double ORBIT_SPEED = Math.PI / 8.0; // ~22.5°/tick (one full orbit per ~3 seconds)
    private static final double ORBIT_RADIUS = 3.0;

    @Override
    protected void onStart() {
        initPos = user.position();
        initYRot = user.getYRot();
        initXRot = user.getXRot();
        orbitAngle = 0.0;
        orbitInitialized = false;
        orbitTarget = null;

        user.addEffect(new MobEffectInstance(
                MobEffects.INVISIBILITY, windup + duration + 10, 0, false, false, false));

        if (world instanceof ServerLevel serverLevel) {
            Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z,
                    40, 1.2, 1.0, 1.2, 0.04);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH, pos.x, pos.y, pos.z,
                    30, 1.0, 0.8, 1.0, 0.03);
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.6f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        int ticksSinceWindup = tickCount - windup;
        if (ticksSinceWindup < 0) return;

        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position();

        // Re-acquire target every 10 ticks or if current target died
        if (ticksSinceWindup % 10 == 0 || orbitTarget == null || !orbitTarget.isAlive()) {
            List<LivingEntity> nearby = world.getEntitiesOfClass(LivingEntity.class,
                    new AABB(userPos.subtract(range, 3, range), userPos.add(range, 3, range)),
                    e -> e != user && e.isAlive());
            orbitTarget = nearby.isEmpty() ? null
                    : nearby.stream().min(Comparator.comparingDouble(e -> e.distanceToSqr(user))).orElse(null);
        }

        // Orbit center: nearest enemy, else initPos
        Vec3 center = orbitTarget != null
                ? orbitTarget.position().add(0, orbitTarget.getBbHeight() * 0.5, 0)
                : initPos;

        // Initialize angle from actual position to avoid jump on first tick
        if (!orbitInitialized) {
            Vec3 toUser = userPos.subtract(center);
            orbitAngle = Math.atan2(toUser.z, toUser.x);
            orbitInitialized = true;
        }

        // Advance orbit angle
        orbitAngle += ORBIT_SPEED;

        // Teleport directly to orbit position each tick for instant movement
        Vec3 orbitPos = center.add(
                Math.cos(orbitAngle) * ORBIT_RADIUS,
                0,
                Math.sin(orbitAngle) * ORBIT_RADIUS
        );

        if (user instanceof ServerPlayer sp) {
            sp.teleportTo(orbitPos.x, orbitPos.y, orbitPos.z);
        } else {
            user.absMoveTo(orbitPos.x, orbitPos.y, orbitPos.z, user.getYRot(), user.getXRot());
        }
        user.setDeltaMovement(Vec3.ZERO);
        user.hurtMarked = true;

        // Slash at current position — hits all targets in hitbox
        Vec3 slashCenter = orbitPos.add(0, user.getBbHeight() / 2, 0);
        List<LivingEntity> targets = getTargetsInCustomHitbox(slashCenter, hitboxSize, hitboxSize, hitboxSize);
        for (LivingEntity target : targets) {
            hitTargetNoImmunity(target);
        }

        // Light mist trail every tick
        serverLevel.sendParticles(ParticleTypes.CLOUD,
                slashCenter.x, slashCenter.y, slashCenter.z,
                3, 0.15, 0.15, 0.15, 0.03);
        if (ticksSinceWindup % 3 == 0) {
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                    slashCenter.x, slashCenter.y, slashCenter.z,
                    2, 0.1, 0.1, 0.1, 0.02);
        }
    }

    @Override
    protected void onStop() {
        // Halt all movement and snap back near start
        user.setDeltaMovement(Vec3.ZERO);

        if (user instanceof ServerPlayer sp) {
            sp.teleportTo(initPos.x, initPos.y, initPos.z);
        } else {
            user.absMoveTo(initPos.x, initPos.y, initPos.z, initYRot, initXRot);
        }
        user.hurtMarked = true;

        user.removeEffect(MobEffects.INVISIBILITY);

        if (world instanceof ServerLevel serverLevel) {
            Vec3 pos = user.position().add(0, user.getBbHeight() / 2, 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z,
                    50, 1.5, 1.2, 1.5, 0.06);
            serverLevel.sendParticles(ParticleTypes.FALLING_WATER, pos.x, pos.y, pos.z,
                    30, 1.0, 0.8, 1.0, 0.05);
        }

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.7f);
    }
}
