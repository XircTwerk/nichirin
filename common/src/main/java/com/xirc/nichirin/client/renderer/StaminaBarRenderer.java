package com.xirc.nichirin.client.renderer;

import com.xirc.nichirin.client.gui.StaminaBarHUD;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

@Environment(EnvType.CLIENT)
public class StaminaBarRenderer {

    /**
     * Registers the stamina bar renderer
     */
    public static void register() {
        // Register the render event
        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> {
            renderStaminaBar(graphics, tickDelta);
        });
    }

    /**
     * Renders the stamina bar
     */
    private static void renderStaminaBar(GuiGraphics graphics, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        // Render the stamina bar
        if (StaminaBarHUD.shouldRender()) {
            StaminaBarHUD.render(graphics, partialTicks);
        }
    }
}