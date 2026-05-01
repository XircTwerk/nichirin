package com.xirc.nichirin.client;

import com.xirc.nichirin.client.animation.NichirinAnimations;
import com.xirc.nichirin.client.config.NichirinClientConfig;
import com.xirc.nichirin.client.handler.*;
import com.xirc.nichirin.client.shader.FlameBreathingAuraShader;
import com.xirc.nichirin.client.shader.WaterBreathingAuraShader;
import dev.architectury.event.events.client.ClientGuiEvent;
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
    private static FlameBreathingAuraShader flameAuraShader;
    private static WaterBreathingAuraShader waterAuraShader;

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
            ParticleProviderRegistry.register(NichirinParticleRegistry.BLOOD_SPLAT, BloodSplatParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.BREATHING_AURA_WISP, BreathingAuraWispParticleProvider::new);
            ParticleProviderRegistry.register(NichirinParticleRegistry.SLASH_IMPACT_SPARK, SlashImpactSparkParticleProvider::new);
            LOGGER.info("Particles registered successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to register particles: {}", e.getMessage());
        }
    }

    private static void registerShaders() {
        try {
            // Register Dead Calm shader effect
            deadCalmEffect = new DeadCalmShaderEffect();
            flameAuraShader = new FlameBreathingAuraShader();
            waterAuraShader = new WaterBreathingAuraShader();
            NichirinShaderManager.getInstance().register(flameAuraShader);
            NichirinShaderManager.getInstance().register(waterAuraShader);
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

        // Register client-only visual config
        NichirinClientConfig.register();

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

            // Wire up the tick-driven intensity for breathing aura shaders
            BreathingAuraShaderHandler.register();

            // Register rendering hooks for shaders
            registerRenderingHooks();

            // Register critical systems first
            BlockingInputHandler.register();
            BreathingAuraWispHandler.register();
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

            // Register blood moon screen overlay
            ClientGuiEvent.RENDER_HUD.register((graphics, partialTicks) -> {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level == null || mc.options.hideGui) return;
                if (!BloodMoonClientState.isActive()) return;
                long dayTime = mc.level.getDayTime() % 24000L;
                boolean isNight = dayTime > 13000L || dayTime < 1000L;
                if (!isNight) return;
                int screenWidth  = mc.getWindow().getGuiScaledWidth();
                int screenHeight = mc.getWindow().getGuiScaledHeight();
                // Subtle red overlay: ARGB where alpha ~12/255 -> 0x0C in hex
                graphics.fill(0, 0, screenWidth, screenHeight, 0x0CAA0000);
            });

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

    /** Maps a breathing style ID to an RGB color for the aura wisp particle. */
    public static float[] getBreathingStyleColor(String styleId) {
        if (styleId == null) return new float[]{1.0f, 0.8f, 0.4f};
        return switch (styleId) {
            case "flame_breathing"   -> new float[]{1.0f, 0.35f, 0.0f};
            case "water_breathing"   -> new float[]{0.1f, 0.5f, 1.0f};
            case "thunder_breathing" -> new float[]{1.0f, 0.9f, 0.0f};
            case "insect_breathing"  -> new float[]{0.7f, 0.2f, 0.9f};
            case "sound_breathing"   -> new float[]{1.0f, 0.3f, 0.6f};
            default                  -> new float[]{1.0f, 0.8f, 0.4f};
        };
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