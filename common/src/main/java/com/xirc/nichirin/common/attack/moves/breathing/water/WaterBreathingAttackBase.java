package com.xirc.nichirin.common.attack.moves.breathing.water;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.common.entity.npc.BaseBreathingTrainerEntity;
import com.xirc.nichirin.common.vfx.VfxManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

// Base for Water Breathing attacks. All hits apply a slowness effect representing water pressure.
@SuppressWarnings("rawtypes")
public abstract class WaterBreathingAttackBase extends AbstractBreathingAttack<WaterBreathingAttackBase, IBreathingAttacker> {

    private static final int WATER_PARTICLE_COUNT = 20;
    private static final float WATER_PARTICLE_SPREAD = 1.2f;

    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;
        float savedDamage = damage;
        if (user instanceof BaseBreathingTrainerEntity trainer) damage *= trainer.getDifficultyDamageMultiplier();
        super.hitTarget(target);
        damage = savedDamage;
        applyWaterEffect(target);
        playWaterHitSound(target.position());
    }

    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;
        float savedDamage = damage;
        if (user instanceof BaseBreathingTrainerEntity trainer) damage *= trainer.getDifficultyDamageMultiplier();
        super.hitTargetNoImmunity(target);
        damage = savedDamage;
        applyWaterEffect(target);
        playWaterHitSound(target.position());
    }

    protected void applyWaterEffect(LivingEntity target) {
        if (target instanceof Player player && player.isCreative()) return;
        int slownessDurationTicks = Math.max(40, (int)(damage * 2));
        target.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                slownessDurationTicks, 0, false, true));
    }


    protected void playWaterSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
    }

    protected void playWaterHitSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.PLAYER_SPLASH, SoundSource.PLAYERS, 0.8f, 1.3f);
        }
    }

    protected void playWaterSlashSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 1.1f);
        }
    }

    protected void playWaterFlowSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.WATER_AMBIENT, SoundSource.PLAYERS, 1.0f, 0.8f);
        }
    }

    protected void playWaterExplosionSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.2f, 0.9f);
        }
    }

    protected void playWaterVfx(ResourceLocation effectId, Vec3 origin, Vec3 direction, float scale) {
        if (world instanceof ServerLevel serverLevel) {
            VfxManager.playAttached(serverLevel, user, effectId, origin, direction, scale);
        }
    }

    public boolean isWhirlpoolAttack() { return false; }
    public boolean isDashAttack() { return hasDash() || hasTeleport(); }
    public boolean isOmnidirectional() { return false; }
    public boolean isPersistentArea() { return false; }
    public boolean hasDefensiveProperties() { return false; }

    public int getPressureDuration() {
        return Math.max(40, (int)(damage * 2));
    }

    @Override
    protected abstract void onStart();

    @Override
    protected abstract void perform();

    @Override
    protected void onStop() {
        super.onStop();
    }
}
