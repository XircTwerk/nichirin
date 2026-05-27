package com.xirc.nichirin.mixin;

import com.xirc.nichirin.common.util.AttackInterruptTracker;
import com.xirc.nichirin.common.util.NichirinArmorDamage;
import com.xirc.nichirin.common.util.ComboTracker;
import com.xirc.nichirin.registry.NichirinEffectRegistry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Mixin to detect when stun effects expire and reset combos automatically
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    public abstract Map<MobEffect, MobEffectInstance> getActiveEffectsMap();

    @ModifyVariable(method = "hurtArmor", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float nichirin$reduceAttackArmorWear(float damageAmount, DamageSource damageSource) {
        return NichirinArmorDamage.scaleArmorDamage(damageSource, damageAmount);
    }

    @Inject(method = "hurt", at = @At("HEAD"))
    private void nichirin$recordMoveInterruptDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        AttackInterruptTracker.record((LivingEntity) (Object) this, source);
    }

    /**
     * Hook into the effect removal process to detect when STUNNED effect expires
     * This is called when effects are naturally removed due to duration expiry
     */
    @Inject(method = "removeEffect", at = @At("HEAD"))
    private void onEffectRemoved(MobEffect effect, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Only handle on server side
        if (entity.level().isClientSide) {
            return;
        }

        // Check if the effect being removed is our STUNNED effect
        if (effect == NichirinEffectRegistry.STUNNED.get()) {
            // Get the current effect instance to check if it's expiring naturally
            MobEffectInstance currentEffect = getActiveEffectsMap().get(effect);

            if (currentEffect != null) {
                // Check if the effect is expiring naturally (duration <= 1)
                // We use <= 1 because the effect gets removed when duration reaches 0,
                // but this method is called when duration is 1 (about to become 0)
                if (currentEffect.getDuration() <= 1) {
                    System.out.println("STUNNED effect expiring naturally for " +
                            entity.getName().getString() + " - triggering combo reset");

                    // Call our combo tracker to handle the expiry
                    ComboTracker.handleStunExpired(entity);
                }
            }
        }
    }

    /**
     * Alternative hook - this catches all effect updates including natural expiration
     * This is called every tick for active effects and handles the decrementing
     */
    @Inject(method = "tickEffects", at = @At("TAIL"))
    private void onEffectsTicked(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Only handle on server side
        if (entity.level().isClientSide) {
            return;
        }

        // Check if we have a STUNNED effect that just expired (duration == 0)
        MobEffectInstance stunEffect = getActiveEffectsMap().get(NichirinEffectRegistry.STUNNED.get());

        if (stunEffect != null && stunEffect.getDuration() == 0) {
            System.out.println("STUNNED effect detected at 0 duration for " +
                    entity.getName().getString() + " - triggering combo reset");

            // Call our combo tracker to handle the expiry
            ComboTracker.handleStunExpired(entity);
        }
    }
}
