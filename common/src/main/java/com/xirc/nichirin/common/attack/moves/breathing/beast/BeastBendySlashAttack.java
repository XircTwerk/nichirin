package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.util.HitboxData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Ninth Fang: Extending Bendy Slash. Long-range forward sweep, covers everything in front in one wide slash.
public class BeastBendySlashAttack extends BeastBreathingAttackBase {

    private boolean slashDone = false;

    @Override
    protected void onStart() {
        slashDone = false;
        playHeavySlashSound();
    }

    @Override
    protected void perform() {
        if (world.isClientSide || slashDone) return;

        executeFrontalSlash();
        slashDone = true;
    }

    private void executeFrontalSlash() {
        Vec3 origin = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 look = user.getLookAngle();

        for (int dist = 1; dist <= (int) range; dist++) {
            Vec3 center = origin.add(look.scale(dist));
            List<LivingEntity> targets = getTargetsInCustomHitbox(center, hitboxSize, HitboxData.HitboxShape.CUBE);
            for (LivingEntity target : targets) {
                hitTarget(target);
            }
        }

        createFrontalSlashEffect(origin, look);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.6f);
    }

    private void createFrontalSlashEffect(Vec3 origin, Vec3 look) {
        if (!(world instanceof ServerLevel sl)) return;
        Vec3 perp = new Vec3(-look.z, 0, look.x).normalize();

        for (int dist = 1; dist <= (int) range; dist++) {
            Vec3 center = origin.add(look.scale(dist));
            for (int i = -3; i <= 3; i++) {
                Vec3 p = center.add(perp.scale(i * 0.8));
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0.05, 0.1, 0.05, 0);
                if (i == 0 || dist % 2 == 0) {
                    sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.05);
                }
            }
        }
    }

    @Override
    protected void onStop() {
        slashDone = false;
    }
}