package com.xirc.nichirin.client;

import com.xirc.nichirin.client.animation.AnimationRegistryHelper;
import com.xirc.nichirin.client.animation.NichirinAnimations;
import com.xirc.nichirin.client.handler.AttackWheelHandler;
import com.xirc.nichirin.client.handler.BigGuiKeyHandler;
import com.xirc.nichirin.client.handler.ClientDoubleJumpHandler;
import com.xirc.nichirin.client.handler.ComboClientHandler;
import com.xirc.nichirin.client.particle.*;
import com.xirc.nichirin.client.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.client.renderer.BreathingBarRenderer;
import com.xirc.nichirin.client.renderer.StaminaBarRenderer;
import com.xirc.nichirin.client.renderer.StanceBarRenderer;
import com.xirc.nichirin.client.util.ClientInputTracker;
import com.xirc.nichirin.client.util.ItemPropertiesHelper;
import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.event.CooldownClearEventHandler;
import com.xirc.nichirin.common.item.tool.BentoBoxItem;
import com.xirc.nichirin.common.network.CooldownDisplayPacket;
import com.xirc.nichirin.common.util.BlockingInputHandler;
import com.xirc.nichirin.common.util.KatanaInputHandler;
import com.xirc.nichirin.common.util.PlayerStats;
import com.xirc.nichirin.registry.NichirinEntityRendererRegistry;
import com.xirc.nichirin.registry.NichirinItemRegistry;
import com.xirc.nichirin.registry.NichirinParticleRegistry;
import com.xirc.nichirin.registry.NichirinShaderRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import dev.architectury.utils.Env;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class BreathOfNichirinClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(BreathOfNichirinClient.class);
    private static boolean initialized = false;

    private static void registerParticles() {
        try {
            ParticleProviderRegistry.register(NichirinParticleRegistry.THUNDER, ThunderParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.SHOCKWAVE, ShockwaveParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.SOUND, SoundParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.FLASH1, Flash1ParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.FLASH2, Flash2ParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.BLUE_FLASH1, BlueFlash1ParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.BLUE_FLASH2, BlueFlash2ParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.BLUE_SHOCKWAVE, BlueShockwaveParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.BUTTERFLY, ButterflyParticleProvider::new);
            LOGGER.info("Particles registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register particles: {}", e.getMessage());
        }
    }

    public static void init() {
        LOGGER.info("DEBUG: BreathOfNichirinClient.init() called");

        try {
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
            LOGGER.info("DEBUG: About to register katana client handler");
            KatanaInputHandler.registerClient();
            LOGGER.info("DEBUG: Katana client handler registered");

            // Initialize shaders early
            NichirinShaderRegistry.init();
            LOGGER.info("Initialized Nichirin shaders");

            // Register critical systems first
            BlockingInputHandler.register();
            PlayerStats.initialize();
            ItemPropertiesHelper.registerBentoBoxProperty();
            CooldownClearEventHandler.register();
            ComboClientHandler.register();

            // Register renderers AFTER block entities are fully registered
            try {
                LOGGER.info("About to register entity renderers...");
                NichirinEntityRendererRegistry.init();
                LOGGER.info("Entity renderers registered successfully");
            } catch (Exception e) {
                LOGGER.error("ERROR: Failed to initialize renderers", e);
                // Don't rethrow - continue with other initialization
            }

            // Register keybinds
            NichirinKeybindRegistry.register();

            // Register handlers AFTER keybinds
            BigGuiKeyHandler.register();
            LOGGER.info("DEBUG: AttackWheelHandler.register() called");
            AttackWheelHandler.register();
            ClientDoubleJumpHandler.register();
            CooldownDisplayPacket.registerClient();

            // Register animations
            NichirinAnimations.init();
            AnimationRegistryHelper.preloadAnimations();

            // Register other client components
            MoveExecutor.registerClientHandler();

            // Register particles (this might be causing the late registration warnings)
            registerParticles();

            // Register UI renderers
            BreathingBarRenderer.register();
            StaminaBarRenderer.register();
            StanceBarRenderer.register();

            LOGGER.info("DEBUG: Client initialization complete");
            initialized = true;

        } catch (Exception e) {
            LOGGER.error("ERROR: Failed to initialize client", e);
            // Set initialized to true anyway to prevent complete failure
            initialized = true;
        }
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