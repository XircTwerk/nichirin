package com.xirc.nichirin.common.util.enums;

import lombok.Getter;

@Getter
public enum MoveClass {
    BASIC(),
    SPECIAL(),
    ULTIMATE();

    private final String name;

    MoveClass() {
        this.name = name().toLowerCase();
    }
}