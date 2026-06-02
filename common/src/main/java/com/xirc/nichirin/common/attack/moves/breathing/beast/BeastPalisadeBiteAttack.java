package com.xirc.nichirin.common.attack.moves.breathing.beast;

import com.xirc.nichirin.common.util.HitboxData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// Sixth Fang: Palisade Bite. Four wide slashes advancing forward in alternating left/right sweeps.
public class BeastPalisadeBiteAttack extends BeastBreathingAttackBase {

    private static final int SLASH_COUNT = 4;
    private static final int TICKS_PER_SLASH = 4;

    @Override
    protected void onStart() {
        playSlashSound();
    }

    @Override
    protected void perform() {
        if (world.isClientSide) return;

        int slashIndex = (tickCount - 1) / TICKS_PER_SLASH;
        int tickInSlash = (tickCount - 1) % TICKS_PER_SLASH;

        if (slashIndex >= SLASH_COUNT) return;

        if (tickInSlash == 0) {
            executeWideSlash(slashIndex);
        }
    }

    private void executeWideSlash(int slashIndex) {
        Vec3 origin = user.position().add(0, user.getBbHeight() / 2, 0);
        Vec3 look = user.getLookAngle();
        Vec3 perp = new Vec3(-look.z, 0, look.x).normalize();

        float baseDistance = 1.5f + slashIndex * 1.2f;
        double perpOffset = (slashIndex % 2 == 0) ? 1.5 : -1.5;

        for (int i = -3; i <= 3; i++) {
            Vec3 center = origin
                    .add(look.scale(baseDistance))
                    .add(perp.scale(perpOffset + i * 0.7));

            List<LivingEntity> targets = getTargetsInCustomHitbox(center, hitboxSize, HitboxData.HitboxShape.CUBE);
            for (LivingEntity target : targets) {
                hitTarget(target);
            }
        }

        createWideSlashEffect(origin, look, perp, baseDistance, perpOffset);
        world.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 0.85f + slashIndex * 0.05f);
    }

    private void createWideSlashEffect(Vec3 origin, Vec3 look, Vec3 perp, float dist, double perpBase) {
        if (!(world instanceof ServerLevel sl)) return;
        for (int i = -5; i <= 5; i++) {
            Vec3 p = origin.add(look.scale(dist)).add(perp.scale(perpBase + i * 0.5));
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0);
            sl.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 2, 0.2, 0.2, 0.2, 0.08);
        }
    }

    @Override
    protected void onStop() {}
}