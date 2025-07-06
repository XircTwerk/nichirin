package com.xirc.nichirin.common.advancement;

import net.minecraft.advancements.CriteriaTriggers;

/**
 * Registry for custom advancement triggers
 */
public class NichirinCriteriaTriggers {

    public static ThunderBreathingTrigger THUNDER_BREATHING_TRIGGER;

    public static void init() {
        THUNDER_BREATHING_TRIGGER = CriteriaTriggers.register(new ThunderBreathingTrigger());
    }
}