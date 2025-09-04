package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.registry.MovesetRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Data class for storing player's selected breathing style
 * Handles modifiers and statistics tracking automatically
 */
public class BreathingStyleData {

    @Nullable
    private AbstractMoveset currentMoveset;

    @Nullable
    private String movesetId;

    // Keep track of the player for modifiers and statistics
    @Nullable
    private Player player;

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
        // Handle statistics tracking and modifiers for old style
        if (this.currentMoveset != null && this.player != null) {
            // Record unequip time for old style
            var statistics = getStatistics();
            if (statistics != null) {
                statistics.onStyleUnequipped(this.currentMoveset.getMovesetId());
            }

            // Remove old modifiers
            this.currentMoveset.removeAllModifiers(this.player);
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
            this.currentMoveset.applyAllModifiers(this.player);
            System.out.println("DEBUG: Applied all modifiers for moveset: " + movesetId);
        }
    }

    /**
     * Gets the current breathing style moveset
     */
    @Nullable
    public AbstractMoveset getMoveset() {
        // If we only have an ID but no moveset instance, try to load it
        if (currentMoveset == null && movesetId != null) {
            currentMoveset = MovesetRegistry.getMoveset(movesetId);
            // Apply modifiers when loading
            if (currentMoveset != null && player != null) {
                currentMoveset.applyAllModifiers(player);
                var statistics = getStatistics();
                if (statistics != null) {
                    statistics.onStyleEquipped(movesetId);
                }
                System.out.println("DEBUG: Applied all modifiers for loaded moveset: " + movesetId);
            }
        }
        return currentMoveset;
    }

    /**
     * Sets the moveset by its ID
     */
    public void setMovesetId(@Nullable String movesetId) {
        // Handle old style cleanup
        if (this.currentMoveset != null && this.player != null) {
            var statistics = getStatistics();
            if (statistics != null) {
                statistics.onStyleUnequipped(this.currentMoveset.getMovesetId());
            }
            this.currentMoveset.removeAllModifiers(this.player);
        }

        this.movesetId = movesetId;
        // Clear the moveset instance to force reload
        this.currentMoveset = null;

        // Load and apply new moveset
        getMoveset(); // This will trigger loading and modifier application
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
        // Handle statistics and modifiers before clearing
        if (this.currentMoveset != null && this.player != null) {
            var statistics = getStatistics();
            if (statistics != null) {
                statistics.onStyleUnequipped(this.currentMoveset.getMovesetId());
            }
            this.currentMoveset.removeAllModifiers(this.player);
            System.out.println("DEBUG: Removed all modifiers for cleared moveset");
        }

        this.currentMoveset = null;
        this.movesetId = null;
    }

    /**
     * Copies data from another instance
     */
    public void copyFrom(BreathingStyleData other) {
        // Remove old modifiers
        if (this.currentMoveset != null && this.player != null) {
            var statistics = getStatistics();
            if (statistics != null) {
                statistics.onStyleUnequipped(this.currentMoveset.getMovesetId());
            }
            this.currentMoveset.removeAllModifiers(this.player);
        }

        this.movesetId = other.getMovesetId();
        this.currentMoveset = other.getMoveset();

        // Apply new modifiers
        if (this.currentMoveset != null && this.player != null) {
            this.currentMoveset.applyAllModifiers(this.player);
            var statistics = getStatistics();
            if (statistics != null) {
                statistics.onStyleEquipped(this.currentMoveset.getMovesetId());
            }
        }
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
            // Access statistics through PlayerDataProvider
            return com.xirc.nichirin.common.data.PlayerDataProvider.getData(player).getStatistics();
        }
        return null;
    }

    /**
     * Called when player disconnects/dies to clean up modifiers
     */
    public void cleanup() {
        if (this.currentMoveset != null && this.player != null) {
            var statistics = getStatistics();
            if (statistics != null) {
                statistics.onStyleUnequipped(this.currentMoveset.getMovesetId());
            }
            this.currentMoveset.removeAllModifiers(this.player);
        }
    }
}