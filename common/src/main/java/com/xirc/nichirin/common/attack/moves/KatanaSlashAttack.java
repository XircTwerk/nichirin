package com.xirc.nichirin.common.attack.moves;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Light katana slash — single hit with sweep particles.
 */
public class KatanaSlashAttack extends AbstractKatanaAttack {

    public KatanaSlashAttack(int startup, int active, int recovery, int cooldown,
                             float damage, float range, float knockback,
                             float hitboxSize, Vec3 hitboxOffset, int hitStun,
                             SoundEvent startSound, SoundEvent hitSound) {
        super(startup, active, recovery, cooldown, damage, range, knockback,
              hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
    }

    @Override
    protected void onStart(LivingEntity player) {
        if (!(player.level() instanceof ServerLevel sl)) return;

        Vec3 userPos = player.position().add(0, player.getBbHeight() * 0.75, 0);
        Vec3 lookDir = player.getLookAngle();
        for (int i = -30; i <= 30; i += 10) {
            double angle = Math.toRadians(i);
            Vec3 offset = lookDir.yRot((float) angle).scale(range);
            Vec3 pos = userPos.add(offset);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }

        if (startSound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    startSound, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        if (hitSound != null) {
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    hitSound, SoundSource.PLAYERS, 1.0f, 1.0f);
        }

        if (world instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    10, 0.2, 0.2, 0.2, 0.1);
        }
    }

    public static class Builder extends AbstractKatanaAttack.Builder<Builder, KatanaSlashAttack> {
        public Builder() {
            startup = 0; active = 7; recovery = 1;
            cooldown = 0; range = 2.5f;
            knockback = 0.3f; hitboxSize = 2.0f; hitStun = 5;
        }

        @Override
        public KatanaSlashAttack build() {
            return new KatanaSlashAttack(startup, active, recovery, cooldown, damage, range,
                    knockback, hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
        }
    }

    public static KatanaSlashAttack createSlash1() {
        return new Builder()
                .withTiming(0, 7, 1)
                .withDamage(4.0f)
                .withHitStun(5)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }

    public static KatanaSlashAttack createSlash2() {
        return new Builder()
                .withTiming(0, 10, 1)
                .withDamage(5.0f)
                .withKnockback(0.5f)
                .withHitStun(5)
                .withSounds(SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG)
                .build();
    }
}
