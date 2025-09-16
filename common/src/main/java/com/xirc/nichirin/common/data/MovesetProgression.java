package com.xirc.nichirin.common.data;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks player progression and unlocked movesets (breathing techniques and demon arts)
 */
public class MovesetProgression {

    // Set of unlocked moveset IDs
    private final Set<String> unlockedMovesets = new HashSet<>();

    // Player stats
    @Getter
    private int demonsSlain = 0;
    @Getter
    private int totalDamageDealt = 0;
    @Getter
    private int techniqueExperience = 0;
    @Getter
    private int slayerRank = 0;

    public MovesetProgression() {
        // Everyone starts with no movesets unlocked
        // Movesets are unlocked via specific events/commands
    }

    /**
     * Unlocks a moveset for the player
     */
    public void unlockMoveset(String movesetId) {
        unlockedMovesets.add(movesetId);
    }

    /**
     * Checks if a moveset is unlocked
     */
    public boolean isMovesetUnlocked(String movesetId) {
        return unlockedMovesets.contains(movesetId);
    }

    /**
     * Checks if player has unlocked any moveset
     */
    public boolean hasAnyMoveset() {
        return !unlockedMovesets.isEmpty();
    }

    /**
     * Gets the unlock requirement description for a moveset
     */
    public String getUnlockRequirement(String movesetId) {
        return switch (movesetId) {
            case "thunder_breathing" -> "Get struck by lightning while wearing no armor";
            case "flame_breathing" -> "Survive being on fire for 15 seconds";
            case "insect_breathing" -> "Throw a poison potion";
            case "sound_breathing" -> "Play a music disc in a jukebox";
            default -> "Unknown requirement";
        };
    }

    // Player stat methods

    public void addDemonKill() {
        demonsSlain++;
        techniqueExperience += 100;
        updateSlayerRank();
    }

    public void addDamageDealt(int damage) {
        totalDamageDealt += damage;
        techniqueExperience += damage / 10; // 1 exp per 10 damage
    }

    private void updateSlayerRank() {
        // Simple rank progression based on demons slain
        if (demonsSlain >= 50) {
            slayerRank = Math.max(slayerRank, 5);
        } else if (demonsSlain >= 25) {
            slayerRank = Math.max(slayerRank, 4);
        } else if (demonsSlain >= 15) {
            slayerRank = Math.max(slayerRank, 3);
        } else if (demonsSlain >= 8) {
            slayerRank = Math.max(slayerRank, 2);
        } else if (demonsSlain >= 3) {
            slayerRank = Math.max(slayerRank, 1);
        }
    }

    /**
     * Copies data from another progression instance
     */
    public void copyFrom(MovesetProgression other) {
        this.unlockedMovesets.clear();
        this.unlockedMovesets.addAll(other.unlockedMovesets);
        this.demonsSlain = other.demonsSlain;
        this.totalDamageDealt = other.totalDamageDealt;
        this.techniqueExperience = other.techniqueExperience;
        this.slayerRank = other.slayerRank;
    }

    /**
     * Saves progression data to NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        // Save unlocked movesets
        ListTag movesetsList = new ListTag();
        for (String moveset : unlockedMovesets) {
            movesetsList.add(StringTag.valueOf(moveset));
        }
        tag.put("UnlockedMovesets", movesetsList);

        // Save stats
        tag.putInt("DemonsSlain", demonsSlain);
        tag.putInt("TotalDamageDealt", totalDamageDealt);
        tag.putInt("TechniqueExperience", techniqueExperience);
        tag.putInt("SlayerRank", slayerRank);

        return tag;
    }

    /**
     * Loads progression data from NBT with backwards compatibility
     */
    public void load(CompoundTag tag) {
        // Load unlocked movesets
        unlockedMovesets.clear();

        // Try new format first
        if (tag.contains("UnlockedMovesets")) {
            ListTag movesetsList = tag.getList("UnlockedMovesets", 8); // 8 = String tag type
            for (int i = 0; i < movesetsList.size(); i++) {
                unlockedMovesets.add(movesetsList.getString(i));
            }
        }
        // Backwards compatibility with old "UnlockedStyles"
        else if (tag.contains("UnlockedStyles")) {
            ListTag stylesList = tag.getList("UnlockedStyles", 8);
            for (int i = 0; i < stylesList.size(); i++) {
                unlockedMovesets.add(stylesList.getString(i));
            }
        }

        // Load stats with backwards compatibility
        demonsSlain = tag.getInt("DemonsSlain");
        totalDamageDealt = tag.getInt("TotalDamageDealt");

        // Try new name first, fallback to old name
        if (tag.contains("TechniqueExperience")) {
            techniqueExperience = tag.getInt("TechniqueExperience");
        } else {
            techniqueExperience = tag.getInt("BreathingExperience");
        }

        slayerRank = tag.getInt("SlayerRank");
    }
}