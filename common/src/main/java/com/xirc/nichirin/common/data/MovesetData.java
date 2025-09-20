package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.registry.MovesetRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Unified data class for storing player's selected movesets (breathing techniques AND demon arts)
 * Supports dual moveset system - player can have both breathing and demon movesets simultaneously
 * Handles modifiers and statistics tracking automatically
 */
public class MovesetData {

    @Nullable
    private AbstractMoveset currentBreathingMoveset;

    @Nullable
    private AbstractMoveset currentDemonMoveset;

    @Nullable
    private String breathingMovesetId;

    @Nullable
    private String demonMovesetId;

    // Keep track of the player for modifiers and statistics
    @Nullable
    private Player player;

    // Flag to prevent recursive operations during modifier application
    private boolean isApplyingModifiers = false;

    public MovesetData() {
        this.currentBreathingMoveset = null;
        this.currentDemonMoveset = null;
        this.breathingMovesetId = null;
        this.demonMovesetId = null;
        this.player = null;
    }

    /**
     * Sets the player reference (called by PlayerDataProvider)
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Sets the current breathing moveset
     */
    public void setBreathingMoveset(@Nullable AbstractMoveset moveset) {
        if (isApplyingModifiers) {
            return;
        }

        try {
            isApplyingModifiers = true;

            // Handle old breathing moveset cleanup
            if (this.currentBreathingMoveset != null && this.player != null) {
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onMovesetUnequipped(this.currentBreathingMoveset.getMovesetId());
                }

                try {
                    this.currentBreathingMoveset.removeAllModifiers(this.player);
                } catch (Exception e) {
                    System.err.println("Error removing breathing modifiers: " + e.getMessage());
                }
            }

            this.currentBreathingMoveset = moveset;
            this.breathingMovesetId = moveset != null ? moveset.getMovesetId() : null;

            // Handle new breathing moveset
            if (this.currentBreathingMoveset != null && this.player != null) {
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onMovesetEquipped(this.currentBreathingMoveset.getMovesetId());
                }

                this.currentBreathingMoveset.applyAllModifiers(this.player);
            }
        } finally {
            isApplyingModifiers = false;
        }
    }

    /**
     * Sets the current demon moveset
     */
    public void setDemonMoveset(@Nullable AbstractMoveset moveset) {
        if (isApplyingModifiers) {
            return;
        }

        try {
            isApplyingModifiers = true;

            // Handle old demon moveset cleanup
            if (this.currentDemonMoveset != null && this.player != null) {
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onMovesetUnequipped(this.currentDemonMoveset.getMovesetId());
                }
                // Demon movesets don't have modifiers, so no removal needed
            }

            this.currentDemonMoveset = moveset;
            this.demonMovesetId = moveset != null ? moveset.getMovesetId() : null;

            // Handle new demon moveset
            if (this.currentDemonMoveset != null && this.player != null) {
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onMovesetEquipped(this.currentDemonMoveset.getMovesetId());
                }
                // Demon movesets don't have modifiers to apply
            }
        } finally {
            isApplyingModifiers = false;
        }
    }

    /**
     * Gets the current breathing moveset
     */
    @Nullable
    public AbstractMoveset getBreathingMoveset() {
        if (currentBreathingMoveset == null && breathingMovesetId != null) {
            try {
                currentBreathingMoveset = MovesetRegistry.getMoveset(breathingMovesetId);
                if (currentBreathingMoveset != null && currentBreathingMoveset.isBreathingMoveset() && player != null && !isApplyingModifiers) {
                    isApplyingModifiers = true;
                    try {
                        currentBreathingMoveset.applyAllModifiers(player);
                        var statistics = getStatistics();
                        if (statistics != null) {
                            statistics.onMovesetEquipped(breathingMovesetId);
                        }
                    } finally {
                        isApplyingModifiers = false;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading breathing moveset " + breathingMovesetId + ": " + e.getMessage());
                this.breathingMovesetId = null;
            }
        }
        return currentBreathingMoveset;
    }

    /**
     * Gets the current demon moveset
     */
    @Nullable
    public AbstractMoveset getDemonMoveset() {
        if (currentDemonMoveset == null && demonMovesetId != null) {
            try {
                currentDemonMoveset = MovesetRegistry.getMoveset(demonMovesetId);
                if (currentDemonMoveset != null && currentDemonMoveset.isDemonMoveset() && player != null && !isApplyingModifiers) {
                    var statistics = getStatistics();
                    if (statistics != null) {
                        statistics.onMovesetEquipped(demonMovesetId);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading demon moveset " + demonMovesetId + ": " + e.getMessage());
                this.demonMovesetId = null;
            }
        }
        return currentDemonMoveset;
    }

    /**
     * Gets the primary moveset (breathing takes precedence, then demon)
     * This maintains backwards compatibility with old code
     */
    @Nullable
    public AbstractMoveset getMoveset() {
        AbstractMoveset breathing = getBreathingMoveset();
        if (breathing != null) {
            return breathing;
        }
        return getDemonMoveset();
    }

    /**
     * Sets breathing moveset by ID
     */
    public void setBreathingMovesetId(@Nullable String movesetId) {
        if (isApplyingModifiers) {
            return;
        }

        try {
            isApplyingModifiers = true;

            // Handle old breathing moveset cleanup
            if (this.currentBreathingMoveset != null && this.player != null) {
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onMovesetUnequipped(this.currentBreathingMoveset.getMovesetId());
                }
                try {
                    this.currentBreathingMoveset.removeAllModifiers(this.player);
                } catch (Exception e) {
                    System.err.println("Error removing breathing modifiers: " + e.getMessage());
                }
            }

            this.breathingMovesetId = movesetId;
            this.currentBreathingMoveset = null; // Force reload
        } finally {
            isApplyingModifiers = false;
        }

        // Load and apply new moveset
        getBreathingMoveset();
    }

    /**
     * Sets demon moveset by ID
     */
    public void setDemonMovesetId(@Nullable String movesetId) {
        if (isApplyingModifiers) {
            return;
        }

        try {
            isApplyingModifiers = true;

            // Handle old demon moveset cleanup
            if (this.currentDemonMoveset != null && this.player != null) {
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onMovesetUnequipped(this.currentDemonMoveset.getMovesetId());
                }
            }

            this.demonMovesetId = movesetId;
            this.currentDemonMoveset = null; // Force reload
        } finally {
            isApplyingModifiers = false;
        }

        // Load new moveset
        getDemonMoveset();
    }

    /**
     * Sets moveset by ID (backwards compatibility - determines type automatically)
     */
    public void setMovesetId(@Nullable String movesetId) {
        if (movesetId == null) {
            clearMovesets();
            return;
        }

        // Determine if it's breathing or demon based on registered moveset
        AbstractMoveset moveset = MovesetRegistry.getMoveset(movesetId);
        if (moveset != null) {
            if (moveset.isBreathingMoveset()) {
                setBreathingMovesetId(movesetId);
            } else if (moveset.isDemonMoveset()) {
                setDemonMovesetId(movesetId);
            }
        }
    }

    /**
     * Gets breathing moveset ID
     */
    @Nullable
    public String getBreathingMovesetId() {
        return breathingMovesetId;
    }

    /**
     * Gets demon moveset ID
     */
    @Nullable
    public String getDemonMovesetId() {
        return demonMovesetId;
    }

    /**
     * Gets primary moveset ID (backwards compatibility)
     */
    @Nullable
    public String getMovesetId() {
        if (breathingMovesetId != null) {
            return breathingMovesetId;
        }
        return demonMovesetId;
    }

    /**
     * Checks if player has any moveset
     */
    public boolean hasMoveset() {
        return breathingMovesetId != null || demonMovesetId != null;
    }

    /**
     * Checks if player has a breathing moveset
     */
    public boolean hasBreathingMoveset() {
        return breathingMovesetId != null;
    }

    /**
     * Checks if player has a demon moveset
     */
    public boolean hasDemonMoveset() {
        return demonMovesetId != null;
    }

    /**
     * Clears all movesets
     */
    public void clearMovesets() {
        setBreathingMovesetId(null);
        setDemonMovesetId(null);
    }

    /**
     * Clears only breathing moveset
     */
    public void clearBreathingMoveset() {
        setBreathingMovesetId(null);
    }

    /**
     * Clears only demon moveset
     */
    public void clearDemonMoveset() {
        setDemonMovesetId(null);
    }

    /**
     * Legacy method - clears all movesets
     */
    public void clearMoveset() {
        clearMovesets();
    }

    /**
     * Copies data from another instance
     */
    public void copyFrom(MovesetData other) {
        if (isApplyingModifiers) {
            return;
        }

        try {
            isApplyingModifiers = true;

            // Clean up current movesets
            if (this.currentBreathingMoveset != null && this.player != null) {
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onMovesetUnequipped(this.currentBreathingMoveset.getMovesetId());
                }
                try {
                    this.currentBreathingMoveset.removeAllModifiers(this.player);
                } catch (Exception e) {
                    System.err.println("Error during copyFrom breathing cleanup: " + e.getMessage());
                }
            }

            if (this.currentDemonMoveset != null && this.player != null) {
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onMovesetUnequipped(this.currentDemonMoveset.getMovesetId());
                }
            }

            // Copy IDs
            this.breathingMovesetId = other.getBreathingMovesetId();
            this.demonMovesetId = other.getDemonMovesetId();
            this.currentBreathingMoveset = null; // Force reload
            this.currentDemonMoveset = null; // Force reload

        } finally {
            isApplyingModifiers = false;
        }

        // Load and apply new movesets
        getBreathingMoveset();
        getDemonMoveset();
    }

    /**
     * Saves data to NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (breathingMovesetId != null) {
            tag.putString("BreathingMovesetId", breathingMovesetId);
        }
        if (demonMovesetId != null) {
            tag.putString("DemonMovesetId", demonMovesetId);
        }

        // Legacy compatibility - save primary moveset as "MovesetId"
        String primaryId = getMovesetId();
        if (primaryId != null) {
            tag.putString("MovesetId", primaryId);
        }

        return tag;
    }

    /**
     * Loads data from NBT with backwards compatibility
     */
    public void load(CompoundTag tag) {
        if (isApplyingModifiers) {
            return;
        }

        // Load dual moveset format (new)
        if (tag.contains("BreathingMovesetId")) {
            this.breathingMovesetId = tag.getString("BreathingMovesetId");
        }
        if (tag.contains("DemonMovesetId")) {
            this.demonMovesetId = tag.getString("DemonMovesetId");
        }

        // Backwards compatibility - convert old single moveset to appropriate type
        if (tag.contains("MovesetId") && !tag.contains("BreathingMovesetId") && !tag.contains("DemonMovesetId")) {
            String oldMovesetId = tag.getString("MovesetId");
            AbstractMoveset moveset = MovesetRegistry.getMoveset(oldMovesetId);
            if (moveset != null) {
                if (moveset.isBreathingMoveset()) {
                    this.breathingMovesetId = oldMovesetId;
                } else if (moveset.isDemonMoveset()) {
                    this.demonMovesetId = oldMovesetId;
                }
            }
        }

        // Clear instances to force reload
        this.currentBreathingMoveset = null;
        this.currentDemonMoveset = null;
    }

    /**
     * Gets the statistics tracking system
     */
    private MovesetStatistics getStatistics() {
        if (player != null) {
            try {
                return com.xirc.nichirin.common.data.PlayerDataProvider.getData(player).getStatistics();
            } catch (Exception e) {
                System.err.println("Error accessing statistics: " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Called when player disconnects/dies to clean up modifiers
     */
    public void cleanup() {
        if (isApplyingModifiers) {
            return;
        }

        if (this.currentBreathingMoveset != null && this.player != null) {
            var statistics = getStatistics();
            if (statistics != null) {
                statistics.onMovesetUnequipped(this.currentBreathingMoveset.getMovesetId());
            }
            try {
                this.currentBreathingMoveset.removeAllModifiers(this.player);
            } catch (Exception e) {
                System.err.println("Error during cleanup: " + e.getMessage());
            }
        }

        if (this.currentDemonMoveset != null && this.player != null) {
            var statistics = getStatistics();
            if (statistics != null) {
                statistics.onMovesetUnequipped(this.currentDemonMoveset.getMovesetId());
            }
        }
    }

    /**
     * Force reapply modifiers (useful for troubleshooting breathing techniques)
     */
    public void reapplyModifiers() {
        if (this.currentBreathingMoveset != null && this.player != null && !isApplyingModifiers) {
            try {
                isApplyingModifiers = true;
                this.currentBreathingMoveset.applyAllModifiers(this.player);
            } catch (Exception e) {
                System.err.println("Error force reapplying modifiers: " + e.getMessage());
            } finally {
                isApplyingModifiers = false;
            }
        }
    }

    /**
     * Helper method to get player name safely
     */
    private String getPlayerName() {
        return player != null ? player.getName().getString() : "unknown";
    }
}