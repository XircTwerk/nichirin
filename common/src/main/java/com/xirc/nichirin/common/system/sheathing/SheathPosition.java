package com.xirc.nichirin.common.system.sheathing;

public enum SheathPosition {
    LEFT_HIP("Left Hip"),
    RIGHT_HIP("Right Hip"),
    BACK("Back"),
    BACK_2("Back 2");

    private final String displayName;

    SheathPosition(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
