package com.xirc.nichirin.client;

import com.xirc.nichirin.client.gui.CooldownHUD;
import dev.architectury.event.events.client.ClientGuiEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ClientEventHandler {

    public static void register() {
        // Register HUD render event
        ClientGuiEvent.RENDER_HUD.register((graphics, partialTicks) -> {
            CooldownHUD.render(graphics, partialTicks);
        });
    }
}