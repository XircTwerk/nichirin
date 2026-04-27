package com.xirc.nichirin.common.attack.moves.breathing.mist;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Form 3: 360° circular slash. Deflects projectiles, brief invuln on active frames.
public class ScatteringMistSplashAttack extends MistBreathingAttackBase {

    private final Set<LivingEntity> hitEnemies = new HashSet<>();
    private boolean invulnerabilityApplied = false;
    private int spinTicks = 0;

    @Override
    protected void onStart() {
        hitEnemies.clear();
        invulnerabilityApplied = false;
        spinTicks = 0;

        createMistParticles();
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 0.9f, 1.3f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        if (!invulnerabilityApplied) {
            user.setInvulnerable(true);
            invulnerabilityApplied = true;
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 0.9f);
        }

        spinTicks++;

        performCircularSlash();
        deflectProjectiles();

        if (spinTicks % 4 == 0) {
            createSpinningMistEffect();
        }
        if (spinTicks % 8 == 0) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.5f, 1.4f);
        }
    }

    private void performCircularSlash() {
        Vec3 userPos = user.position();
        createMistCircle(userPos.add(0, 1, 0), range, 20);

        List<LivingEntity> targets = getTargetsInCircle(range, 12);

        for (LivingEntity target : targets) {
            if (!hitEnemies.contains(target)) {
                hitTarget(target);
                hitEnemies.add(target);

                Vec3 pushDir = target.position().subtract(userPos).normalize();
                target.push(pushDir.x * knockback, 0.15, pushDir.z * knockback);
            }
        }
    }

    private void deflectProjectiles() {
        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        List<Projectile> projectiles = world.getEntitiesOfClass(Projectile.class,
                new AABB(userPos.subtract(range, 2, range), userPos.add(range, 2, range)),
                projectile -> projectile.isAlive() && projectile.getOwner() != user);

        for (Projectile projectile : projectiles) {
            createDeflectEffect(projectile.position());

            if (projectile instanceof AbstractArrow) {
                projectile.discard();
            } else {
                Vec3 reflectDir = projectile.position().subtract(userPos).normalize();
                projectile.setDeltaMovement(reflectDir.scale(1.5));
                projectile.hurtMarked = true;
            }

            world.playSound(null, projectile.getX(), projectile.getY(), projectile.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 1.5f);
        }
    }

    private void createDeflectEffect(Vec3 position) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        serverLevel.sendParticles(ParticleTypes.CLOUD, position.x, position.y, position.z,
                8, 0.3, 0.3, 0.3, 0.05);
        serverLevel.sendParticles(ParticleTypes.CRIT, position.x, position.y, position.z,
                5, 0.2, 0.2, 0.2, 0.2);
        serverLevel.sendParticles(ParticleTypes.WHITE_ASH, position.x, position.y, position.z,
                4, 0.15, 0.15, 0.15, 0.02);
    }

    private void createSpinningMistEffect() {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);

        for (int ring = 0; ring < 2; ring++) {
            float ringRadius = range * (0.5f + ring * 0.5f);
            int particlesInRing = 10 + ring * 6;

            for (int i = 0; i < particlesInRing; i++) {
                double baseAngle = (2 * Math.PI * i) / particlesInRing;
                double spinOffset = spinTicks * 0.25 * (ring % 2 == 0 ? 1 : -1);
                double angle = baseAngle + spinOffset;

                double x = userPos.x + Math.cos(angle) * ringRadius;
                double z = userPos.z + Math.sin(angle) * ringRadius;
                double y = userPos.y + ring * 0.9;

                serverLevel.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.05, 0.05, 0.05, 0.01);
                if (i % 4 == 0) {
                    serverLevel.sendParticles(ParticleTypes.WHITE_ASH, x, y, z, 1, 0.03, 0.03, 0.03, 0.01);
                }
            }
        }

        // Central mist pillar
        serverLevel.sendParticles(ParticleTypes.CLOUD,
                userPos.x, userPos.y, userPos.z, 4, 0.2, 0.8, 0.2, 0.02);
    }

    @Override
    protected void onStop() {
        user.setInvulnerable(false);
        hitEnemies.clear();
        spinTicks = 0;

        // Final expanding mist burst
        createMistCircle(user.position().add(0, 1, 0), range * 1.5f, 24);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.2f);
    }
}
