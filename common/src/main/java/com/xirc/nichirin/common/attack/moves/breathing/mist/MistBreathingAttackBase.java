package com.xirc.nichirin.common.attack.moves.breathing.mist;

import com.xirc.nichirin.common.attack.component.AbstractBreathingAttack;
import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import com.xirc.nichirin.common.network.s2c.TriggerShaderPacket;
import com.xirc.nichirin.common.vfx.VfxManager;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.registry.NichirinPacketRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

// Base for all Mist Breathing attacks. Provides shared VFX, blur, and hit overrides.
@SuppressWarnings("rawtypes")
public abstract class MistBreathingAttackBase extends AbstractBreathingAttack<MistBreathingAttackBase, IBreathingAttacker> {

    protected void playMistVfx(ResourceLocation effectId, Vec3 origin, Vec3 direction, float scale) {
        if (world instanceof ServerLevel serverLevel) {
            VfxManager.playAttached(serverLevel, user, effectId, origin, direction, scale);
        }
    }

    protected void playMistVfxAt(ResourceLocation effectId, Vec3 origin, Vec3 direction, float scale) {
        if (world instanceof ServerLevel serverLevel) {
            VfxManager.playOwned(serverLevel, user, effectId, origin, direction, scale);
        }
    }

    @Override
    protected void hitTarget(LivingEntity target) {
        if (world.isClientSide) return;
        super.hitTarget(target);
        playMistHitSound(target.position());
    }

    @Override
    protected void hitTargetNoImmunity(LivingEntity target) {
        if (world.isClientSide) return;
        super.hitTargetNoImmunity(target);
        playMistHitSound(target.position());
    }

    protected void applyMistBlur(LivingEntity target, int durationTicks) {
        target.addEffect(new MobEffectInstance(
                NichirinEffectRegistry.blurry(), durationTicks, 0, false, false, false));
        if (target instanceof ServerPlayer player) triggerMistBlur(player);
    }

    private void triggerMistBlur(ServerPlayer player) {
        NichirinPacketRegistry.sendToPlayer(
                new TriggerShaderPacket("com.xirc.nichirin.client.shader.MistBlurShaderEffect", true, 0.85f),
                player);
    }

    protected void playMistSound() {
        if (world != null && user != null) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.7f, 1.4f);
        }
    }

    protected void playMistHitSound(Vec3 position) {
        if (world != null) {
            world.playSound(null, position.x, position.y, position.z,
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.5f, 1.6f);
        }
    }

    protected Vec3 rotateDirection(Vec3 direction, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(
                direction.x * cos - direction.z * sin,
                direction.y,
                direction.x * sin + direction.z * cos
        ).normalize();
    }

    @Override
    protected abstract void onStart();

    @Override
    protected abstract void perform();
}
