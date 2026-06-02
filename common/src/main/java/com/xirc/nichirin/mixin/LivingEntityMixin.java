package com.xirc.nichirin.mixin;

import com.xirc.nichirin.common.util.AttackInterruptTracker;
import com.xirc.nichirin.common.util.NichirinArmorDamage;
import com.xirc.nichirin.common.util.ComboTracker;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.common.effect.ShockedStatusEffect;
import com.xirc.nichirin.common.effect.StunnedStatusEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to detect when stun effects expire and reset combos automatically
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyVariable(method = "hurtArmor", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float nichirin$reduceAttackArmorWear(float damageAmount, DamageSource damageSource) {
        return NichirinArmorDamage.scaleArmorDamage(damageSource, damageAmount);
    }

    @Inject(method = "hurt", at = @At("HEAD"))
    private void nichirin$recordMoveInterruptDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        AttackInterruptTracker.record((LivingEntity) (Object) this, source);
    }

    @Inject(method = "onEffectRemoved", at = @At("HEAD"))
    private void nichirin$onEffectRemoved(MobEffectInstance effect, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (effect.getEffect().value() == NichirinEffectRegistry.stunned().value()) {
            StunnedStatusEffect.removeMovementModifier(entity);
            StunnedStatusEffect.removeKnockbackGrace(entity);

            if (!entity.level().isClientSide && effect.getDuration() <= 0) {
                ComboTracker.handleStunExpired(entity);
            }
        }

        if (!entity.level().isClientSide
                && effect.getEffect().value() == NichirinEffectRegistry.shocked().value()) {
            ShockedStatusEffect.spawnRemovalParticles(entity);
        }
    }
}