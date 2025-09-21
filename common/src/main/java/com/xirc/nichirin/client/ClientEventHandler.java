package com.xirc.nichirin.client;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.client.gui.DemonBloodGui;
import com.xirc.nichirin.client.util.KatanaClientHandler;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class ClientEventHandler {

    public static void register() {
        // CRITICAL FIX: Register katana/demon input handler directly
        KatanaClientHandler.registerClientEvents();

        // Register HUD render event
        ClientGuiEvent.RENDER_HUD.register((graphics, partialTicks) -> {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player == null || minecraft.options.hideGui) {
                return;
            }

            // Render cooldown HUD
            CooldownHUD.render(graphics, partialTicks);

            // Render demon blood bar (only for demons not in creative/spectator)
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();
            DemonBloodGui.renderBloodBar(graphics, screenWidth, screenHeight);
        });
    }
}