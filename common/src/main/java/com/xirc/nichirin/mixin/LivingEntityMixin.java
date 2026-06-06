package com.xirc.nichirin.mixin;

import com.xirc.nichirin.common.util.AttackInterruptTracker;
import com.xirc.nichirin.common.util.NichirinArmorDamage;
import com.xirc.nichirin.common.util.ComboTracker;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import com.xirc.nichirin.common.effect.ShockedStatusEffect;
import com.xirc.nichirin.common.effect.SlammedStatusEffect;
import com.xirc.nichirin.common.effect.StunnedStatusEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
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

        if (effect.getEffect().value() == NichirinEffectRegistry.slammed().value()) {
            SlammedStatusEffect.removeMovementModifier(entity);
        }

        if (!entity.level().isClientSide
                && effect.getEffect().value() == NichirinEffectRegistry.shocked().value()) {
            ShockedStatusEffect.spawnRemovalParticles(entity);
        }
    }

    // Force the slammed swim/crawl pose. Vanilla's pose/swimming update runs earlier in tick() and
    // clears it out of water, so we re-assert at TAIL; the swim animation keys off Pose.SWIMMING
    // (isVisuallySwimming), not just the swimming flag.
    @Inject(method = "tick", at = @At("TAIL"))
    private void nichirin$forceSlammedSwimPose(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        // Only players get the forced swim/crawl pose. Forcing Pose.SWIMMING on mobs shrinks their
        // hitbox and renders them prone at foot level, which makes them clip down into the ground.
        if (entity instanceof net.minecraft.world.entity.player.Player
                && entity.hasEffect(NichirinEffectRegistry.slammed())) {
            entity.setSwimming(true);
            entity.setPose(Pose.SWIMMING);
        }
    }
}