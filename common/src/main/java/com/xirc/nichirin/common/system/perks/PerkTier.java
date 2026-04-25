package com.xirc.nichirin.common.system.perks;

/**
 * Rarity/tier levels for perks.
 * Higher tiers are more powerful and harder to obtain.
 */
public enum PerkTier {
    COMMON   (0, "Common",    0xFFAAAAAA, 0xFF555555),
    UNCOMMON (1, "Uncommon",  0xFF55FF55, 0xFF006600),
    RARE     (2, "Rare",      0xFF5555FF, 0xFF000099),
    EPIC     (3, "Epic",      0xFFAA00FF, 0xFF550066),
    LEGENDARY(4, "Legendary", 0xFFFFAA00, 0xFF885500);

    public final int level;
    public final String displayName;
    /** Primary border/text colour for the perk slot. */
    public final int primaryColor;
    /** Darker background tint for the slot. */
    public final int bgColor;

    PerkTier(int level, String displayName, int primaryColor, int bgColor) {
        this.level = level;
        this.displayName = displayName;
        this.primaryColor = primaryColor;
        this.bgColor = bgColor;
    }

    public static PerkTier fromLevel(int level) {
        for (PerkTier t : values()) {
            if (t.level == level) return t;
        }
        return COMMON;
    }

    /** Returns the next tier, or null if this is already LEGENDARY. */
    public PerkTier next() {
        int nextLevel = level + 1;
        for (PerkTier t : values()) {
            if (t.level == nextLevel) return t;
        }
        return null;
    }
}
