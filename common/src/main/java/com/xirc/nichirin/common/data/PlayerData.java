package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.system.perks.PerkData;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

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

    /**
     * Player-owned CQC preset/loadout.
     */
    private final CqcPresetData cqcPresetData = new CqcPresetData();

    /**
     * Gets perk system data (discovered, equipped, flaws, presets)
     */
    private final PerkData perkData = new PerkData();

    /**
     * Demon types (e.g. "akaza") that have spared this player in a mercy/recruitment encounter.
     * Each such Upper Moon demon stays neutral until the player attacks it again. Per-demon, not
     * global — being spared by one Upper Moon doesn't pacify the others.
     */
    private final Set<String> sparedByDemons = new HashSet<>();

    public PlayerData() {
        // Constructor
    }

    public boolean isSparedBy(String demonType) {
        return demonType != null && sparedByDemons.contains(demonType);
    }

    public void setSparedBy(String demonType) {
        if (demonType != null) sparedByDemons.add(demonType);
    }

    public boolean removeSparedBy(String demonType) {
        return sparedByDemons.remove(demonType);
    }

    /**
     * Copies all data from another instance
     */
    public void copyFrom(PlayerData other) {
        this.movesetData.copyFrom(other.movesetData);
        this.progression.copyFrom(other.progression);
        this.statistics.copyFrom(other.statistics);
        this.cqcPresetData.copyFrom(other.cqcPresetData);
        this.perkData.copyFrom(other.perkData);
        this.sparedByDemons.clear();
        this.sparedByDemons.addAll(other.sparedByDemons);
    }

    /**
     * Saves all data to NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("MovesetData", movesetData.save());
        tag.put("Progression", progression.save());
        tag.put("Statistics", statistics.save());
        tag.put("CqcPresetData", cqcPresetData.save());
        tag.put("PerkData", perkData.save());

        ListTag spared = new ListTag();
        for (String demonType : sparedByDemons) {
            spared.add(StringTag.valueOf(demonType));
        }
        tag.put("SparedByDemons", spared);
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
        if (tag.contains("CqcPresetData")) {
            cqcPresetData.load(tag.getCompound("CqcPresetData"));
        }
        if (tag.contains("PerkData")) {
            perkData.load(tag.getCompound("PerkData"));
        }

        sparedByDemons.clear();
        if (tag.contains("SparedByDemons")) {
            ListTag spared = tag.getList("SparedByDemons", Tag.TAG_STRING);
            for (int i = 0; i < spared.size(); i++) {
                sparedByDemons.add(spared.getString(i));
            }
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

    public MovesetData getMovesetData() {
        return movesetData;
    }

    public CqcPresetData getCqcPresetData() {
        return cqcPresetData;
    }
}
