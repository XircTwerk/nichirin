package com.xirc.nichirin.common.system.perks;

/** Gameplay category tags for perks.  Used for sorting and Resonance detection. */
public enum PerkTag {
    BREATH    ("Breath",    0xFF00AAFF),
    STAMINA   ("Stamina",   0xFFFFDD00),
    COMBAT    ("Combat",    0xFFFF4444),
    MOVEMENT  ("Movement",  0xFF44FF88),
    DEFENSE   ("Defense",   0xFF888888),
    SURVIVAL  ("Survival",  0xFFFF8800),
    SPECIAL   ("Special",   0xFFFF55FF),
    DEMON     ("Demon",     0xFF880000);

    public final String displayName;
    public final int color;

    PerkTag(String displayName, int color) {
        this.displayName = displayName;
        this.color = color;
    }
}
