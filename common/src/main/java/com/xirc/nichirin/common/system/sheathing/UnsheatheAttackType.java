package com.xirc.nichirin.common.system.sheathing;

public enum UnsheatheAttackType {
    QUICKDRAW_SLASH("Quickdraw Slash"),
    REVERSE_DRAW_SLASH("Reverse Draw Slash"),
    OVERHEAD_BACKDRAW("Overhead Backdraw"),
    DIAGONAL_BACKDRAW("Diagonal Backdraw"),
    SPRINTING_DRAW_DASH("Sprinting Draw Dash"),
    CROUCHING_LOW_DRAW("Crouching Low Draw"),
    AERIAL_DRAW_SLASH("Aerial Draw Slash"),
    CHARGED_DRAW_SLASH("Charged Draw Slash"),
    DUAL_CROSS_SLASH("Dual Cross Slash");

    private final String displayName;

    UnsheatheAttackType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
