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

    /**
     * The adjacent slot in the same pair. LEFT_HIP↔RIGHT_HIP and BACK↔BACK_2 form the two
     * valid "dual katana" pairs — sheathing a dual katana fills one slot here and marks the
     * other as occupied so a second sword can't squeeze in.
     */
    public SheathPosition adjacent() {
        return switch (this) {
            case LEFT_HIP -> RIGHT_HIP;
            case RIGHT_HIP -> LEFT_HIP;
            case BACK -> BACK_2;
            case BACK_2 -> BACK;
        };
    }
}