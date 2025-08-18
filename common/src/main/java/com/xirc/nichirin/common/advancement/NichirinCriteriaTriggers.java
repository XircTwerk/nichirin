package com.xirc.nichirin.common.advancement;

import net.minecraft.advancements.CriteriaTriggers;

/**
 * Registry for custom advancement triggers
 */
public class NichirinCriteriaTriggers {

    public static ThunderBreathingTrigger THUNDER_BREATHING_TRIGGER;
    public static FlameBreathingTrigger FLAME_BREATHING_TRIGGER;
    public static FirstBreathTrigger FIRST_BREATH_TRIGGER;
    public static InsectBreathingTrigger INSECT_BREATHING_TRIGGER;
    public static SoundBreathingTrigger SOUND_BREATHING_TRIGGER;

    public static void init() {
        THUNDER_BREATHING_TRIGGER = CriteriaTriggers.register(new ThunderBreathingTrigger());
        FLAME_BREATHING_TRIGGER = CriteriaTriggers.register(new FlameBreathingTrigger());
        FIRST_BREATH_TRIGGER = CriteriaTriggers.register(new FirstBreathTrigger());
        INSECT_BREATHING_TRIGGER = CriteriaTriggers.register(new InsectBreathingTrigger());
        SOUND_BREATHING_TRIGGER = CriteriaTriggers.register(new SoundBreathingTrigger());
    }
}