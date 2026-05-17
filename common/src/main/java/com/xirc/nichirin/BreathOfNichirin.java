package com.xirc.nichirin;

import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.advancement.NichirinCriteriaTriggers;
import com.xirc.nichirin.common.event.item.DrinkingGourdInteractionHandler;
import com.xirc.nichirin.common.event.system.*;
import com.xirc.nichirin.common.event.*;
import com.xirc.nichirin.common.event.unlock.*;
import com.xirc.nichirin.registry.NichirinMovesetRegistry;
import com.xirc.nichirin.common.data.PlayerDataProvider;
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
        // Register config first so all systems can read from it immediately
        try {
            me.shedaniel.autoconfig.AutoConfig.register(
                    NichirinModConfig.class,
                    me.shedaniel.autoconfig.serializer.GsonConfigSerializer::new);
        } catch (Exception e) {
            LOGGER.warn("Could not register Cloth Config (cloth-config not installed?). Using hardcoded defaults.", e);
        }

        // Initialize common registries first
        NichirinItemRegistry.init();
        NichirinCreativeTabRegistry.init();
        NichirinBlockRegistry.register();
        NichirinBlockEntityRegistry.register();
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
        WaterBreathingUnlockHandler.register();
        BeastBreathingUnlockHandler.register();

        // SERVER-SIDE handlers only
        BreathOfNichirinEventHandler.init();
        InputHandler.register();
        PlayerTickHandler.register();
        FallDamageHandler.register();
        BreathingEventHandler.register();
        StaminaEventHandler.register();
        BlockingEventHandler.register();
        DodgeEventHandler.register();
        PerkDamageHandler.register();
        DemonFoodHandler.register();
        DrinkingGourdInteractionHandler.register();

        // Data
        NichirinMovesetRegistry.init();
        PlayerDataProvider.register();
        NichirinCriteriaTriggers.init();

    }

    public static ResourceLocation id(String name) {
        return new ResourceLocation(MOD_ID, name);
    }
}