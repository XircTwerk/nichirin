package com.xirc.nichirin.common.attack.moves.breathing.mist;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Form 2: 8 rapid slashes bypassing immunity frames. Right Click.
public class EightLayeredMistAttack extends MistBreathingAttackBase {

    private static final int TOTAL_SLASHES = 8;
    private static final int SLASH_INTERVAL = 3; // ticks between each slash

    private int slashesPerformed = 0;

    @Override
    protected void onStart() {
        slashesPerformed = 0;
        createMistParticles();

        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.5f);
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        int ticksSinceWindup = tickCount - windup;

        if (ticksSinceWindup >= 0 && ticksSinceWindup % SLASH_INTERVAL == 0) {
            int slashIndex = ticksSinceWindup / SLASH_INTERVAL;
            if (slashIndex < TOTAL_SLASHES) {
                performSlash(slashIndex);
                slashesPerformed++;
            }
        }
    }

    private void performSlash(int slashIndex) {
        boolean isLeftSlash = slashIndex % 2 == 0;

        createSlashParticles(isLeftSlash);

        Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 lookDir = user.getLookAngle();
        Vec3 rightDir = lookDir.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 slashOffset = rightDir.scale(isLeftSlash ? -0.5 : 0.5);
        Vec3 slashCenter = userPos.add(lookDir.scale(range * 0.8)).add(slashOffset);

        List<LivingEntity> targets = getTargetsInCustomHitbox(slashCenter, hitboxSize, hitboxSize * 1.25, hitboxSize * 0.75);

        for (LivingEntity target : targets) {
            hitTargetNoImmunity(target);

            // fake pressure — barely any displacement
            Vec3 tinyKnockback = target.position().subtract(userPos).normalize().scale(knockback * 0.15f);
            target.push(tinyKnockback.x, 0, tinyKnockback.z);
        }

        float pitch = 1.2f + (slashIndex * 0.08f);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.6f, pitch);
    }

    private void createSlashParticles(boolean isLeftSlash) {
        if (!(world instanceof ServerLevel serverLevel)) return;

        Vec3 userPos = user.position().add(0, user.getBbHeight() * 0.7, 0);
        Vec3 lookDir = user.getLookAngle();

        for (int i = -3; i <= 3; i++) {
            double actualAngle = isLeftSlash ? Math.toRadians(i * 20) : Math.toRadians(-i * 20);
            Vec3 slashDir = lookDir.yRot((float) actualAngle);
            Vec3 particlePos = userPos.add(slashDir.scale(range * 0.85));

            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.05, 0.05, 0.05, 0.02);
            serverLevel.sendParticles(ParticleTypes.WHITE_ASH,
                    particlePos.x, particlePos.y, particlePos.z,
                    1, 0.02, 0.02, 0.02, 0.01);
        }
    }

    @Override
    protected void onStop() {
        if (world instanceof ServerLevel serverLevel) {
            Vec3 userPos = user.position().add(0, user.getBbHeight() / 2, 0);
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    userPos.x, userPos.y, userPos.z, 14, 0.5, 0.5, 0.5, 0.03);
        }
        slashesPerformed = 0;
    }
}
