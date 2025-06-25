package com.xirc.nichirin.common.util.enums;

/**
 * Enum representing different types of attacks that can be performed
 */
public enum AttackType {
    SIMPLE_ATTACK("Simple Attack", "Basic physical attacks using stamina"),
    BREATHING_ATTACK("Breathing Attack", "Attacks that consume breath"),
    BLITZ_ATTACK("Blitz Attack", "Multi-hit attacks with multiple hitboxes");

    private final String displayName;
    private final String description;

    AttackType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}