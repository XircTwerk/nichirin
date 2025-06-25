package com.xirc.nichirin.mixin;

import com.xirc.nichirin.common.system.slayerabilities.PlayerDoubleJump;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-side mixin to update double jump states for all players
 */
@Mixin(Player.class)
public class PlayerDoubleJumpMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onPlayerTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        PlayerDoubleJump.tickPlayer(player);
    }
}