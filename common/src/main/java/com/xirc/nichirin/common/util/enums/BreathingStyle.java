package com.xirc.nichirin.common.util.enums;

import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import net.minecraft.world.entity.LivingEntity;

public enum BreathingStyle {
    WATER("Water Breathing", 0x1E90FF),
    FLAME("Flame Breathing", 0xFF4500),
    THUNDER("Thunder Breathing", 0xFFD700),
    WIND("Wind Breathing", 0x98FB98),
    STONE("Stone Breathing", 0x808080),
    LOVE("Love Breathing", 0xFF69B4),
    MIST("Mist Breathing", 0xE0E0E0),
    INSECT("Insect Breathing", 0x9370DB),
    SERPENT("Serpent Breathing", 0x8B008B),
    SOUND("Sound Breathing", 0x4B0082),
    MOON("Moon Breathing", 0x191970),
    SUN("Sun Breathing", 0xFFA500);

    private final String displayName;
    private final int color;

    BreathingStyle(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getColor() {
        return color;
    }

    public float modifyDamage(float baseDamage, IBreathingAttacker<?, ?> attacker, LivingEntity target) {
        // Override for specific breathing style damage modifiers
        return baseDamage;
    }
}