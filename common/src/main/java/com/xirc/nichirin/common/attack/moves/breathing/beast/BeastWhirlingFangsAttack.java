package com.xirc.nichirin.common.attack.moves.breathing.beast;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Tenth Fang: Whirling Fangs. Rapid spinning that deflects all projectiles and hits nearby enemies.
public class BeastWhirlingFangsAttack extends BeastBreathingAttackBase {

    private double spinAngle = 0;

    @Override
    protected void onStart() {
        spinAngle = 0;
    }

    @Override
    protected void onActiveStart() {
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.8f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        spinAngle += 45; // 45° per tick = 2 full rotations over 16 ticks

        Vec3 center = user.position().add(0, user.getBbHeight() / 2, 0);

        deflectProjectiles();

        if (tickCount % 2 == 0) {
            List<LivingEntity> targets = getTargetsInCircle(range, 8);
            for (LivingEntity target : targets) {
                hitTarget(target);
            }
        }

        createSpinParticles(center);

        if (tickCount % 4 == 0) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, 1.5f + (tickCount * 0.02f));
        }
    }

    private void deflectProjectiles() {
        AABB searchBox = new AABB(
                user.getX() - range * 1.5, user.getY() - range, user.getZ() - range * 1.5,
                user.getX() + range * 1.5, user.getY() + range, user.getZ() + range * 1.5
        );

        List<AbstractArrow> arrows = world.getEntitiesOfClass(AbstractArrow.class, searchBox,
                a -> a.getOwner() != user);
        for (AbstractArrow arrow : arrows) {
            Vec3 fromUser = arrow.position().subtract(user.position()).normalize();
            arrow.setDeltaMovement(fromUser.scale(1.5).add(0, 0.3, 0));
        }

        List<Projectile> projectiles = world.getEntitiesOfClass(Projectile.class, searchBox,
                p -> p.getOwner() != user);
        for (Projectile proj : projectiles) {
            Vec3 fromUser = proj.position().subtract(user.position()).normalize();
            proj.setDeltaMovement(fromUser.scale(1.5).add(0, 0.3, 0));
        }
    }

    private void createSpinParticles(Vec3 center) {
        if (!(world instanceof ServerLevel sl)) return;

        int steps = 12;
        double r = range * 0.8;
        for (int i = 0; i < steps; i++) {
            double angle = Math.toRadians(spinAngle + (i / (double) steps) * 360);
            double x = center.x + Math.cos(angle) * r;
            double z = center.z + Math.sin(angle) * r;

            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, x, center.y, z, 1, 0.05, 0.1, 0.05, 0);
            sl.sendParticles(ParticleTypes.CLOUD, x, center.y, z, 1, 0.1, 0.1, 0.1, 0.05);
        }
    }

    @Override
    protected void onStop() {
        spinAngle = 0;
    }
}