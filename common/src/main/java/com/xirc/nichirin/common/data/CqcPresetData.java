package com.xirc.nichirin.common.data;

import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;

/**
 * Player-owned CQC loadout. Presets are intentionally small: left/right click plus five wheel moves.
 */
public class CqcPresetData {

    public static final int WHEEL_SLOT_COUNT = 5;

    private String leftClickMove = "jab";
    private String rightClickMove = "cross";
    private String crouchRightClickMove = "low_kick";
    private final String[] wheelMoves = {
            "uppercut",
            "elbow_strike",
            "throat_chop",
            "spinning_backfist",
            "axe_kick"
    };

    public String getLeftClickMove() {
        return leftClickMove;
    }

    public String getRightClickMove() {
        return rightClickMove;
    }

    public String getCrouchRightClickMove() {
        return crouchRightClickMove;
    }

    public String getWheelMove(int index) {
        if (index < 0 || index >= WHEEL_SLOT_COUNT) return null;
        return wheelMoves[index];
    }

    public String[] getWheelMovesCopy() {
        return Arrays.copyOf(wheelMoves, wheelMoves.length);
    }

    public boolean setSlot(Slot slot, int wheelIndex, String moveId) {
        String normalized = CqcMoveCatalog.normalize(moveId);
        if (!CqcMoveCatalog.contains(normalized)) return false;

        switch (slot) {
            case LEFT_CLICK -> leftClickMove = normalized;
            case RIGHT_CLICK -> rightClickMove = normalized;
            case CROUCH_RIGHT_CLICK -> crouchRightClickMove = normalized;
            case WHEEL -> {
                if (wheelIndex < 0 || wheelIndex >= WHEEL_SLOT_COUNT) return false;
                wheelMoves[wheelIndex] = normalized;
            }
        }
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("LeftClick", leftClickMove);
        tag.putString("RightClick", rightClickMove);
        tag.putString("CrouchRightClick", crouchRightClickMove);
        for (int i = 0; i < WHEEL_SLOT_COUNT; i++) {
            tag.putString("Wheel" + i, wheelMoves[i]);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("LeftClick")) setSlot(Slot.LEFT_CLICK, -1, tag.getString("LeftClick"));
        if (tag.contains("RightClick")) setSlot(Slot.RIGHT_CLICK, -1, tag.getString("RightClick"));
        if (tag.contains("CrouchRightClick")) setSlot(Slot.CROUCH_RIGHT_CLICK, -1, tag.getString("CrouchRightClick"));
        for (int i = 0; i < WHEEL_SLOT_COUNT; i++) {
            if (tag.contains("Wheel" + i)) setSlot(Slot.WHEEL, i, tag.getString("Wheel" + i));
        }
    }

    public void copyFrom(CqcPresetData other) {
        this.leftClickMove = other.leftClickMove;
        this.rightClickMove = other.rightClickMove;
        this.crouchRightClickMove = other.crouchRightClickMove;
        System.arraycopy(other.wheelMoves, 0, this.wheelMoves, 0, WHEEL_SLOT_COUNT);
    }

    public enum Slot {
        LEFT_CLICK,
        RIGHT_CLICK,
        CROUCH_RIGHT_CLICK,
        WHEEL;

        public static Slot fromWireName(String name) {
            if (name == null) return WHEEL;
            return switch (name) {
                case "LEFT_CLICK" -> LEFT_CLICK;
                case "RIGHT_CLICK" -> RIGHT_CLICK;
                case "CROUCH_RIGHT_CLICK" -> CROUCH_RIGHT_CLICK;
                default -> WHEEL;
            };
        }
    }
}
