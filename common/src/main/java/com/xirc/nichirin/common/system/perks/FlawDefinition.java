package com.xirc.nichirin.common.system.perks;

/**
 * A negative perk (flaw) the player must equip to unlock perk slots past the free slots.
 */
public class FlawDefinition {

    public final String id;
    public final String name;
    public final String description;

    public FlawDefinition(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
}