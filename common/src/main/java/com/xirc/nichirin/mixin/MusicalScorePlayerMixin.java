package com.xirc.nichirin.mixin;

import com.xirc.nichirin.common.event.MusicalScoreEventHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept player damage at the source level
 * This allows Musical Score to prevent death before it happens
 */
@Mixin(Player.class)
public class MusicalScorePlayerMixin {

    /**
     * Intercept damage to the player and check for Musical Score activation
     */
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void onPlayerHurt(DamageSource damageSource, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player)(Object)this;

        // Only process on server side
        if (!player.level().isClientSide) {
            // Calculate if this damage would be lethal
            float healthAfterDamage = player.getHealth() - amount;

            if (healthAfterDamage <= 0.0f) {
                // This damage would kill the player - check for Musical Score
                boolean saved = MusicalScoreEventHandler.onPlayerDeath(player, damageSource);

                if (saved) {
                    // Musical Score activated - cancel the damage completely
                    cir.setReturnValue(false);
                }
            }
        }
    }
}