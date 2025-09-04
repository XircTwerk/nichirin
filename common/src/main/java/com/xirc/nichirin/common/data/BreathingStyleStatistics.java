package com.xirc.nichirin.common.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks detailed statistics for breathing style usage
 */
public class BreathingStyleStatistics {

    // Per-style statistics
    private final Map<String, StyleStats> styleStats = new HashMap<>();

    // Global statistics
    private int totalDamageDealt = 0;
    private int longestComboChain = 0;
    private int totalSuccessfulDodges = 0;
    private int totalSuccessfulBlocks = 0;

    // Current session tracking (not saved)
    private transient long sessionStartTime = System.currentTimeMillis();
    private transient int currentComboChain = 0;

    /**
     * Individual style statistics container
     */
    public static class StyleStats {
        private int usageCount = 0;
        private int damageDealt = 0;
        private int longestCombo = 0;
        private long totalTimeEquipped = 0; // In milliseconds
        private long lastEquippedTime = 0; // When it was last equipped

        // Getters
        public int getUsageCount() { return usageCount; }
        public int getDamageDealt() { return damageDealt; }
        public int getLongestCombo() { return longestCombo; }
        public long getTotalTimeEquipped() { return totalTimeEquipped; }
        public long getTotalTimeEquippedHours() { return totalTimeEquipped / (1000 * 60 * 60); }
        public long getTotalTimeEquippedMinutes() { return (totalTimeEquipped / (1000 * 60)) % 60; }

        // Internal methods
        void addUsage() { usageCount++; }
        void addDamage(int damage) { damageDealt += damage; }
        void updateCombo(int combo) { longestCombo = Math.max(longestCombo, combo); }
        void startEquipTime() { lastEquippedTime = System.currentTimeMillis(); }
        void endEquipTime() {
            if (lastEquippedTime > 0) {
                totalTimeEquipped += System.currentTimeMillis() - lastEquippedTime;
                lastEquippedTime = 0;
            }
        }
        boolean isCurrentlyEquipped() { return lastEquippedTime > 0; }

        // NBT serialization
        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("UsageCount", usageCount);
            tag.putInt("DamageDealt", damageDealt);
            tag.putInt("LongestCombo", longestCombo);
            tag.putLong("TotalTimeEquipped", totalTimeEquipped);
            tag.putLong("LastEquippedTime", lastEquippedTime);
            return tag;
        }

