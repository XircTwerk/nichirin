package com.xirc.nichirin.client;

import com.xirc.nichirin.client.animation.NichirinAnimations;
import com.xirc.nichirin.client.handler.AttackWheelHandler;
import com.xirc.nichirin.client.handler.BigGuiKeyHandler;
import com.xirc.nichirin.client.handler.ClientDoubleJumpHandler;
import com.xirc.nichirin.client.handler.ComboClientHandler;
import com.xirc.nichirin.client.particle.*;
import com.xirc.nichirin.client.renderer.armor.ArmorRendererManager;
import com.xirc.nichirin.client.shader.DeadCalmShaderEffect;
import com.xirc.nichirin.client.shader.NichirinShaderManager;
import com.xirc.nichirin.common.util.InputHandler;
import com.xirc.nichirin.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.client.renderer.gui.BreathingBarRenderer;
import com.xirc.nichirin.client.renderer.gui.StaminaBarRenderer;
import com.xirc.nichirin.client.renderer.gui.StanceBarRenderer;
import com.xirc.nichirin.client.renderer.effects.AttackHitboxRenderer;
import com.xirc.nichirin.client.util.ClientInputTracker;
import com.xirc.nichirin.client.util.ItemPropertiesHelper;
import com.xirc.nichirin.common.attack.MoveExecutor;
import com.xirc.nichirin.common.event.system.CooldownClearEventHandler;
import com.xirc.nichirin.common.network.util.CooldownDisplayPacket;
import com.xirc.nichirin.common.util.BlockingInputHandler;
import com.xirc.nichirin.common.util.PlayerStats;
import com.xirc.nichirin.registry.*;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import dev.architectury.registry.client.rendering.RenderTypeRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(EnvType.CLIENT)
public class BreathOfNichirinClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(BreathOfNichirinClient.class);
    private static boolean initialized = false;

    // Store shader effect for easy access
    private static DeadCalmShaderEffect deadCalmEffect;

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

    private static void registerShaders() {
        try {
            // Register Dead Calm shader effect
            deadCalmEffect = new DeadCalmShaderEffect();
            NichirinShaderManager.getInstance().register(deadCalmEffect);

            LOGGER.info("Dead Calm shader registered successfully");
            System.out.println("DEBUG: Dead Calm shader registered!");
        } catch (Exception e) {
            LOGGER.error("Failed to register shaders: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private static void registerRenderingHooks() {
        try {
            // Rendering hooks are handled via LevelRendererMixin
            // See: com.xirc.nichirin.mixin.client.LevelRendererMixin

            LOGGER.info("Rendering hooks will be injected via mixins");
        } catch (Exception e) {
            LOGGER.error("Failed to register rendering hooks: {}", e.getMessage());
        }
    }

    public static void init() {
        LOGGER.info("DEBUG: BreathOfNichirinClient.init() called");

        try {
            // Register armor renderers EARLY - before anything else that might need them
            LOGGER.info("Registering armor renderers...");
            ArmorRendererManager.registerAll();
            LOGGER.info("Armor renderers registered successfully");

            // Register client tick event to monitor player state
            ClientTickEvent.CLIENT_POST.register(minecraft -> {
                if (minecraft.level != null) {
                    // Add input tracking
                    ClientInputTracker.tick();

                    // Tick shader skybox renderers
                    if (deadCalmEffect != null && deadCalmEffect.getSkyboxRenderer().isActive()) {
                        deadCalmEffect.getSkyboxRenderer().tick();
                    }

                    if (minecraft.level.getGameTime() % 100 == 0) {
                        LocalPlayer player = minecraft.player;
                    }
                }
            });

            // Register all client handlers and components
            ClientEventHandler.register();
            LOGGER.info("DEBUG: About to register katana client handler");
            InputHandler.registerClient();
            LOGGER.info("DEBUG: Katana client handler registered");

            // Initialize shaders early
            NichirinShaderRegistry.init();
            LOGGER.info("Initialized Nichirin shaders");

            // Register post-processing shaders
            registerShaders();

            // Register rendering hooks for shaders
            registerRenderingHooks();

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

            // Register other client components
            MoveExecutor.registerClientHandler();

            registerParticles();

            // Register UI renderers
            BreathingBarRenderer.register();
            StaminaBarRenderer.register();
            StanceBarRenderer.register();
            AttackHitboxRenderer.init();

            RenderTypeRegistry.register(RenderType.cutout(),
                    NichirinBlockRegistry.WYSTERIA_DOOR.get(),
                    NichirinBlockRegistry.WYSTERIA_TRAPDOOR.get(),
                    NichirinBlockRegistry.WYSTERIA_SAPLING.get());

            LOGGER.info("DEBUG: Client initialization complete");
            initialized = true;

            ClientTickEvent.CLIENT_POST.register(minecraft -> {
                if (minecraft.level != null) {
                    // Add input tracking
                    ClientInputTracker.tick();

                    // Tick shader skybox and block renderers
                    if (deadCalmEffect != null) {
                        if (deadCalmEffect.getSkyboxRenderer().isActive()) {
                            deadCalmEffect.getSkyboxRenderer().tick();
                        }
                        if (deadCalmEffect.getBlockRenderer().isActive()) {
                            deadCalmEffect.getBlockRenderer().tick();
                        }
                    }

                    if (minecraft.level.getGameTime() % 100 == 0) {
                        LocalPlayer player = minecraft.player;
                    }
                }
            });

        } catch (Exception e) {
            LOGGER.error("ERROR: Failed to initialize client", e);
            // Set initialized to true anyway to prevent complete failure
            initialized = true;
        }
    }

    /**
     * Get the Dead Calm shader effect instance
     */
    public static DeadCalmShaderEffect getDeadCalmEffect() {
        return deadCalmEffect;
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