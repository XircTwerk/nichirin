package com.xirc.nichirin;

import com.xirc.nichirin.client.BreathOfNichirinClient;
import com.xirc.nichirin.client.animation.NichirinAnimations;
import com.xirc.nichirin.client.renderer.StaminaBarRenderer;
import com.xirc.nichirin.common.event.StaminaEventHandler;
import com.xirc.nichirin.common.registry.*;
import com.xirc.nichirin.common.util.KatanaInputHandler;
import dev.architectury.platform.Platform;
import dev.architectury.registry.registries.DeferredRegister;
import net.fabricmc.api.EnvType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BreathOfNichirin {
    public static final String MOD_ID = "nichirin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB_REGISTRY = DeferredRegister.create(MOD_ID, Registries.CREATIVE_MODE_TAB);

    @SuppressWarnings("CallToPrintStackTrace")
    public static void init() {
        LOGGER.info("=== STARTING NICHIRIN COMMON INITIALIZATION ===");

        // Initialize common registries first (this creates the deferred register entries)
        NichirinItemRegistry.init();
        NichirinCreativeTabRegistry.init();
        OreRegistry.register();

        // Add worldgen initialization here

        // Register the registries themselves
        NichirinItemRegistry.ITEM_REGISTRY.register();
        CREATIVE_TAB_REGISTRY.register();
        NichirinPacketRegistry.init();
        NichirinParticleRegistry.init();
        // Initialize input handler (should be safe for both sides)
        KatanaInputHandler.register();
        StaminaEventHandler.register();

        LOGGER.info("=== NICHIRIN COMMON INITIALIZATION COMPLETE ===");

        // Client-side initialization - do this BEFORE animations
        if (Platform.getEnv() == EnvType.CLIENT) {
            System.out.println("DEBUG: Initializing client side");
            try {
                BreathOfNichirinClient.init();
                System.out.println("DEBUG: Client initialization complete");

                // Only initialize animations AFTER client is ready
                System.out.println("DEBUG: Initializing animations");
                NichirinAnimations.init();
                System.out.println("DEBUG: Animation initialization complete");

            } catch (Exception e) {
                LOGGER.error("ERROR: Failed to initialize client", e);
                e.printStackTrace();
            }
        }
    }
    public static void initClient() {
        // Register client-side renderer
        StaminaBarRenderer.register();
    }
    public static ResourceLocation id(String name) {
        return new ResourceLocation(MOD_ID, name);
    }
}