package com.xirc.nichirin.common.advancement;

import com.xirc.nichirin.BreathOfNichirin;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;

/**
 * Registry for custom advancement triggers
 */
public class NichirinCriteriaTriggers {

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(BreathOfNichirin.MOD_ID, Registries.TRIGGER_TYPE);

    public static final RegistrySupplier<ThunderBreathingTrigger> THUNDER_BREATHING_TRIGGER =
            TRIGGERS.register("thunder_breathing_unlock", ThunderBreathingTrigger::new);
    public static final RegistrySupplier<FlameBreathingTrigger> FLAME_BREATHING_TRIGGER =
            TRIGGERS.register("flame_breathing_unlock", FlameBreathingTrigger::new);
    public static final RegistrySupplier<FirstBreathTrigger> FIRST_BREATH_TRIGGER =
            TRIGGERS.register("first_breath_unlock", FirstBreathTrigger::new);
    public static final RegistrySupplier<InsectBreathingTrigger> INSECT_BREATHING_TRIGGER =
            TRIGGERS.register("insect_breathing_unlock", InsectBreathingTrigger::new);
    public static final RegistrySupplier<SoundBreathingTrigger> SOUND_BREATHING_TRIGGER =
            TRIGGERS.register("sound_breathing_unlock", SoundBreathingTrigger::new);
    public static final RegistrySupplier<WaterBreathingTrigger> WATER_BREATHING_TRIGGER =
            TRIGGERS.register("water_breathing_unlock", WaterBreathingTrigger::new);
    public static final RegistrySupplier<BeastBreathingTrigger> BEAST_BREATHING_TRIGGER =
            TRIGGERS.register("beast_breathing_unlock", BeastBreathingTrigger::new);
    public static final RegistrySupplier<MistBreathingTrigger> MIST_BREATHING_TRIGGER =
            TRIGGERS.register("mist_breathing_unlock", MistBreathingTrigger::new);

    public static void init() {
        TRIGGERS.register();
    }
}