package com.xirc.nichirin.client.renderer;

import com.xirc.nichirin.client.gui.BreathingBarHUD;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

@Environment(EnvType.CLIENT)
public class BreathingBarRenderer {

    /**
     * Registers the breathing bar renderer
     */
    public static void register() {
        // Register the render event
        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> {
            renderBreathingBar(graphics, tickDelta);
        });
    }

    /**
     * Renders the breathing bar
     */
    private static void renderBreathingBar(GuiGraphics graphics, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        // Render the breathing bar
        if (BreathingBarHUD.shouldRender()) {
            BreathingBarHUD.render(graphics, partialTicks);
        }
    }
}