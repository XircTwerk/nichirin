package com.xirc.nichirin.common.util.enums;

import com.mojang.serialization.Codec;
import lombok.Getter;

@Getter
public enum CooldownType {
    // Breathing Style Cooldowns
    BASIC(5),
    SPECIAL1(80),
    SPECIAL2(80),
    SPECIAL3(80),
    SPECIAL4(120),
    ULTIMATE(240, true, true),

    // Universal Cooldowns
    UTILITY(Category.UNIVERSAL, 1),
    DASH(Category.UNIVERSAL, 1, true, true);

    public static final Codec<CooldownType> CODEC = Codec.STRING.xmap(
            name -> CooldownType.valueOf(name.toUpperCase()),
            type -> type.name().toLowerCase()
    );

    private final Category category;
    private final int duration;
    private final boolean nonResettable;
    private final boolean overrideNoCooldowns;

    CooldownType(int duration) {
        this(Category.BREATHING, duration, false, false);
    }

    CooldownType(int duration, boolean nonResettable, boolean overrideNoCooldowns) {
        this(Category.BREATHING, duration, nonResettable, overrideNoCooldowns);
    }

    CooldownType(Category category, int duration) {
        this(category, duration, false, false);
    }

    CooldownType(Category category, int duration, boolean nonResettable, boolean overrideNoCooldowns) {
        this.category = category;
        this.duration = duration;
        this.nonResettable = nonResettable;
        this.overrideNoCooldowns = overrideNoCooldowns;
    }

    public enum Category {
        BREATHING, UNIVERSAL
    }
}