        void load(CompoundTag tag) {
            usageCount = tag.getInt("UsageCount");
            damageDealt = tag.getInt("DamageDealt");
            longestCombo = tag.getInt("LongestCombo");
            totalTimeEquipped = tag.getLong("TotalTimeEquipped");
            lastEquippedTime = tag.getLong("LastEquippedTime");
        }
    }

    /**
     * Records usage of a breathing technique
     */
    public void recordTechniqueUsage(String styleId) {
        getOrCreateStyleStats(styleId).addUsage();
    }

    /**
     * Records damage dealt with a breathing style
     */
    public void recordDamageDealt(String styleId, int damage) {
        getOrCreateStyleStats(styleId).addDamage(damage);
        totalDamageDealt += damage;
    }

    /**
     * Updates combo chain tracking
     */
    public void updateComboChain(String styleId, int comboLength) {
        currentComboChain = comboLength;
        longestComboChain = Math.max(longestComboChain, comboLength);
        getOrCreateStyleStats(styleId).updateCombo(comboLength);
    }

    /**
     * Resets current combo chain (when combo is broken)
     */
    public void resetComboChain() {
        currentComboChain = 0;
    }

    /**
     * Records a successful dodge
     */
    public void recordSuccessfulDodge() {
        totalSuccessfulDodges++;
    }

    /**
     * Records a successful block
     */
    public void recordSuccessfulBlock() {
        totalSuccessfulBlocks++;
    }

    /**
     * Called when a breathing style is equipped
     */
    public void onStyleEquipped(String styleId) {
        // End timing for any currently equipped style
        for (StyleStats stats : styleStats.values()) {
            if (stats.isCurrentlyEquipped()) {
                stats.endEquipTime();
            }
        }

        // Start timing for new style
        getOrCreateStyleStats(styleId).startEquipTime();
    }

    /**
     * Called when a breathing style is unequipped
     */
    public void onStyleUnequipped(String styleId) {
        StyleStats stats = styleStats.get(styleId);
        if (stats != null) {
            stats.endEquipTime();
        }
    }

    /**
     * Gets statistics for a specific breathing style
     */
    public StyleStats getStyleStats(String styleId) {
        return styleStats.get(styleId);
    }

    /**
     * Gets or creates statistics for a breathing style
     */
    private StyleStats getOrCreateStyleStats(String styleId) {
        return styleStats.computeIfAbsent(styleId, k -> new StyleStats());
    }

    // Global getters
    public int getTotalDamageDealt() { return totalDamageDealt; }
    public int getLongestComboChain() { return longestComboChain; }
    public int getTotalSuccessfulDodges() { return totalSuccessfulDodges; }
    public int getTotalSuccessfulBlocks() { return totalSuccessfulBlocks; }
    public int getCurrentComboChain() { return currentComboChain; }

    /**
     * Gets all style IDs that have statistics
     */
    public java.util.Set<String> getTrackedStyles() {
        return styleStats.keySet();
    }

    /**
     * Copies statistics from another instance
     */
    public void copyFrom(BreathingStyleStatistics other) {
        // Copy global stats
        this.totalDamageDealt = other.totalDamageDealt;
        this.longestComboChain = other.longestComboChain;
        this.totalSuccessfulDodges = other.totalSuccessfulDodges;
        this.totalSuccessfulBlocks = other.totalSuccessfulBlocks;

        // Copy style stats
        this.styleStats.clear();
        for (Map.Entry<String, StyleStats> entry : other.styleStats.entrySet()) {
            StyleStats newStats = new StyleStats();
            newStats.load(entry.getValue().save()); // Copy via NBT
            this.styleStats.put(entry.getKey(), newStats);
        }
    }

    /**
     * Saves statistics to NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        // Save global stats
        tag.putInt("TotalDamageDealt", totalDamageDealt);
        tag.putInt("LongestComboChain", longestComboChain);
        tag.putInt("TotalSuccessfulDodges", totalSuccessfulDodges);
        tag.putInt("TotalSuccessfulBlocks", totalSuccessfulBlocks);

        // Save per-style stats
        CompoundTag styleStatsTag = new CompoundTag();
        for (Map.Entry<String, StyleStats> entry : styleStats.entrySet()) {
            styleStatsTag.put(entry.getKey(), entry.getValue().save());
        }
        tag.put("StyleStats", styleStatsTag);

        return tag;
    }

    /**
     * Loads statistics from NBT
     */
    public void load(CompoundTag tag) {
        // Load global stats
        totalDamageDealt = tag.getInt("TotalDamageDealt");
        longestComboChain = tag.getInt("LongestComboChain");
        totalSuccessfulDodges = tag.getInt("TotalSuccessfulDodges");
        totalSuccessfulBlocks = tag.getInt("TotalSuccessfulBlocks");

        // Load per-style stats
        styleStats.clear();
        if (tag.contains("StyleStats")) {
            CompoundTag styleStatsTag = tag.getCompound("StyleStats");
            for (String styleId : styleStatsTag.getAllKeys()) {
                StyleStats stats = new StyleStats();
                stats.load(styleStatsTag.getCompound(styleId));
                styleStats.put(styleId, stats);
            }
        }
    }

    /**
     * Updates all currently equipped styles' time tracking
     * Call this periodically (like every minute) to keep time tracking accurate
     */
    public void updateTimeTracking() {
        // This ensures that if the server crashes, we don't lose too much time tracking data
        for (StyleStats stats : styleStats.values()) {
            if (stats.isCurrentlyEquipped()) {
                long currentTime = System.currentTimeMillis();
                stats.totalTimeEquipped += currentTime - stats.lastEquippedTime;
                stats.lastEquippedTime = currentTime;
            }
        }
    }
}