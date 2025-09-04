package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import com.xirc.nichirin.registry.MovesetRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Data class for storing player's selected breathing style
 * Uses Architectury's platform-agnostic approach
 */
public class BreathingStyleData {

    @Nullable
    private AbstractMoveset currentMoveset;

    @Nullable
    private String movesetId;

    // Keep track of the player for speed modifiers
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
        // Remove old speed modifier if we had one
        if (this.currentMoveset != null && this.player != null) {
            this.currentMoveset.removeSpeedModifier(this.player);
        }

        this.currentMoveset = moveset;
        this.movesetId = moveset != null ? moveset.getMovesetId() : null;

        // Apply new speed modifier if we have one
        if (this.currentMoveset != null && this.player != null) {
            this.currentMoveset.applySpeedModifier(this.player);
            System.out.println("DEBUG: Applied speed modifier for moveset: " + movesetId);
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
            // Apply speed modifier when loading
            if (currentMoveset != null && player != null) {
                currentMoveset.applySpeedModifier(player);
                System.out.println("DEBUG: Applied speed modifier for loaded moveset: " + movesetId);
            }
        }
        return currentMoveset;
    }

    /**
     * Sets the moveset by its ID
     */
    public void setMovesetId(@Nullable String movesetId) {
        // Remove old speed modifier
        if (this.currentMoveset != null && this.player != null) {
            this.currentMoveset.removeSpeedModifier(this.player);
        }

        this.movesetId = movesetId;
        // Clear the moveset instance to force reload
        this.currentMoveset = null;

        // Load and apply new moveset
        getMoveset(); // This will trigger loading and speed modifier application
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
        // Remove speed modifier before clearing
        if (this.currentMoveset != null && this.player != null) {
            this.currentMoveset.removeSpeedModifier(this.player);
            System.out.println("DEBUG: Removed speed modifier for cleared moveset");
        }

        this.currentMoveset = null;
        this.movesetId = null;
    }

    /**
     * Copies data from another instance
     */
    public void copyFrom(BreathingStyleData other) {
        // Remove old speed modifier
        if (this.currentMoveset != null && this.player != null) {
            this.currentMoveset.removeSpeedModifier(this.player);
        }

        this.movesetId = other.getMovesetId();
        this.currentMoveset = other.getMoveset();

        // Apply new speed modifier
        if (this.currentMoveset != null && this.player != null) {
            this.currentMoveset.applySpeedModifier(this.player);
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
     * Called when player disconnects/dies to clean up speed modifiers
     */
    public void cleanup() {
        if (this.currentMoveset != null && this.player != null) {
            this.currentMoveset.removeSpeedModifier(this.player);
        }
    }
}