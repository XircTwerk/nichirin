package com.xirc.nichirin;

import com.xirc.nichirin.common.advancement.NichirinCriteriaTriggers;
import com.xirc.nichirin.registry.MovesetRegistry;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import com.xirc.nichirin.common.event.*;
import com.xirc.nichirin.common.handler.FallDamageHandler;
import com.xirc.nichirin.common.handler.PlayerTickHandler;
import com.xirc.nichirin.common.util.InputHandler;
import com.xirc.nichirin.registry.*;
import dev.architectury.registry.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BreathOfNichirin {
    public static final String MOD_ID = "nichirin";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB_REGISTRY = DeferredRegister.create(MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static void init() {
        LOGGER.info("=== STARTING NICHIRIN COMMON INITIALIZATION ===");

        // Initialize common registries first
        NichirinItemRegistry.init();
        NichirinCreativeTabRegistry.init();
        NichirinBlockRegistry.register();
        NichirinBlockEntityRegistry.register();
        NichirinMoveRegistry.init();
        NichirinEntityRegistry.init();
        NichirinPacketRegistry.init();
        NichirinParticleRegistry.init();
        NicirinSoundRegistry.init();
        NichirinEffectRegistry.init();
        NichirinCommandRegistry.init();
        NichirinStructureTypeRegistry.init();
        NichirinStructurePieceTypeRegistry.init();
        NichirinFoliagePlacerTypes.register();
        NichirinTrunkPlacerTypes.register();
        NichirinTreeDecoratorTypes.register();

        // Register the registries themselves
        NichirinItemRegistry.ITEM_REGISTRY.register();
        CREATIVE_TAB_REGISTRY.register();

        //unlock handlers
        FlameBreathingUnlockHandler.register();
        InsectBreathingUnlockHandler.register();
        ThunderBreathingUnlockHandler.register();
        SoundBreathingUnlockHandler.register();

        // SERVER-SIDE handlers only
        BreathOfNichirinEventHandler.init();
        InputHandler.register();
        PlayerTickHandler.register();
        FallDamageHandler.register();
        BreathingEventHandler.register();
        StaminaEventHandler.register();
        BlockingEventHandler.register();
        DodgeEventHandler.register();
        DemonFoodHandler.register();

        // Data
        MovesetRegistry.init();
        PlayerDataProvider.register();
        NichirinCriteriaTriggers.init();

        LOGGER.info("=== NICHIRIN COMMON INITIALIZATION COMPLETE ===");
    }

    public static ResourceLocation id(String name) {
        return new ResourceLocation(MOD_ID, name);
    }
}