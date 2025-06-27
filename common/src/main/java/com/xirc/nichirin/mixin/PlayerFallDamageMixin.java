package com.xirc.nichirin.mixin;

import com.xirc.nichirin.common.system.slayerabilities.PlayerDoubleJump;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin to reduce fall damage for players who used double jump
 */
@Mixin(Player.class)
public class PlayerFallDamageMixin {

    @ModifyVariable(method = "hurt", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float reduceDoubleJumpFallDamage(float damage) {
        Player player = (Player) (Object) this;

        // Only modify fall damage
        if (player.getLastDamageSource() != null &&
                player.getLastDamageSource() == player.damageSources().fall()) {

            // Check if player used double jump
            if (PlayerDoubleJump.hasDoubleJumped(player)) {
                float reducedDamage = Math.max(0, damage - 6.0f);

                System.out.println("DEBUG: Reducing fall damage from " + damage + " to " + reducedDamage);

                // Reset the double jump state after using the benefit
                PlayerDoubleJump.resetDoubleJump(player);

                return reducedDamage;
            }
        }

        return damage;
    }
}