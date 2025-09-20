package com.xirc.nichirin.common.attack.moves.demon.basic;

import com.xirc.nichirin.common.attack.component.AbstractDemonAttack;
import com.xirc.nichirin.common.attack.component.IDemonAttacker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Demon kick attack - high knockback front push kick
 * High stun, moderate damage, excellent for crowd control
 */
public class DemonKickAttack extends AbstractDemonAttack<DemonKickAttack, IDemonAttacker> {

    private boolean kickExecuted = false;

    public DemonKickAttack() {
        // Configuration comes from moveset
    }

    @Override
    protected void onStart() {
        kickExecuted = false;

        // Wind-up sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 0.7f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        // Execute kick after windup
        if (!kickExecuted && tickCount >= windup) {
            executeKick();
            kickExecuted = true;
        }
    }

    private void executeKick() {
        if (user == null) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();

        // Create kick effect
        createKickEffect(userPos, lookDir);

        // Hit enemies in front
        List<LivingEntity> targets = getTargetsAtRange();

        for (LivingEntity target : targets) {
            // Apply damage
            hitTarget(target);

            // High knockback kick
            Vec3 knockbackDir = target.position().subtract(userPos).normalize();
            Vec3 strongKnockback = knockbackDir.scale(knockback * 1.5); // Extra strong
            target.setDeltaMovement(strongKnockback.x, 0.4, strongKnockback.z);
            target.hurtMarked = true;
            target.hasImpulse = true;

            // Impact sound
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.2f, 0.9f);
        }

        // Main kick sound
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 1.0f, 1.1f);

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.HORSE_STEP, SoundSource.PLAYERS, 1.5f, 0.6f);
    }

    private void createKickEffect(Vec3 userPos, Vec3 lookDir) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 kickPos = userPos.add(lookDir.scale(range * 0.7));

        // Impact burst effect
        serverLevel.sendParticles(ParticleTypes.CLOUD,
                kickPos.x, kickPos.y, kickPos.z,
                15, 0.5, 0.3, 0.5, 0.2);

        // Force lines showing the kick
        Vec3 rightVector = lookDir.cross(new Vec3(0, 1, 0)).normalize();

        for (int i = -2; i <= 2; i++) {
            Vec3 linePos = kickPos.add(rightVector.scale(i * 0.3));

            serverLevel.sendParticles(ParticleTypes.CRIT,
                    linePos.x, linePos.y, linePos.z,
                    3, 0.1, 0.1, 0.1, 0.1);
        }

        // Forward impact waves
        for (int wave = 1; wave <= 3; wave++) {
            Vec3 wavePos = kickPos.add(lookDir.scale(wave * 0.8));

            serverLevel.sendParticles(ParticleTypes.POOF,
                    wavePos.x, wavePos.y, wavePos.z,
                    5, 0.3, 0.2, 0.3, 0.05);
        }

        // Ground impact dust
        Vec3 groundPos = kickPos.add(0, -0.5, 0);
        serverLevel.sendParticles(ParticleTypes.POOF,
                groundPos.x, groundPos.y, groundPos.z,
                10, 0.8, 0.1, 0.8, 0.1);
    }

    @Override
    protected void onStop() {
        kickExecuted = false;

        // Final impact effect
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position();

            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    userPos.x, userPos.y + 0.5, userPos.z,
                    8, 0.4, 0.2, 0.4, 0.1);
        }
    }
}