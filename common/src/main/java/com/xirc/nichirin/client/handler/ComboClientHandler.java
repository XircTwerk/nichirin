package com.xirc.nichirin.client.handler;

import com.xirc.nichirin.client.gui.ComboHUD;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;

/**
 * Client-side event handler for combo HUD rendering
 */
public class ComboClientHandler {

    /**
     * Register client-side combo events
     */
    public static void register() {
        // Register HUD rendering
        ClientGuiEvent.RENDER_HUD.register((guiGraphics, partialTick) -> {
            Minecraft mc = Minecraft.getInstance();

            // Don't render HUD if:
            // - No player
            // - Any screen is open (including pause menu)
            // - Debug screen is showing
            if (mc.player == null || mc.screen != null || mc.getDebugOverlay().showDebugScreen()) {
                return;
            }

            // Render combo HUD
            ComboHUD.render(guiGraphics);
        });
    }
}