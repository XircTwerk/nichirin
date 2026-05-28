package com.xirc.nichirin.common.attack.moves;

import com.xirc.nichirin.registry.NichirinParticleRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class DrawSlashAttack extends AbstractKatanaAttack {
    private final boolean dash;

    public DrawSlashAttack(int startup, int active, int recovery, int cooldown,
                           float damage, float range, float knockback,
                           float hitboxSize, Vec3 hitboxOffset, int hitStun,
                           SoundEvent startSound, SoundEvent hitSound, boolean dash) {
        super(startup, active, recovery, cooldown, damage, range, knockback,
                hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
        this.dash = dash;
        this.stopAfterFirstHit = false;
    }

    @Override
    protected void onStart(LivingEntity player) {
        if (startSound != null) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    startSound, SoundSource.PLAYERS, 1.0f, dash ? 1.25f : 0.95f);
        }
    }

    @Override
    protected void onActiveStart(LivingEntity player) {
        if (!dash) return;
        Vec3 look = player.getLookAngle();
        player.setDeltaMovement(look.x * 1.8, player.getDeltaMovement().y, look.z * 1.8);
        player.hurtMarked = true;
        player.hasImpulse = true;
    }

    @Override
    protected void onActiveTick(LivingEntity player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        Vec3 center = player.position().add(player.getLookAngle().scale(1.0)).add(0, player.getBbHeight() * 0.55, 0);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 1, 0.15, 0.15, 0.15, 0.0);
    }

    @Override
    protected void onHitTarget(LivingEntity user, LivingEntity target, Level world) {
        if (hitSound != null) {
            world.playSound(null, target.getX(), target.getY(), target.getZ(),
                    hitSound, SoundSource.PLAYERS, 1.0f, 1.05f);
        }
        if (world instanceof ServerLevel level) {
            Vec3 pos = target.position().add(0, target.getBbHeight() * 0.5, 0);
            level.sendParticles(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(),
                    pos.x, pos.y, pos.z, 6, 0.18, 0.18, 0.18, 0.0);
        }
    }

    public static DrawSlashAttack create(float damage, float range, float knockback, int hitStun, int cooldown, boolean dash) {
        return new DrawSlashAttack(0, 6, 4, cooldown, damage, range, knockback,
                1.8f, new Vec3(0, 0, 0.8), hitStun,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG, dash);
    }
}
