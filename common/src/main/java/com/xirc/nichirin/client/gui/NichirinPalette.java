package com.xirc.nichirin.client.gui;

/**
 * Central color palette for all Nichirin GUI elements.
 * All GUI files should reference these constants instead of hardcoding colors.
 */
public final class NichirinPalette {

    private NichirinPalette() {}

    // Backgrounds

    /** Full-screen dark overlay. */
    public static final int BG_DARK       = 0xC0101010;
    /** Content panel background. */
    public static final int BG_CONTENT    = 0xB0202020;
    /** Normal selectable box background. */
    public static final int BG_BOX        = 0xFF2A2A2A;
    /** Active / selected box background. */
    public static final int BG_BOX_ACTIVE = 0xFF3A3A3A;
    /** Locked (unavailable) box background. */
    public static final int BG_BOX_LOCKED = 0xFF1A1A1A;

    // Borders

    public static final int BORDER_DEFAULT = 0xFF4A4A4A;
    public static final int BORDER_HOVER   = 0xFF5A5A5A;
    /** Accent border for active / selected elements. */
    public static final int BORDER_ACCENT  = 0xFF55FFFF;
    public static final int BORDER_LOCKED  = 0xFF666666;

    // Text (no alpha component — callers supply alpha where needed)

    public static final int TEXT_WHITE   = 0xFFFFFF;
    public static final int TEXT_MUTED   = 0xAAAAAA;
    public static final int TEXT_LOCKED  = 0x888888;
    /** Cyan accent used for selected / equipped labels. */
    public static final int TEXT_ACCENT  = 0x55FFFF;
    public static final int TEXT_WARNING = 0xFFAA00;
    public static final int TEXT_ERROR   = 0xFF5555;

    // HUD bar fills

    /** Light-blue breathing gauge fill. */
    public static final int BAR_BREATHING          = 0xFF55AAFF;
    public static final int BAR_BREATHING_HIGHLIGHT = 0x0088DDFF;

    /** Gold stamina gauge fill. */
    public static final int BAR_STAMINA            = 0xFFFFD700;
    public static final int BAR_STAMINA_HIGHLIGHT  = 0x00FFFF80;

    /** Orange stance gauge fill. */
    public static final int BAR_STANCE             = 0xFFFF8C00;
    public static final int BAR_STANCE_HIGHLIGHT   = 0x00FFAA00;

    /** Shared bar background and border (semi-transparent / opaque black). */
    public static final int BAR_BG     = 0x80000000;
    public static final int BAR_BORDER = 0xFF000000;
}