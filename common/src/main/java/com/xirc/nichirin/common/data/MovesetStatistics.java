package com.xirc.nichirin.common.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Tracks detailed statistics for moveset usage (breathing techniques and demon arts)
 */
public class MovesetStatistics {

    // Per-moveset statistics
    private final Map<String, MovesetStats> movesetStats = new HashMap<>();

    // Global statistics
    private int totalDamageDealt = 0;
    private int longestComboChain = 0;
    private int totalSuccessfulDodges = 0;
    private int totalSuccessfulBlocks = 0;

    // Current session tracking (not saved)
    private transient long sessionStartTime = System.currentTimeMillis();
    private transient int currentComboChain = 0;

    /**
     * Individual moveset statistics container
     */
    public static class MovesetStats {
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
     * Records usage of a technique (breathing or demon)
     */
    public void recordTechniqueUsage(String movesetId) {
        getOrCreateMovesetStats(movesetId).addUsage();
    }

    /**
     * Records damage dealt with a moveset
     */
    public void recordDamageDealt(String movesetId, int damage) {
        getOrCreateMovesetStats(movesetId).addDamage(damage);
        totalDamageDealt += damage;
    }

    /**
     * Updates combo chain tracking
     */
    public void updateComboChain(String movesetId, int comboLength) {
        currentComboChain = comboLength;
        longestComboChain = Math.max(longestComboChain, comboLength);
        getOrCreateMovesetStats(movesetId).updateCombo(comboLength);
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
     * Called when a moveset is equipped
     */
    public void onMovesetEquipped(String movesetId) {
        // End timing for any currently equipped moveset
        for (MovesetStats stats : movesetStats.values()) {
            if (stats.isCurrentlyEquipped()) {
                stats.endEquipTime();
            }
        }

        // Start timing for new moveset
        getOrCreateMovesetStats(movesetId).startEquipTime();
    }

    /**
     * Called when a moveset is unequipped
     */
    public void onMovesetUnequipped(String movesetId) {
        MovesetStats stats = movesetStats.get(movesetId);
        if (stats != null) {
            stats.endEquipTime();
        }
    }

    /**
     * Gets statistics for a specific moveset
     */
    public MovesetStats getMovesetStats(String movesetId) {
        return movesetStats.get(movesetId);
    }

    /**
     * Gets or creates statistics for a moveset
     */
    private MovesetStats getOrCreateMovesetStats(String movesetId) {
        return movesetStats.computeIfAbsent(movesetId, k -> new MovesetStats());
    }

    // Global getters
    public int getTotalDamageDealt() { return totalDamageDealt; }
    public int getLongestComboChain() { return longestComboChain; }
    public int getTotalSuccessfulDodges() { return totalSuccessfulDodges; }
    public int getTotalSuccessfulBlocks() { return totalSuccessfulBlocks; }
    public int getCurrentComboChain() { return currentComboChain; }

    /**
     * Gets all moveset IDs that have statistics
     */
    public Set<String> getTrackedMovesets() {
        return movesetStats.keySet();
    }

    /**
     * Copies statistics from another instance
     */
    public void copyFrom(MovesetStatistics other) {
        // Copy global stats
        this.totalDamageDealt = other.totalDamageDealt;
        this.longestComboChain = other.longestComboChain;
        this.totalSuccessfulDodges = other.totalSuccessfulDodges;
        this.totalSuccessfulBlocks = other.totalSuccessfulBlocks;

        // Copy moveset stats
        this.movesetStats.clear();
        for (Map.Entry<String, MovesetStats> entry : other.movesetStats.entrySet()) {
            MovesetStats newStats = new MovesetStats();
            newStats.load(entry.getValue().save()); // Copy via NBT
            this.movesetStats.put(entry.getKey(), newStats);
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

        // Save per-moveset stats
        CompoundTag movesetStatsTag = new CompoundTag();
        for (Map.Entry<String, MovesetStats> entry : movesetStats.entrySet()) {
            movesetStatsTag.put(entry.getKey(), entry.getValue().save());
        }
        tag.put("MovesetStats", movesetStatsTag);

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

        // Load per-moveset stats
        movesetStats.clear();
        if (tag.contains("MovesetStats")) {
            CompoundTag movesetStatsTag = tag.getCompound("MovesetStats");
            for (String movesetId : movesetStatsTag.getAllKeys()) {
                MovesetStats stats = new MovesetStats();
                stats.load(movesetStatsTag.getCompound(movesetId));
                movesetStats.put(movesetId, stats);
            }
        }
    }

    /**
     * Updates all currently equipped movesets' time tracking
     * Call this periodically (like every minute) to keep time tracking accurate
     */
    public void updateTimeTracking() {
        // This ensures that if the server crashes, we don't lose too much time tracking data
        for (MovesetStats stats : movesetStats.values()) {
            if (stats.isCurrentlyEquipped()) {
                long currentTime = System.currentTimeMillis();
                stats.totalTimeEquipped += currentTime - stats.lastEquippedTime;
                stats.lastEquippedTime = currentTime;
            }
        }
    }
}
