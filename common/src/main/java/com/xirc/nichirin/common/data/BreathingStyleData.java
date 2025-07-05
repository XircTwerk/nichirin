package com.xirc.nichirin.common.data;

import com.xirc.nichirin.common.attack.moveset.AbstractMoveset;
import net.minecraft.nbt.CompoundTag;
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

    public BreathingStyleData() {
        this.currentMoveset = null;
        this.movesetId = null;
    }

    /**
     * Sets the current breathing style moveset
     */
    public void setMoveset(@Nullable AbstractMoveset moveset) {
        this.currentMoveset = moveset;
        this.movesetId = moveset != null ? moveset.getMovesetId() : null;
    }

    /**
     * Gets the current breathing style moveset
     */
    @Nullable
    public AbstractMoveset getMoveset() {
        // If we only have an ID but no moveset instance, try to load it
        if (currentMoveset == null && movesetId != null) {
            currentMoveset = MovesetRegistry.getMoveset(movesetId);
        }
        return currentMoveset;
    }

    /**
     * Sets the moveset by its ID
     */
    public void setMovesetId(@Nullable String movesetId) {
        this.movesetId = movesetId;
        // Clear the moveset instance to force reload
        this.currentMoveset = null;
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
        this.currentMoveset = null;
        this.movesetId = null;
    }

    /**
     * Copies data from another instance
     */
    public void copyFrom(BreathingStyleData other) {
        this.movesetId = other.getMovesetId();
        this.currentMoveset = other.getMoveset();
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
}