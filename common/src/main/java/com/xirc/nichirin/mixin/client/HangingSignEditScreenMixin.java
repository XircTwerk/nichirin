package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.common.util.SignTexturePaths;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla builds the hanging-sign editor GUI texture as
 * {@code ResourceLocation.withDefaultNamespace("textures/gui/hanging_signs/" + woodType.name() + ".png")}.
 * A namespaced wood type (e.g. {@code "nichirin:wisteria"}) leaves a ':' in the path, which fails
 * path validation and throws when the screen opens. Rebuild it as
 * {@code <namespace>:textures/gui/hanging_signs/<name>.png} so modded hanging signs open and find
 * their texture; vanilla wood types (no ':') fall through unchanged.
 */
@Mixin(HangingSignEditScreen.class)
public class HangingSignEditScreenMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/resources/ResourceLocation;withDefaultNamespace(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation nichirin$namespacedHangingSignTexture(String path) {
        return SignTexturePaths.namespaced(path);
    }
}
