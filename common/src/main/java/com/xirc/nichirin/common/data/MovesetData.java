package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.registry.MovesetRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Unified data class for storing player's selected moveset (breathing techniques or demon arts)
 * Handles modifiers and statistics tracking automatically
 */
public class MovesetData {

    @Nullable
    private AbstractMoveset currentMoveset;

    @Nullable
    private String movesetId;

    // Keep track of the player for modifiers and statistics
    @Nullable
    private Player player;

    // Flag to prevent recursive operations during modifier application
    private boolean isApplyingModifiers = false;

    public MovesetData() {
        this.currentMoveset = null;
        this.movesetId = null;
        this.player = null;
    }

    /**
     * Sets the player reference (called by PlayerDataProvider)
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Sets the current moveset (breathing or demon)
     */
    public void setMoveset(@Nullable AbstractMoveset moveset) {
        // Prevent recursive calls during modifier application
        if (isApplyingModifiers) {
            return;
        }

        try {
            isApplyingModifiers = true;

            // Handle statistics tracking and modifiers for old moveset
            if (this.currentMoveset != null && this.player != null) {
                // Record unequip time for old moveset
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onMovesetUnequipped(this.currentMoveset.getMovesetId());
                }

                // Remove old modifiers (only breathing techniques have modifiers)
                if (this.currentMoveset.isBreathingMoveset()) {
                    try {
                        this.currentMoveset.removeAllModifiers(this.player);
                    } catch (Exception e) {
                        System.err.println("Error removing modifiers for " + this.currentMoveset.getMovesetId() + ": " + e.getMessage());
                    }
                }
            }

            this.currentMoveset = moveset;
            this.movesetId = moveset != null ? moveset.getMovesetId() : null;

            // Handle statistics tracking and modifiers for new moveset
            if (this.currentMoveset != null && this.player != null) {
                // Record equip time for new moveset
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onMovesetEquipped(this.currentMoveset.getMovesetId());
                }

                // Apply new modifiers (only for breathing techniques)
                if (this.currentMoveset.isBreathingMoveset()) {
                    try {
                        this.currentMoveset.applyAllModifiers(this.player);
                        System.out.println("DEBUG: Applied breathing modifiers for moveset: " + movesetId + " to player " + getPlayerName());
                    } catch (Exception e) {
                        System.err.println("Error applying modifiers for " + movesetId + ": " + e.getMessage());
                    }
                } else {
                    System.out.println("DEBUG: Equipped demon moveset (no modifiers): " + movesetId + " for player " + getPlayerName());
                }
            }
        } finally {
            isApplyingModifiers = false;
        }
    }

    /**
     * Gets the current moveset
     */
    @Nullable
    public AbstractMoveset getMoveset() {
        // If we only have an ID but no moveset instance, try to load it
        if (currentMoveset == null && movesetId != null) {
            try {
                currentMoveset = MovesetRegistry.getMoveset(movesetId);

                // Apply modifiers when loading (but only if not already applying)
                if (currentMoveset != null && player != null && !isApplyingModifiers) {
                    isApplyingModifiers = true;
                    try {
                        // Only apply modifiers for breathing techniques
                        if (currentMoveset.isBreathingMoveset()) {
                            currentMoveset.applyAllModifiers(player);
                            System.out.println("DEBUG: Applied breathing modifiers for loaded moveset: " + movesetId + " to player " + getPlayerName());
                        } else {
                            System.out.println("DEBUG: Loaded demon moveset (no modifiers): " + movesetId + " for player " + getPlayerName());
                        }

                        var statistics = getStatistics();
                        if (statistics != null) {
                            statistics.onMovesetEquipped(movesetId);
                        }
                    } finally {
                        isApplyingModifiers = false;
                    }
                }
            } catch (Exception e) {
                System.err.println("Error loading moveset " + movesetId + ": " + e.getMessage());
                // Clear invalid moveset ID
                this.movesetId = null;
            }
        }
        return currentMoveset;
    }

    /**
     * Sets the moveset by its ID
     */
    public void setMovesetId(@Nullable String movesetId) {
        // Prevent recursive calls
        if (isApplyingModifiers) {
            return;
        }

        try {
            isApplyingModifiers = true;

            // Handle old moveset cleanup
            if (this.currentMoveset != null && this.player != null) {
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onMovesetUnequipped(this.currentMoveset.getMovesetId());
                }

                // Only remove modifiers for breathing techniques
                if (this.currentMoveset.isBreathingMoveset()) {
                    try {
                        this.currentMoveset.removeAllModifiers(this.player);
                    } catch (Exception e) {
                        System.err.println("Error removing modifiers during setMovesetId: " + e.getMessage());
                    }
                }
            }

            this.movesetId = movesetId;
            // Clear the moveset instance to force reload
            this.currentMoveset = null;

        } finally {
            isApplyingModifiers = false;
        }

        // Load and apply new moveset (this will handle the flag correctly)
        getMoveset();
    }

    /**
     * Gets the current moveset ID
     */
    @Nullable
    public String getMovesetId() {
        return movesetId;
    }

    /**
     * Checks if the player has a moveset selected
     */
    public boolean hasMoveset() {
        return movesetId != null;
    }

    /**
     * Checks if the player has a breathing technique moveset
     */
    public boolean hasBreathingMoveset() {
        AbstractMoveset moveset = getMoveset();
        return moveset != null && moveset.isBreathingMoveset();
    }

    /**
     * Checks if the player has a demon art moveset
     */
    public boolean hasDemonMoveset() {
        AbstractMoveset moveset = getMoveset();
        return moveset != null && moveset.isDemonMoveset();
    }

    /**
     * Clears the current moveset
     */
    public void clearMoveset() {
        if (isApplyingModifiers) {
            return;
        }

        try {
            isApplyingModifiers = true;

            // Handle statistics and modifiers before clearing
            if (this.currentMoveset != null && this.player != null) {
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onMovesetUnequipped(this.currentMoveset.getMovesetId());
                }

                // Only remove modifiers for breathing techniques
                if (this.currentMoveset.isBreathingMoveset()) {
                    try {
                        this.currentMoveset.removeAllModifiers(this.player);
                        System.out.println("DEBUG: Removed breathing modifiers for cleared moveset from player " + getPlayerName());
                    } catch (Exception e) {
                        System.err.println("Error removing modifiers during clearMoveset: " + e.getMessage());
                    }
                } else {
                    System.out.println("DEBUG: Cleared demon moveset (no modifiers) from player " + getPlayerName());
                }
            }

            this.currentMoveset = null;
            this.movesetId = null;
        } finally {
            isApplyingModifiers = false;
        }
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

            // Remove old modifiers
            if (this.currentMoveset != null && this.player != null) {
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onMovesetUnequipped(this.currentMoveset.getMovesetId());
                }

                // Only remove modifiers for breathing techniques
                if (this.currentMoveset.isBreathingMoveset()) {
                    try {
                        this.currentMoveset.removeAllModifiers(this.player);
                    } catch (Exception e) {
                        System.err.println("Error removing modifiers during copyFrom: " + e.getMessage());
                    }
                }
            }

            this.movesetId = other.getMovesetId();
            this.currentMoveset = null; // Force reload from registry

        } finally {
            isApplyingModifiers = false;
        }

        // Load and apply new moveset
        getMoveset();
    }

    /**
     * Saves data to NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (movesetId != null) {
            tag.putString("MovesetId", movesetId);
        }
        return tag;
    }

    /**
     * Loads data from NBT
     */
    public void load(CompoundTag tag) {
        if (isApplyingModifiers) {
            return;
        }

        if (tag.contains("MovesetId")) {
            this.movesetId = tag.getString("MovesetId");
            // Don't load the moveset instance yet - do it lazily
            this.currentMoveset = null;
        } else {
            // Clear data if no moveset ID found
            clearMoveset();
        }
    }

    /**
     * Gets the statistics tracking system (requires PlayerDataProvider access)
     */
    private MovesetStatistics getStatistics() {
        if (player != null) {
            try {
                // Access statistics through PlayerDataProvider
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

        if (this.currentMoveset != null && this.player != null) {
            var statistics = getStatistics();
            if (statistics != null) {
                statistics.onMovesetUnequipped(this.currentMoveset.getMovesetId());
            }

            // Only remove modifiers for breathing techniques
            if (this.currentMoveset.isBreathingMoveset()) {
                try {
                    this.currentMoveset.removeAllModifiers(this.player);
                    System.out.println("DEBUG: Cleaned up breathing modifiers for player " + getPlayerName());
                } catch (Exception e) {
                    System.err.println("Error during cleanup: " + e.getMessage());
                }
            } else {
                System.out.println("DEBUG: Cleaned up demon moveset (no modifiers) for player " + getPlayerName());
            }
        }
    }

    /**
     * Force reapply modifiers (useful for troubleshooting breathing techniques)
     */
    public void reapplyModifiers() {
        if (this.currentMoveset != null && this.player != null && !isApplyingModifiers && this.currentMoveset.isBreathingMoveset()) {
            try {
                isApplyingModifiers = true;
                this.currentMoveset.applyAllModifiers(this.player);
                System.out.println("DEBUG: Force reapplied breathing modifiers for " + movesetId + " to player " + getPlayerName());
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