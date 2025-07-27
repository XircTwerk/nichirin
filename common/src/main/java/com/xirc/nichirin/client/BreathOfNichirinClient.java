package com.xirc.nichirin.client;

import com.xirc.nichirin.client.animation.AnimationRegistryHelper;
import com.xirc.nichirin.client.animation.NichirinAnimations;
import com.xirc.nichirin.client.handler.AttackWheelHandler;
import com.xirc.nichirin.client.handler.BigGuiKeyHandler;
import com.xirc.nichirin.client.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.client.renderer.BreathingBarRenderer;
import com.xirc.nichirin.client.renderer.StaminaBarRenderer;
import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.registry.NichirinEntityRendererRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import com.xirc.nichirin.client.particle.ThunderParticleProvider;

@Environment(EnvType.CLIENT)
public class BreathOfNichirinClient {

    private static boolean initialized = false;

    private static void registerParticles() {
        ParticleProviderRegistry.register(NichirinParticleRegistry.THUNDER, ThunderParticleProvider::new);
    }

    public static void init() {
        System.out.println("DEBUG: BreathOfNichirinClient.init() called");

        // Register client tick event to monitor player state
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            if (minecraft.level != null && minecraft.level.getGameTime() % 100 == 0) {
                LocalPlayer player = minecraft.player;
            }
        });

        // Register all client handlers and components
        ClientEventHandler.register();

        // Register keybinds FIRST
        NichirinKeybindRegistry.init();

        //Registries
        NichirinEntityRendererRegistry.init();

        // Register handlers AFTER keybinds
        BigGuiKeyHandler.register();
        AttackWheelHandler.register();

        // Register animations
        NichirinAnimations.init();
        AnimationRegistryHelper.preloadAnimations();

        // Register other client components
        MoveExecutor.registerClientHandler();
        registerParticles();
        BreathingBarRenderer.register();
        StaminaBarRenderer.register();

        System.out.println("DEBUG: Client initialization complete");
        initialized = true;
    }

    public static boolean isClientReady() {
        if (!initialized) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null &&
                minecraft.level != null &&
                minecraft.player != null &&
                !minecraft.player.isRemoved();
    }
}