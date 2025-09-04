package com.xirc.nichirin.common.data;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;

/**
 * Complete player data container including breathing styles, progression, and statistics
 */
@Getter
public class PlayerData {

    /**
     * Gets breathing style data
     */
    private final BreathingStyleData breathingStyleData = new BreathingStyleData();

    /**
     * Gets progression data
     */
    private final BreathingStyleProgression progression = new BreathingStyleProgression();

    /**
     * Gets statistics tracking data
     */
    private final BreathingStyleStatistics statistics = new BreathingStyleStatistics();

    public PlayerData() {
        // Constructor
    }

    /**
     * Copies all data from another instance
     */
    public void copyFrom(PlayerData other) {
        this.breathingStyleData.copyFrom(other.breathingStyleData);
        this.progression.copyFrom(other.progression);
        this.statistics.copyFrom(other.statistics);
    }

    /**
     * Saves all data to NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("BreathingStyle", breathingStyleData.save());
        tag.put("Progression", progression.save());
        tag.put("Statistics", statistics.save());
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
        if (tag.contains("Statistics")) {
            statistics.load(tag.getCompound("Statistics"));
        }
    }
}