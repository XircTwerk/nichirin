package com.xirc.nichirin.client;

import com.xirc.nichirin.client.gui.CooldownHUD;
import com.xirc.nichirin.client.gui.CompassNeedleHUD;
import com.xirc.nichirin.client.handler.MistBlurOverlay;
import com.xirc.nichirin.client.shader.NichirinShaderManager;
import com.xirc.nichirin.client.util.ClientInputHandler;
import com.xirc.nichirin.common.system.DemonComponent;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class ClientEventHandler {

    public static void register() {
        ClientInputHandler.registerClientEvents();

        // Register HUD render event
        ClientGuiEvent.RENDER_HUD.register((graphics, partialTicks) -> {
            Minecraft minecraft = Minecraft.getInstance();

            if (minecraft.player == null || minecraft.options.hideGui) {
                return;
            }

            // Render cooldown HUD
            CooldownHUD.render(graphics, partialTicks.getGameTimeDeltaPartialTick(true));
            CompassNeedleHUD.render(graphics);

            // Note: Blood bar is now rendered via mixin - no manual rendering needed
        });

        // Handle player respawn - reset blood display
        ClientPlayerEvent.CLIENT_PLAYER_RESPAWN.register((oldPlayer, newPlayer) -> {
            // Reset client-side blood display when player respawns
            DemonComponent.onPlayerRespawn();
            clearScreenEffects();
        });

        // Clear lingering screen shaders/overlays the moment the player dies.
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            if (minecraft.player != null && !minecraft.player.isAlive()) {
                clearScreenEffects();
            }
        });
    }

    private static void clearScreenEffects() {
        NichirinShaderManager.getInstance().clearAll();
        MistBlurOverlay.clear();
        CompassNeedleHUD.clear();
    }
}
