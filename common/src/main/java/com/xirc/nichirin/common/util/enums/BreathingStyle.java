package com.xirc.nichirin.common.util.enums;

import com.xirc.nichirin.common.attack.component.IBreathingAttacker;
import lombok.Getter;
import net.minecraft.world.entity.LivingEntity;

@Getter
public enum BreathingStyle {
    WATER   ("water_breathing",   "Water Breathing",   0x0D80FF),
    FLAME   ("flame_breathing",   "Flame Breathing",   0xFF3800),
    THUNDER ("thunder_breathing", "Thunder Breathing", 0xFFE000),
    MIST    ("mist_breathing",    "Mist Breathing",    0x8CCCFF),
    INSECT  ("insect_breathing",  "Insect Breathing",  0xA61AE6),
    SOUND   ("sound_breathing",   "Sound Breathing",   0xFF33A6),
    BEAST   ("beast_breathing",   "Beast Breathing",   0x59A6E6),
    WIND    ("wind_breathing",    "Wind Breathing",    0x98FB98),
    STONE   ("stone_breathing",   "Stone Breathing",   0x808080),
    LOVE    ("love_breathing",    "Love Breathing",    0xFF69B4),
    SERPENT ("serpent_breathing", "Serpent Breathing", 0x8B008B),
    MOON    ("moon_breathing",    "Moon Breathing",    0x191970),
    SUN     ("sun_breathing",     "Sun Breathing",     0xFFA500);

    private final String movesetId;
    private final String displayName;
    private final int color;

    BreathingStyle(String movesetId, String displayName, int color) {
        this.movesetId = movesetId;
        this.displayName = displayName;
        this.color = color;
    }

    /** Returns the color as a float[3] RGB array (0.0–1.0 each). */
    public float[] getColorAsFloat() {
        return new float[]{
            ((color >> 16) & 0xFF) / 255f,
            ((color >>  8) & 0xFF) / 255f,
            ( color        & 0xFF) / 255f
        };
    }

    /** Returns the BreathingStyle whose movesetId matches, or {@code null} if unknown. */
    public static BreathingStyle fromMovesetId(String id) {
        if (id == null) return null;
        for (BreathingStyle style : values()) {
            if (style.movesetId.equals(id)) return style;
        }
        return null;
    }

    public float modifyDamage(float baseDamage, IBreathingAttacker<?, ?> attacker, LivingEntity target) {
        return baseDamage;
    }
}