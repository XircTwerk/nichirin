package com.xirc.nichirin.client;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.client.gui.DemonBloodGui;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class ClientEventHandler {

    public static void register() {
        // Register HUD render event
        ClientGuiEvent.RENDER_HUD.register((graphics, partialTicks) -> {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player == null || minecraft.options.hideGui) {
                return;
            }

            // Render cooldown HUD
            CooldownHUD.render(graphics, partialTicks);

            // Render demon blood bar
            int screenWidth = minecraft.getWindow().getGuiScaledWidth();
            int screenHeight = minecraft.getWindow().getGuiScaledHeight();
            DemonBloodGui.renderBloodBar(graphics, screenWidth, screenHeight);
        });

        // Cancel hunger bar rendering for demons
        ClientGuiEvent.RENDER_HUD.register((graphics, partialTicks) -> {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player != null && DemonBloodGui.shouldHideHungerBar(minecraft.player)) {
                // This cancels the hunger bar rendering for demons
                // The actual implementation depends on your modding platform
                // You might need a mixin for this
            }
        });
    }
}