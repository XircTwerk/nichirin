package com.xirc.nichirin;

import com.xirc.nichirin.client.BreathOfNichirinClient;
import com.xirc.nichirin.client.renderer.BreathingBarRenderer;
import com.xirc.nichirin.client.renderer.StaminaBarRenderer;
import com.xirc.nichirin.common.advancement.NichirinCriteriaTriggers;
import com.xirc.nichirin.common.data.BreathingStyleSyncPacket;
import com.xirc.nichirin.common.data.MovesetRegistry;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.event.BreathingEventHandler;
import com.xirc.nichirin.common.event.StaminaEventHandler;
import com.xirc.nichirin.common.event.ThunderBreathingUnlockHandler;
import com.xirc.nichirin.common.handler.FallDamageHandler;
import com.xirc.nichirin.common.handler.PlayerTickHandler;
import com.xirc.nichirin.common.util.KatanaInputHandler;
import com.xirc.nichirin.common.util.PlayerStats;
import com.xirc.nichirin.registry.*;
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

        // Initialize common registries first
        NichirinItemRegistry.init();
        NichirinCreativeTabRegistry.init();
        NichirinOreRegistry.register();
        NichirinMoveRegistry.init();
        NichirinEntityRegistry.init();
        NichirinPacketRegistry.init();
        NichirinParticleRegistry.init();
        NicirinSoundRegistry.init();
        ThunderBreathingUnlockHandler.register();
        NichirinEffectRegistry.init();
        NichirinCommandRegistry.init();

        // Register the registries themselves
        NichirinItemRegistry.ITEM_REGISTRY.register();
        CREATIVE_TAB_REGISTRY.register();

        //handlers
        PlayerTickHandler.register();
        FallDamageHandler.register();
        KatanaInputHandler.register();
        BreathingEventHandler.register();
        StaminaEventHandler.register();

        //data
        PlayerStats.initialize();
        MovesetRegistry.init();
        PlayerDataProvider.register();
        BreathingStyleSyncPacket.register();
        NichirinCriteriaTriggers.init();


        LOGGER.info("=== NICHIRIN COMMON INITIALIZATION COMPLETE ===");

        // Client-side initialization - ONLY call BreathOfNichirinClient.init()
        if (Platform.getEnv() == EnvType.CLIENT) {
            System.out.println("DEBUG: Initializing client side");
            try {
                BreathOfNichirinClient.init();
                System.out.println("DEBUG: Client initialization complete");
            } catch (Exception e) {
                LOGGER.error("ERROR: Failed to initialize client", e);
                e.printStackTrace();
            }
        }
    }

    public static void initClient() {
        // Register client-side renderer
        BreathingBarRenderer.register();
        StaminaBarRenderer.register();
    }

    public static ResourceLocation id(String name) {
        return new ResourceLocation(MOD_ID, name);
    }
}