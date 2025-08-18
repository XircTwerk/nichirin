package com.xirc.nichirin.common.data;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks player progression and unlocked breathing styles
 */
public class BreathingStyleProgression {

    // Set of unlocked breathing style IDs
    private final Set<String> unlockedStyles = new HashSet<>();

    // Getters
    // Player stats
    @Getter
    private int demonsSlain = 0;
    @Getter
    private int totalDamageDealt = 0;
    @Getter
    private int breathingExperience = 0;
    @Getter
    private int slayerRank = 0;

    public BreathingStyleProgression() {
        // Everyone starts with no breathing styles unlocked
        // Thunder Breathing is unlocked via lightning strike (handled elsewhere)
        // Flame Breathing is unlocked via being on fire for 15 seconds (handled elsewhere)
    }

    /**
     * Unlocks a breathing style for the player
     */
    public void unlockStyle(String styleId) {
        unlockedStyles.add(styleId);
    }

    /**
     * Checks if a breathing style is unlocked
     */
    public boolean isStyleUnlocked(String styleId) {
        return unlockedStyles.contains(styleId);
    }

    /**
     * Gets all unlocked breathing styles
     */
    public Set<String> getUnlockedStyles() {
        return new HashSet<>(unlockedStyles);
    }

    /**
     * Checks if player has unlocked any breathing style
     */
    public boolean hasAnyBreathingStyle() {
        return !unlockedStyles.isEmpty();
    }

    /**
     * Gets the unlock requirement description for a style
     */
    public String getUnlockRequirement(String styleId) {
        return switch (styleId) {
            case "thunder_breathing" -> "Get struck by lightning while wearing no armor";
            case "flame_breathing" -> "Survive being on fire for 15 seconds";
            case "insect_breathing" -> "Throw a poison potion";
            default -> "Unknown requirement";
        };
    }

    /**
     * Checks if requirements are met for a breathing style
     * Note: Thunder Breathing and Flame Breathing unlocks are handled by their respective handlers
     */
    public boolean meetsRequirements(String styleId) {
        // Handled by lightning strike event
        // Handled by burning duration event
        return false;
    }

    /**
     * Attempts to unlock a style if requirements are met
     * @return true if successfully unlocked, false if requirements not met
     */
    public boolean tryUnlockStyle(String styleId) {
        if (isStyleUnlocked(styleId)) {
            return true; // Already unlocked
        }

        if (meetsRequirements(styleId)) {
            unlockStyle(styleId);
            return true;
        }

        return false;
    }

    // Player stat methods

    public void addDemonKill() {
        demonsSlain++;
        breathingExperience += 100;
        updateSlayerRank();
    }

    public void addDamageDealt(int damage) {
        totalDamageDealt += damage;
        breathingExperience += damage / 10; // 1 exp per 10 damage
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

    public String getSlayerRankName() {
        return switch (slayerRank) {
            case 0 -> "Novice";
            case 1 -> "Apprentice";
            case 2 -> "Slayer";
            case 3 -> "Veteran";
            case 4 -> "Elite";
            case 5 -> "Master";
            default -> "Unknown";
        };
    }

    /**
     * Copies data from another progression instance
     */
    public void copyFrom(BreathingStyleProgression other) {
        this.unlockedStyles.clear();
        this.unlockedStyles.addAll(other.unlockedStyles);
        this.demonsSlain = other.demonsSlain;
        this.totalDamageDealt = other.totalDamageDealt;
        this.breathingExperience = other.breathingExperience;
        this.slayerRank = other.slayerRank;
    }

    /**
     * Saves progression data to NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        // Save unlocked styles
        ListTag stylesList = new ListTag();
        for (String style : unlockedStyles) {
            stylesList.add(StringTag.valueOf(style));
        }
        tag.put("UnlockedStyles", stylesList);

        // Save stats
        tag.putInt("DemonsSlain", demonsSlain);
        tag.putInt("TotalDamageDealt", totalDamageDealt);
        tag.putInt("BreathingExperience", breathingExperience);
        tag.putInt("SlayerRank", slayerRank);

        return tag;
    }

    /**
     * Loads progression data from NBT
     */
    public void load(CompoundTag tag) {
        // Load unlocked styles
        unlockedStyles.clear();
        if (tag.contains("UnlockedStyles")) {
            ListTag stylesList = tag.getList("UnlockedStyles", 8); // 8 = String tag type
            for (int i = 0; i < stylesList.size(); i++) {
                unlockedStyles.add(stylesList.getString(i));
            }
        }

        // Load stats
        demonsSlain = tag.getInt("DemonsSlain");
        totalDamageDealt = tag.getInt("TotalDamageDealt");
        breathingExperience = tag.getInt("BreathingExperience");
        slayerRank = tag.getInt("SlayerRank");
    }
}