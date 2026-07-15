package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.common.util.SignTexturePaths;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla builds sign / hanging-sign sprite locations as
 * {@code withDefaultNamespace("entity/signs[/hanging]/" + woodType.name())}. A namespaced wood type
 * (e.g. {@code "nichirin:wisteria"}) would produce an invalid path and throw while {@link Sheets}
 * initialises its material maps. Rebuild the location so the sprite resolves to the mod's namespace.
 */
@Mixin(Sheets.class)
public class SheetsMixin {

    @Redirect(method = {"createSignMaterial", "createHangingSignMaterial"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/resources/ResourceLocation;withDefaultNamespace(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation nichirin$namespacedSignSprite(String path) {
        return SignTexturePaths.namespaced(path);
    }
}
