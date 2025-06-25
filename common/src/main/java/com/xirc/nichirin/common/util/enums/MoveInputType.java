package com.xirc.nichirin.common.util.enums;

import lombok.Getter;

@Getter
public enum MoveInputType {
    BASIC(MoveClass.BASIC),
    SPECIAL1(MoveClass.SPECIAL1),
    SPECIAL2(MoveClass.SPECIAL2),
    SPECIAL3(MoveClass.SPECIAL3),
    SPECIAL4(MoveClass.SPECIAL4),
    ULTIMATE(MoveClass.ULTIMATE);

    private final MoveClass moveClass;

    MoveInputType(MoveClass moveClass) {
        this.moveClass = moveClass;
    }

    public MoveClass getMoveClass() {
        return moveClass;
    }
}