package com.xirc.nichirin.mixin.client;

import com.xirc.nichirin.common.data.MovesetHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.resources.ResourceLocation;

@Mixin(Gui.class)
public class GuiMixin {

    @Redirect(method = "renderPlayerHealth",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
    private void nichirin$redirectHungerBlits(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();

        // Only render hunger bar blits if not a demon, or if it's not the hunger bar texture region
        boolean isDemon = minecraft.player != null && MovesetHelper.hasDemonMoveset(minecraft.player);
        boolean isHungerBarBlit = v == 27; // Food icons are at v=27 in the GUI_ICONS_LOCATION texture

        // Don't hide hunger bar in creative or spectator mode
        boolean isCreativeOrSpectator = minecraft.player != null &&
                (minecraft.player.isCreative() || minecraft.player.isSpectator());

        if (!isDemon || !isHungerBarBlit || isCreativeOrSpectator) {
            guiGraphics.blit(texture, x, y, u, v, width, height);
        }
    }
}