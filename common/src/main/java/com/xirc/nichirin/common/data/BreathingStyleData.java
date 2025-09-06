package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.registry.MovesetRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Data class for storing player's selected breathing style
 * Handles modifiers and statistics tracking automatically
 * FIXED: Race conditions and multiplayer synchronization issues
 */
public class BreathingStyleData {

    @Nullable
    private AbstractMoveset currentMoveset;

    @Nullable
    private String movesetId;

    // Keep track of the player for modifiers and statistics
    @Nullable
    private Player player;

    // Flag to prevent recursive operations during modifier application
    private boolean isApplyingModifiers = false;

    public BreathingStyleData() {
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
     * Sets the current breathing style moveset
     */
    public void setMoveset(@Nullable AbstractMoveset moveset) {
        // Prevent recursive calls during modifier application
        if (isApplyingModifiers) {
            return;
        }

        try {
            isApplyingModifiers = true;

            // Handle statistics tracking and modifiers for old style
            if (this.currentMoveset != null && this.player != null) {
                // Record unequip time for old style
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onStyleUnequipped(this.currentMoveset.getMovesetId());
                }

                // Remove old modifiers
                try {
                    this.currentMoveset.removeAllModifiers(this.player);
                } catch (Exception e) {
                    System.err.println("Error removing modifiers for " + this.currentMoveset.getMovesetId() + ": " + e.getMessage());
                }
            }

            this.currentMoveset = moveset;
            this.movesetId = moveset != null ? moveset.getMovesetId() : null;

            // Handle statistics tracking and modifiers for new style
            if (this.currentMoveset != null && this.player != null) {
                // Record equip time for new style
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onStyleEquipped(this.currentMoveset.getMovesetId());
                }

                // Apply new modifiers
                try {
                    this.currentMoveset.applyAllModifiers(this.player);
                    System.out.println("DEBUG: Applied all modifiers for moveset: " + movesetId + " to player " + getPlayerName());
                } catch (Exception e) {
                    System.err.println("Error applying modifiers for " + movesetId + ": " + e.getMessage());
                }
            }
        } finally {
            isApplyingModifiers = false;
        }
    }

    /**
     * Gets the current breathing style moveset
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
                        currentMoveset.applyAllModifiers(player);
                        var statistics = getStatistics();
                        if (statistics != null) {
                            statistics.onStyleEquipped(movesetId);
                        }
                        System.out.println("DEBUG: Applied all modifiers for loaded moveset: " + movesetId + " to player " + getPlayerName());
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

            // Handle old style cleanup
            if (this.currentMoveset != null && this.player != null) {
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onStyleUnequipped(this.currentMoveset.getMovesetId());
                }
                try {
                    this.currentMoveset.removeAllModifiers(this.player);
                } catch (Exception e) {
                    System.err.println("Error removing modifiers during setMovesetId: " + e.getMessage());
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
                    statistics.onStyleUnequipped(this.currentMoveset.getMovesetId());
                }
                try {
                    this.currentMoveset.removeAllModifiers(this.player);
                    System.out.println("DEBUG: Removed all modifiers for cleared moveset from player " + getPlayerName());
                } catch (Exception e) {
                    System.err.println("Error removing modifiers during clearMoveset: " + e.getMessage());
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
    public void copyFrom(BreathingStyleData other) {
        if (isApplyingModifiers) {
            return;
        }

        try {
            isApplyingModifiers = true;

            // Remove old modifiers
            if (this.currentMoveset != null && this.player != null) {
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onStyleUnequipped(this.currentMoveset.getMovesetId());
                }
                try {
                    this.currentMoveset.removeAllModifiers(this.player);
                } catch (Exception e) {
                    System.err.println("Error removing modifiers during copyFrom: " + e.getMessage());
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
    private BreathingStyleStatistics getStatistics() {
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
                statistics.onStyleUnequipped(this.currentMoveset.getMovesetId());
            }
            try {
                this.currentMoveset.removeAllModifiers(this.player);
                System.out.println("DEBUG: Cleaned up modifiers for player " + getPlayerName());
            } catch (Exception e) {
                System.err.println("Error during cleanup: " + e.getMessage());
            }
        }
    }

    /**
     * Force reapply modifiers (useful for troubleshooting)
     */
    public void reapplyModifiers() {
        if (this.currentMoveset != null && this.player != null && !isApplyingModifiers) {
            try {
                isApplyingModifiers = true;
                this.currentMoveset.applyAllModifiers(this.player);
                System.out.println("DEBUG: Force reapplied modifiers for " + movesetId + " to player " + getPlayerName());
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