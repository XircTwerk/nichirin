package com.xirc.nichirin.common.util.enums;

import lombok.Getter;

@Getter
public enum Gauge {
    STAMINA("Stamina", 0x00FF00, 100.0f),
    BREATHING("Breathing", 0x00BFFF, 100.0f),
    STANCE("Stance", 0xFFFF8C00, 100f);

    private final String displayName;
    private final int color;
    private final float maxValue;

    Gauge(String displayName, int color, float maxValue) {
        this.displayName = displayName;
        this.color = color;
        this.maxValue = maxValue;
    }
}