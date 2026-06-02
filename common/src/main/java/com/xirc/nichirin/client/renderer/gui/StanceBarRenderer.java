package com.xirc.nichirin.client.renderer.gui;

import com.xirc.nichirin.client.gui.StanceBarHUD;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

@Environment(EnvType.CLIENT)
public class StanceBarRenderer {

    /**
     * Registers the stance bar renderer
     */
    public static void register() {
        // Register the render event
        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> {
            renderStanceBar(graphics, tickDelta.getGameTimeDeltaPartialTick(true));
        });
    }

    /**
     * Renders the orange stance bar
     */
    private static void renderStanceBar(GuiGraphics graphics, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        // Render the stance bar
        if (StanceBarHUD.shouldRender()) {
            StanceBarHUD.render(graphics, partialTicks);
        }
    }
}