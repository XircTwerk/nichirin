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
    private final DrawStyle style;

    public enum DrawStyle {
        QUICK,
        REVERSE,
        DIAGONAL,
        LOW,
        AERIAL,
        DUAL
    }

    public DrawSlashAttack(int startup, int active, int recovery, int cooldown,
                           float damage, float range, float knockback,
                           float hitboxSize, Vec3 hitboxOffset, int hitStun,
                           SoundEvent startSound, SoundEvent hitSound, boolean dash) {
        this(startup, active, recovery, cooldown, damage, range, knockback, hitboxSize, hitboxOffset,
                hitStun, startSound, hitSound, dash, DrawStyle.QUICK);
    }

    public DrawSlashAttack(int startup, int active, int recovery, int cooldown,
                           float damage, float range, float knockback,
                           float hitboxSize, Vec3 hitboxOffset, int hitStun,
                           SoundEvent startSound, SoundEvent hitSound, boolean dash, DrawStyle style) {
        super(startup, active, recovery, cooldown, damage, range, knockback,
                hitboxSize, hitboxOffset, hitStun, startSound, hitSound);
        this.dash = dash;
        this.style = style;
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
        Vec3 look = player.getLookAngle();
        Vec3 center = player.position().add(look.scale(1.0))
                .add(0, player.getBbHeight() * particleHeight(), 0);
        switch (style) {
            case REVERSE -> {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 2, 0.45, 0.08, 0.45, 0.0);
                level.sendParticles(ParticleTypes.CRIT, center.x - look.z * 0.7, center.y, center.z + look.x * 0.7, 4, 0.08, 0.08, 0.08, 0.08);
            }
            case DIAGONAL -> {
                for (int i = 0; i < 4; i++) {
                    double offset = (i - 1.5) * 0.28;
                    level.sendParticles(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(),
                            center.x + offset, center.y + offset * 0.8, center.z, 1, 0.08, 0.08, 0.08, 0.0);
                }
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 1, 0.25, 0.25, 0.25, 0.0);
            }
            case LOW -> {
                level.sendParticles(ParticleTypes.CLOUD, center.x, center.y - 0.25, center.z, 5, 0.55, 0.04, 0.55, 0.02);
                level.sendParticles(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(), center.x, center.y, center.z, 2, 0.4, 0.04, 0.4, 0.0);
            }
            case AERIAL -> {
                level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z, 8, 0.15, 0.45, 0.15, 0.12);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y - 0.35, center.z, 2, 0.2, 0.2, 0.2, 0.0);
            }
            case DUAL -> {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y + 0.2, center.z, 2, 0.45, 0.25, 0.45, 0.0);
                level.sendParticles(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(), center.x, center.y, center.z, 10, 0.55, 0.2, 0.55, 0.01);
            }
            default -> {
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 1, 0.15, 0.15, 0.15, 0.0);
                level.sendParticles(NichirinParticleRegistry.SLASH_IMPACT_SPARK.get(), center.x, center.y, center.z, 1, 0.12, 0.12, 0.12, 0.0);
            }
        }
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
                    pos.x, pos.y, pos.z, 7, 0.22, 0.22, 0.22, 0.0);
            if (style == DrawStyle.LOW) {
                level.sendParticles(ParticleTypes.CLOUD, pos.x, target.getY() + 0.1, pos.z, 8, 0.35, 0.03, 0.35, 0.02);
            } else if (style == DrawStyle.DUAL) {
                level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y, pos.z, 12, 0.3, 0.3, 0.3, 0.12);
            }
        }
    }

    private float particleHeight() {
        return switch (style) {
            case LOW -> 0.18f;
            case AERIAL -> 0.85f;
            default -> 0.55f;
        };
    }

    public static DrawSlashAttack create(float damage, float range, float knockback, int hitStun, int cooldown, boolean dash) {
        return createStyled(damage, range, knockback, hitStun, cooldown, dash, DrawStyle.QUICK, 1.8f);
    }

    private static DrawSlashAttack createStyled(float damage, float range, float knockback, int hitStun,
                                                int cooldown, boolean dash, DrawStyle style, float hitboxSize) {
        return new DrawSlashAttack(0, 6, 4, cooldown, damage, range, knockback,
                hitboxSize, new Vec3(0, 0, 0.8), hitStun,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.PLAYER_ATTACK_STRONG, dash, style);
    }

    public static DrawSlashAttack quickdraw() {
        return createStyled(6.0f, 2.8f, 0.55f, 10, 35, false, DrawStyle.QUICK, 1.8f);
    }

    public static DrawSlashAttack reverseDraw() {
        return createStyled(5.5f, 2.4f, 0.95f, 12, 35, false, DrawStyle.REVERSE, 2.2f);
    }

    public static DrawSlashAttack diagonalDraw() {
        return createStyled(6.5f, 3.0f, 0.65f, 11, 35, false, DrawStyle.DIAGONAL, 2.1f);
    }

    public static DrawSlashAttack lowDraw() {
        return createStyled(4.5f, 2.2f, 1.35f, 18, 35, false, DrawStyle.LOW, 1.9f);
    }

    public static DrawSlashAttack aerialDraw() {
        return createStyled(7.0f, 2.7f, 0.8f, 12, 35, false, DrawStyle.AERIAL, 2.0f);
    }

    public static DrawSlashAttack dualCross() {
        return createStyled(9.0f, 3.0f, 1.0f, 16, 35, false, DrawStyle.DUAL, 2.4f);
    }
}
