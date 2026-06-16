package com.xirc.nichirin.client;

import com.xirc.nichirin.client.animation.NichirinAnimations;
import com.xirc.nichirin.client.aura.AuraNetworkHandler;
import com.xirc.nichirin.client.aura.EntityAuraTracker;
import com.xirc.nichirin.client.light.WisteriaLightData;
import com.xirc.nichirin.client.outline.OutlineNetworkHandler;
import com.xirc.nichirin.client.outline.OutlineTracker;
import com.xirc.nichirin.common.util.enums.BreathingStyle;
import com.xirc.nichirin.client.config.NichirinClientConfig;
import com.xirc.nichirin.client.handler.*;
import com.xirc.nichirin.client.shader.*;
import dev.architectury.event.events.client.ClientGuiEvent;
import com.xirc.nichirin.client.renderer.armor.ArmorRendererManager;
import com.xirc.nichirin.client.renderer.item.KatanaRendererManager;
import com.xirc.nichirin.common.util.InputHandler;
import com.xirc.nichirin.registry.NichirinKeybindRegistry;
import com.xirc.nichirin.client.renderer.gui.BreathingBarRenderer;
import com.xirc.nichirin.client.renderer.gui.StaminaBarRenderer;
import com.xirc.nichirin.client.renderer.gui.StanceBarRenderer;
import com.xirc.nichirin.client.renderer.effects.AttackHitboxRenderer;
import com.xirc.nichirin.client.util.ClientInputTracker;
import com.xirc.nichirin.client.util.ItemPropertiesHelper;
import com.xirc.nichirin.common.event.system.CooldownClearEventHandler;
import com.xirc.nichirin.client.util.ThunderclapChargeInputHandler;
import com.xirc.nichirin.common.util.BlockingInputHandler;
import com.xirc.nichirin.common.util.PlayerStats;
import com.xirc.nichirin.registry.*;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
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
    private static final int WISTERIA_DAY_LEAF_COLOR = 0xC8A2FF;
    private static final int WISTERIA_NIGHT_LEAF_COLOR = 0x7E3DCC;
    private static long lastWisteriaLeafRefreshTick = -100L;

    // Store shader effect for easy access
    private static DeadCalmShaderEffect deadCalmEffect;
    private static ImpactShakeShaderEffect impactShakeShaderEffect;

    private static void registerShaders() {
        try {
            // Register Dead Calm shader effect
            deadCalmEffect = new DeadCalmShaderEffect();
            impactShakeShaderEffect = ImpactShakeShaderEffect.getInstance();
            NichirinShaderManager.getInstance().register(deadCalmEffect);
            NichirinShaderManager.getInstance().register(impactShakeShaderEffect);

        } catch (Exception e) {
            LOGGER.error("Failed to register shaders: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    private static void registerRenderingHooks() {
        try {
            // Rendering hooks are handled via LevelRendererMixin
            // See: com.xirc.nichirin.mixin.client.LevelRendererMixin

        } catch (Exception e) {
            LOGGER.error("Failed to register rendering hooks: {}", e.getMessage());
        }
    }

    private static void registerBlockColors() {
        ColorHandlerRegistry.registerBlockColors((state, level, pos, tintIndex) -> getWisteriaLeafColor(),
                NichirinBlockRegistry.WISTERIA_LEAVES);
        ColorHandlerRegistry.registerItemColors((stack, tintIndex) -> WISTERIA_DAY_LEAF_COLOR,
                NichirinBlockRegistry.WISTERIA_LEAVES_ITEM);
    }

    private static int getWisteriaLeafColor() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return WISTERIA_DAY_LEAF_COLOR;
        }

        float dayAmount = getDayAmount(minecraft.level.getTimeOfDay(0.0F));
        return blendRgb(WISTERIA_NIGHT_LEAF_COLOR, WISTERIA_DAY_LEAF_COLOR, dayAmount);
    }

    private static float getDayAmount(float timeOfDay) {
        float brightness = ((float) Math.cos(timeOfDay * Math.PI * 2.0F) + 1.0F) * 0.5F;
        return brightness * brightness * (3.0F - 2.0F * brightness);
    }

    private static int blendRgb(int from, int to, float amount) {
        int r = Math.round(channel(from, 16) + (channel(to, 16) - channel(from, 16)) * amount);
        int g = Math.round(channel(from, 8) + (channel(to, 8) - channel(from, 8)) * amount);
        int b = Math.round(channel(from, 0) + (channel(to, 0) - channel(from, 0)) * amount);
        return (r << 16) | (g << 8) | b;
    }

    private static int channel(int color, int shift) {
        return (color >> shift) & 0xFF;
    }

    private static void refreshWisteriaLeafColors(Minecraft minecraft) {
        if (minecraft.level == null
                || minecraft.player == null
                || !WisteriaLightData.hasLights()) {
            return;
        }

        long gameTime = minecraft.level.getGameTime();
        if (gameTime - lastWisteriaLeafRefreshTick < 5L) {
            return;
        }

        lastWisteriaLeafRefreshTick = gameTime;
        var pos = minecraft.player.blockPosition();
        minecraft.levelRenderer.setBlocksDirty(
                pos.getX() - 24,
                pos.getY() - 16,
                pos.getZ() - 24,
                pos.getX() + 24,
                pos.getY() + 16,
                pos.getZ() + 24);
    }

    public static void init() {

        // Register client-only visual config
        NichirinClientConfig.register();

        try {
            // Register armor renderers EARLY - before anything else that might need them
            ArmorRendererManager.registerAll();

            // Register katana item renderers (AzureLib geo models)
            KatanaRendererManager.registerAll();

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
            InputHandler.registerClient();

            // Initialize shaders early
            NichirinShaderRegistry.init();

            // Register post-processing shaders
            registerShaders();

            // Wire up the entity-attached aura system (2D pixelated billboard renderer).
            AuraNetworkHandler.register();
            // Wire up the entity-attached outline system (configurable colour + see-through).
            OutlineNetworkHandler.register();

            // Wire up the Blurry effect screen shader
            MistBlurShaderHandler.register();
            ImpactFrameOverlay.register();
            SunlightVignetteOverlay.register();
            SlammedWobble.register();

            // Register rendering hooks for shaders
            registerRenderingHooks();
            registerBlockColors();

            // Register critical systems first
            BlockingInputHandler.register();
            ThunderclapChargeInputHandler.register();
            BreathingAuraWispHandler.register();
            PlayerStats.initialize();
            ItemPropertiesHelper.registerBentoBoxProperty();
            CooldownClearEventHandler.register();
            ComboClientHandler.register();

            // Register renderers AFTER block entities are fully registered
            try {
                NichirinEntityRendererRegistry.init();
            } catch (Exception e) {
                LOGGER.error("ERROR: Failed to initialize renderers", e);
                // Don't rethrow - continue with other initialization
            }

            // Register handlers AFTER keybinds
            BigGuiKeyHandler.register();
            AttackWheelHandler.register();
            SheathingKeyHandler.register();
            ClientDoubleJumpHandler.register();
            // cooldown_display is now registered in NichirinPacketRegistry (both sides) so the
            // server can send it; registering it again here would double-register the payload.

            // Register animations
            NichirinAnimations.init();

            // Register UI renderers
            BreathingBarRenderer.register();
            StaminaBarRenderer.register();
            StanceBarRenderer.register();
            AttackHitboxRenderer.init();

            // Register blood moon screen overlay
            ClientGuiEvent.RENDER_HUD.register((graphics, partialTicks) -> {
                Minecraft mc = Minecraft.getInstance();
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
                    NichirinBlockRegistry.WISTERIA_DOOR.get(),
                    NichirinBlockRegistry.WISTERIA_TRAPDOOR.get(),
                    NichirinBlockRegistry.WISTERIA_SAPLING.get());

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
                    WisteriaLightData.tick(minecraft);
                    ImpactFrameOverlay.tick();
                    ImpactCameraShake.tick();
                    refreshWisteriaLeafColors(minecraft);
                    EntityAuraTracker.tick();
                    OutlineTracker.tick();

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
        BreathingStyle style = BreathingStyle.fromMovesetId(styleId);
        return style != null ? style.getColorAsFloat() : new float[]{1.0f, 0.8f, 0.4f};
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
        return
                minecraft != null &&
                minecraft.level != null &&
                minecraft.player != null &&
                !minecraft.player.isRemoved();
    }
}
