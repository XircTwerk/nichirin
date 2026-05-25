package com.xirc.nichirin.mixin.client;

import com.mojang.authlib.GameProfile;
import com.xirc.nichirin.client.animation.NichirinPlayerModifierLayer;
import com.xirc.nichirin.common.util.INichirinAnimatedPlayer;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin implements INichirinAnimatedPlayer {

    @Unique
    private final NichirinPlayerModifierLayer<IAnimation> nichirin$animLayer = new NichirinPlayerModifierLayer<>();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nichirin$init(ClientLevel world, GameProfile profile, CallbackInfo ci) {
        PlayerAnimationAccess.getPlayerAnimLayer((AbstractClientPlayer) (Object) this)
                .addAnimLayer(1001, nichirin$animLayer);
    }

    @Override
    public ModifierLayer<IAnimation> nichirin_getAnimLayer() {
        return nichirin$animLayer;
    }
}
