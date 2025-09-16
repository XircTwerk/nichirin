package com.xirc.nichirin.common.data;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;

/**
 * Complete player data container including movesets, progression, and statistics
 */
@Getter
public class PlayerData {

    /**
     * Gets unified moveset data (breathing techniques and demon arts)
     */
    private final MovesetData movesetData = new MovesetData();

    /**
     * Gets progression data
     */
    private final MovesetProgression progression = new MovesetProgression();

    /**
     * Gets statistics tracking data
     */
    private final MovesetStatistics statistics = new MovesetStatistics();

    public PlayerData() {
        // Constructor
    }

    /**
     * Copies all data from another instance
     */
    public void copyFrom(PlayerData other) {
        this.movesetData.copyFrom(other.movesetData);
        this.progression.copyFrom(other.progression);
        this.statistics.copyFrom(other.statistics);
    }

    /**
     * Saves all data to NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("MovesetData", movesetData.save());
        tag.put("Progression", progression.save());
        tag.put("Statistics", statistics.save());
        return tag;
    }

    /**
     * Loads all data from NBT with backwards compatibility
     */
    public void load(CompoundTag tag) {
        // Try new format first
        if (tag.contains("MovesetData")) {
            movesetData.load(tag.getCompound("MovesetData"));
        }
        // Handle backwards compatibility with old BreathingStyleData
        else if (tag.contains("BreathingStyle")) {
            movesetData.load(tag.getCompound("BreathingStyle"));
        }

        if (tag.contains("Progression")) {
            progression.load(tag.getCompound("Progression"));
        }
        if (tag.contains("Statistics")) {
            statistics.load(tag.getCompound("Statistics"));
        }
    }

    /**
     * Legacy getter for backwards compatibility
     * @deprecated Use getMovesetData() instead
     */
    @Deprecated
    public MovesetData getBreathingStyleData() {
        return movesetData;
    }
}