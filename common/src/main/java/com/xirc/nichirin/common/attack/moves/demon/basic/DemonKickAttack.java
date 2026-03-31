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
        if (!(world instanceof ServerLevel sl)) return;

        Vec3 kickCenter = userPos.add(lookDir.scale(range));
        Vec3 right = new Vec3(-lookDir.z, 0, lookDir.x).normalize();

        // Central cloud burst
        sl.sendParticles(ParticleTypes.CLOUD,
                kickCenter.x, kickCenter.y, kickCenter.z, 15, 0.5, 0.3, 0.5, 0.2);

        // Crit arc across the kick sweep
        double[] sweepOffsets = { -1.0, -0.5, 0.0, 0.5, 1.0 };
        for (double offset : sweepOffsets) {
            Vec3 pos = kickCenter.add(right.scale(offset));
            sl.sendParticles(ParticleTypes.CRIT,
                    pos.x, pos.y, pos.z, 3, 0.1, 0.1, 0.1, 0.05);
        }

        // Forward wave poof
        for (double dist : new double[]{ 0.5, 1.0, 1.5 }) {
            Vec3 pos = userPos.add(lookDir.scale(dist)).add(0, 0.3, 0);
            sl.sendParticles(ParticleTypes.POOF,
                    pos.x, pos.y, pos.z, 5, 0.15, 0.1, 0.15, 0.05);
        }

        // Ground impact poof
        sl.sendParticles(ParticleTypes.POOF,
                kickCenter.x, userPos.y - 0.8, kickCenter.z, 10, 0.4, 0.05, 0.4, 0.05);
    }

    @Override
    protected void onStop() {
        if (world instanceof ServerLevel sl && user != null) {
            sl.sendParticles(ParticleTypes.CLOUD,
                    user.getX(), user.getY() + user.getBbHeight() * 0.5, user.getZ(),
                    8, 0.4, 0.2, 0.4, 0.1);
        }
        kickExecuted = false;
    }
}