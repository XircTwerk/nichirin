package com.xirc.nichirin.common.data;

import net.minecraft.nbt.CompoundTag;

/**
 * Complete player data container including breathing styles and progression
 */
public class PlayerData {

    private final BreathingStyleData breathingStyleData = new BreathingStyleData();
    private final BreathingStyleProgression progression = new BreathingStyleProgression();

    public PlayerData() {
        // Constructor
    }

    /**
     * Gets breathing style data
     */
    public BreathingStyleData getBreathingStyleData() {
        return breathingStyleData;
    }

    /**
     * Gets progression data
     */
    public BreathingStyleProgression getProgression() {
        return progression;
    }

    /**
     * Copies all data from another instance
     */
    public void copyFrom(PlayerData other) {
        this.breathingStyleData.copyFrom(other.breathingStyleData);
        this.progression.copyFrom(other.progression);
    }

    /**
     * Saves all data to NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("BreathingStyle", breathingStyleData.save());
        tag.put("Progression", progression.save());
        return tag;
    }

    /**
     * Loads all data from NBT
     */
    public void load(CompoundTag tag) {
        if (tag.contains("BreathingStyle")) {
            breathingStyleData.load(tag.getCompound("BreathingStyle"));
        }
        if (tag.contains("Progression")) {
            progression.load(tag.getCompound("Progression"));
        }
    }
}