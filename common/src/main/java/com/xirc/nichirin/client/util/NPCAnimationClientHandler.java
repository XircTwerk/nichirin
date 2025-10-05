package com.xirc.nichirin.client.util;

import com.xirc.nichirin.client.renderer.entity.npc.NPCAnimationManager;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class NPCAnimationClientHandler {

    /**
     * Register NPC animation related client events
     */
    public static void register() {
        // Tick animations every client tick
        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.level != null && client.player != null) {
                NPCAnimationManager.tickAllAnimations();
            }
        });

        // Clean up when joining/leaving worlds
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register((client) -> {
            // Clear animations when joining a new world
            NPCAnimationManager.clearAll();
        });

        // Also clean up on disconnect
        ClientLifecycleEvent.CLIENT_STOPPING.register(client -> {
            NPCAnimationManager.clearAll();
        });
    }
}