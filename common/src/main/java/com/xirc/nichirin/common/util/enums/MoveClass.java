package com.xirc.nichirin.common.util.enums;

import lombok.Getter;

@Getter
public enum MoveClass {
    BASIC(),
    SPECIAL1(),
    SPECIAL2(),
    SPECIAL3(),
    SPECIAL4(),
    ULTIMATE();

    private final String name;

    MoveClass() {
        this.name = name().toLowerCase();
    }
}