package com.xirc.nichirin.client;

import com.xirc.nichirin.client.animation.AnimationRegistryHelper;
import com.xirc.nichirin.client.animation.NichirinAnimations;
import com.xirc.nichirin.client.handler.AttackWheelHandler;
import com.xirc.nichirin.client.handler.BigGuiKeyHandler;
import com.xirc.nichirin.client.handler.ClientDoubleJumpHandler;
import com.xirc.nichirin.client.particle.Flash1ParticleProvider;
import com.xirc.nichirin.client.particle.ShockwaveParticleProvider;
import com.xirc.nichirin.client.particle.SoundParticleProvider;
import com.xirc.nichirin.client.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.client.renderer.BreathingBarRenderer;
import com.xirc.nichirin.client.renderer.StaminaBarRenderer;
import com.xirc.nichirin.client.renderer.StanceBarRenderer;
import com.xirc.nichirin.client.util.ClientInputTracker;
import com.xirc.nichirin.client.util.ItemPropertiesHelper;
import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.item.tool.BentoBoxItem;
import com.xirc.nichirin.common.network.CooldownDisplayPacket;
import com.xirc.nichirin.common.util.BlockingInputHandler;
import com.xirc.nichirin.common.util.KatanaInputHandler;
import com.xirc.nichirin.common.util.PlayerStats;
import com.xirc.nichirin.registry.NichirinEntityRendererRegistry;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import com.xirc.nichirin.client.particle.ThunderParticleProvider;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class BreathOfNichirinClient {

    private static boolean initialized = false;

    private static void registerParticles() {
        ParticleProviderRegistry.register(NichirinParticleRegistry.THUNDER, ThunderParticleProvider::new);
        ParticleProviderRegistry.register(NichirinParticleRegistry.SHOCKWAVE, ShockwaveParticleProvider::new);
        ParticleProviderRegistry.register(NichirinParticleRegistry.SOUND, SoundParticleProvider::new);
        ParticleProviderRegistry.register(NichirinParticleRegistry.FLASH1, Flash1ParticleProvider::new);
    }

    public static void init() {
        System.out.println("DEBUG: BreathOfNichirinClient.init() called");

        // Register client tick event to monitor player state
        ClientTickEvent.CLIENT_POST.register(minecraft -> {
            if (minecraft.level != null) {
                // Add input tracking
                ClientInputTracker.tick();

                if (minecraft.level.getGameTime() % 100 == 0) {
                    LocalPlayer player = minecraft.player;
                }
            }
        });

        // Register all client handlers and components
        ClientEventHandler.register();
        System.out.println("DEBUG: About to register katana client handler");
        KatanaInputHandler.registerClient();
        System.out.println("DEBUG: Katana client handler registered");
        BlockingInputHandler.register();
        PlayerStats.initialize();

        // Register keybinds FIRST
        NichirinKeybindRegistry.register();

        //Registries
        NichirinEntityRendererRegistry.init();

        // Register handlers AFTER keybinds
        BigGuiKeyHandler.register();
        System.out.println("DEBUG: About to register attack wheel handler");
        com.xirc.nichirin.client.handler.AttackWheelHandler.register();
        System.out.println("DEBUG: Attack wheel handler registered");
        ClientDoubleJumpHandler.register();
        CooldownDisplayPacket.registerClient();

        // Register animations
        NichirinAnimations.init();
        AnimationRegistryHelper.preloadAnimations();

        // Register other client components
        MoveExecutor.registerClientHandler();
        registerParticles();
        BreathingBarRenderer.register();
        StaminaBarRenderer.register();
        StanceBarRenderer.register();

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