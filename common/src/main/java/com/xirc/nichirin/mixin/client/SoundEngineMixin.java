package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.client.sound.SoundSpamGuard;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drops near-duplicate sounds before they claim an OpenAL channel — see {@link SoundSpamGuard}.
 */
@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void nichirin$dropDuplicateSpam(SoundInstance sound, CallbackInfo ci) {
        if (!SoundSpamGuard.allow(sound)) {
            ci.cancel();
        }
    }
}
