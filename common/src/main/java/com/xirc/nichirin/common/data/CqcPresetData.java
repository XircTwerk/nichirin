package com.xirc.nichirin.common.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Player-owned CQC loadout. Presets are intentionally small: left/right click plus five wheel moves.
 */
public class CqcPresetData {

    public static final int WHEEL_SLOT_COUNT = 5;
    public static final int PRESET_COUNT = 7;

    private final Preset[] presets = createDefaultPresets();
    private int activePresetIndex = 0;

    /** Built-in defaults — also used by {@link #resetActivePreset()} to restore a preset. */
    private static Preset[] createDefaultPresets() {
        return new Preset[] {
                new Preset("Balanced", "jab", "cross", "low_kick", 0,
                        "uppercut", "elbow_strike", "throat_chop", "spinning_backfist", "axe_kick"),
                new Preset("Striker", "jab", "overhand_right", "roundhouse_fast", 1,
                        "cross", "lefthook", "superman_punch", "headkick", "spinning_heel_kick"),
                new Preset("Guarded", "eye_poke", "double_palm", "backhand_slap", 2,
                        "throat_chop", "low_kick", "knee", "elbow_strike", "spinning_backfist"),
                new Preset("Demon", "demon_gut_punch", "demon_slash", "high_jump", 0,
                        "demon_stomp", "demon_kick", "dashing_strike", "demon_bite", "demon_grab"),
                new Preset("Destructive Death", "snap_punch", "annihilation_type", "crown_splitter", 0,
                        "explosive_flurry", "flying_planet_thousand_wheels", "eight_layered_demon_core",
                        "ten_thousand_leaves_flashing_willow", "donut"),
                new Preset("Custom 1", "", "", "", 0, "", "", "", "", ""),
                new Preset("Custom 2", "", "", "", 0, "", "", "", "", "")
        };
    }

    /** Restores the active preset to its built-in default slots, stance, and followups. */
    public void resetActivePreset() {
        Preset[] defaults = createDefaultPresets();
        activePreset().copyFrom(defaults[Math.max(0, Math.min(PRESET_COUNT - 1, activePresetIndex))]);
    }

    public String getLeftClickMove() {
        return activePreset().leftClickMove;
    }

    public String getRightClickMove() {
        return activePreset().rightClickMove;
    }

    public String getCrouchRightClickMove() {
        return activePreset().crouchRightClickMove;
    }

    public String getWheelMove(int index) {
        if (index < 0 || index >= WHEEL_SLOT_COUNT) return null;
        return activePreset().wheelMoves[index];
    }

    public String[] getWheelMovesCopy() {
        return Arrays.copyOf(activePreset().wheelMoves, WHEEL_SLOT_COUNT);
    }

    public int getStanceIndex() {
        return activePreset().stanceIndex;
    }

    public String getStanceAnimation() {
        return "cqc_stance_" + (getStanceIndex() + 1);
    }

    public int getActivePresetIndex() {
        return activePresetIndex;
    }

    public boolean setActivePresetIndex(int index) {
        if (index < 0 || index >= PRESET_COUNT) return false;
        activePresetIndex = index;
        return true;
    }

    public String getPresetName(int index) {
        if (index < 0 || index >= PRESET_COUNT) return "";
        return presets[index].name;
    }

    public String getFollowupMove(String baseMoveId) {
        return activePreset().getFollowupMove(baseMoveId);
    }

    public Map<String, String> getFollowupsCopy() {
        return activePreset().followupsCopy();
    }

    public boolean setStanceIndex(int stanceIndex) {
        if (stanceIndex < 0 || stanceIndex > 3) return false;
        activePreset().stanceIndex = stanceIndex;
        return true;
    }

    public boolean setSlot(Slot slot, int wheelIndex, String moveId) {
        String normalized = CqcMoveCatalog.normalize(moveId);
        if (!normalized.isEmpty() && !CqcMoveCatalog.contains(normalized)) return false;

        Preset preset = activePreset();
        switch (slot) {
            case LEFT_CLICK -> preset.leftClickMove = normalized;
            case RIGHT_CLICK -> preset.rightClickMove = normalized;
            case CROUCH_RIGHT_CLICK -> preset.crouchRightClickMove = normalized;
            case WHEEL -> {
                if (wheelIndex < 0 || wheelIndex >= WHEEL_SLOT_COUNT) return false;
                preset.wheelMoves[wheelIndex] = normalized;
            }
        }
        return true;
    }

    public boolean setSlotForPlayer(Player player, Slot slot, int wheelIndex, String moveId) {
        String normalized = CqcMoveCatalog.normalize(moveId);
        if (!canAssign(player, normalized)) return false;
        return setSlot(slot, wheelIndex, normalized);
    }

    public boolean setFollowupForPlayer(Player player, String baseMoveId, String followupMoveId) {
        String normalizedBase = CqcMoveCatalog.normalize(baseMoveId);
        String normalizedFollowup = CqcMoveCatalog.normalize(followupMoveId);
        if (!canHaveFollowup(normalizedBase)) return false;
        if (!canAssign(player, normalizedFollowup)) return false;
        activePreset().setFollowupMove(normalizedBase, normalizedFollowup);
        return true;
    }

    public static boolean canAssign(Player player, String moveId) {
        String normalized = CqcMoveCatalog.normalize(moveId);
        if (normalized.isEmpty()) return true;
        CqcMoveCatalog.Definition definition = CqcMoveCatalog.get(moveId);
        if (definition == null) return false;
        return !definition.demonOnly() || com.xirc.nichirin.common.system.DemonManager.isDemon(player);
    }

    public static boolean canHaveFollowup(String baseMoveId) {
        String normalized = CqcMoveCatalog.normalize(baseMoveId);
        if (normalized.isEmpty() || !CqcMoveCatalog.contains(normalized)) return false;
        return !"demon_slash".equals(normalized);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("ActivePresetIndex", activePresetIndex);
        for (int i = 0; i < PRESET_COUNT; i++) {
            tag.put("Preset" + i, presets[i].save());
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("Preset0")) {
            for (int i = 0; i < PRESET_COUNT; i++) {
                if (tag.contains("Preset" + i)) {
                    presets[i].load(tag.getCompound("Preset" + i));
                }
            }
            if (tag.contains("ActivePresetIndex")) setActivePresetIndex(tag.getInt("ActivePresetIndex"));
            return;
        }

        int previousActive = activePresetIndex;
        activePresetIndex = 0;
        if (tag.contains("LeftClick")) setSlot(Slot.LEFT_CLICK, -1, tag.getString("LeftClick"));
        if (tag.contains("RightClick")) setSlot(Slot.RIGHT_CLICK, -1, tag.getString("RightClick"));
        if (tag.contains("CrouchRightClick")) setSlot(Slot.CROUCH_RIGHT_CLICK, -1, tag.getString("CrouchRightClick"));
        for (int i = 0; i < WHEEL_SLOT_COUNT; i++) {
            if (tag.contains("Wheel" + i)) setSlot(Slot.WHEEL, i, tag.getString("Wheel" + i));
        }
        if (tag.contains("StanceIndex")) setStanceIndex(tag.getInt("StanceIndex"));
        activePresetIndex = previousActive;
    }

    public void copyFrom(CqcPresetData other) {
        this.activePresetIndex = other.activePresetIndex;
        for (int i = 0; i < PRESET_COUNT; i++) {
            this.presets[i].copyFrom(other.presets[i]);
        }
    }

    public Preset getPresetCopy(int index) {
        if (index < 0 || index >= PRESET_COUNT) return null;
        Preset copy = new Preset(presets[index].name, "", "", "", 0, "", "", "", "", "");
        copy.copyFrom(presets[index]);
        return copy;
    }

    public void setPresetFromNetwork(int index, Preset preset) {
        if (index < 0 || index >= PRESET_COUNT || preset == null) return;
        presets[index].copyFrom(preset);
    }

    private Preset activePreset() {
        return presets[Math.max(0, Math.min(PRESET_COUNT - 1, activePresetIndex))];
    }

    public static class Preset {
        private final String name;
        private String leftClickMove;
        private String rightClickMove;
        private String crouchRightClickMove;
        private int stanceIndex;
        private final String[] wheelMoves;
        private final Map<String, String> followups = new HashMap<>();

        public Preset(String name, String leftClickMove, String rightClickMove, String crouchRightClickMove,
                      int stanceIndex, String wheel0, String wheel1, String wheel2, String wheel3, String wheel4) {
            this.name = name;
            this.leftClickMove = CqcMoveCatalog.normalize(leftClickMove);
            this.rightClickMove = CqcMoveCatalog.normalize(rightClickMove);
            this.crouchRightClickMove = CqcMoveCatalog.normalize(crouchRightClickMove);
            this.stanceIndex = Math.max(0, Math.min(3, stanceIndex));
            this.wheelMoves = new String[] {
                    CqcMoveCatalog.normalize(wheel0),
                    CqcMoveCatalog.normalize(wheel1),
                    CqcMoveCatalog.normalize(wheel2),
                    CqcMoveCatalog.normalize(wheel3),
                    CqcMoveCatalog.normalize(wheel4)
            };
            if ("Demon".equals(name)) {
                setFollowupMove("high_jump", "demon_stomp");
            }
        }

        public String leftClickMove() { return leftClickMove; }
        public String rightClickMove() { return rightClickMove; }
        public String crouchRightClickMove() { return crouchRightClickMove; }
        public int stanceIndex() { return stanceIndex; }
        public String[] wheelMovesCopy() { return Arrays.copyOf(wheelMoves, WHEEL_SLOT_COUNT); }
        public Map<String, String> followupsCopy() { return new HashMap<>(followups); }

        public String getFollowupMove(String baseMoveId) {
            return followups.getOrDefault(CqcMoveCatalog.normalize(baseMoveId), "");
        }

        public void setFollowupMove(String baseMoveId, String followupMoveId) {
            String normalizedBase = CqcMoveCatalog.normalize(baseMoveId);
            String normalizedFollowup = CqcMoveCatalog.normalize(followupMoveId);
            if (!canHaveFollowup(normalizedBase)) return;
            if (normalizedFollowup.isEmpty()) {
                followups.remove(normalizedBase);
                return;
            }
            if (CqcMoveCatalog.contains(normalizedFollowup)) {
                followups.put(normalizedBase, normalizedFollowup);
            }
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putString("LeftClick", leftClickMove);
            tag.putString("RightClick", rightClickMove);
            tag.putString("CrouchRightClick", crouchRightClickMove);
            for (int i = 0; i < WHEEL_SLOT_COUNT; i++) {
                tag.putString("Wheel" + i, wheelMoves[i]);
            }
            tag.putInt("StanceIndex", stanceIndex);
            CompoundTag followupTag = new CompoundTag();
            for (Map.Entry<String, String> entry : followups.entrySet()) {
                followupTag.putString(entry.getKey(), entry.getValue());
            }
            tag.put("Followups", followupTag);
            return tag;
        }

        private void load(CompoundTag tag) {
            if (tag.contains("LeftClick")) leftClickMove = validOrEmpty(tag.getString("LeftClick"));
            if (tag.contains("RightClick")) rightClickMove = validOrEmpty(tag.getString("RightClick"));
            if (tag.contains("CrouchRightClick")) crouchRightClickMove = validOrEmpty(tag.getString("CrouchRightClick"));
            for (int i = 0; i < WHEEL_SLOT_COUNT; i++) {
                if (tag.contains("Wheel" + i)) wheelMoves[i] = validOrEmpty(tag.getString("Wheel" + i));
            }
            if (tag.contains("StanceIndex")) stanceIndex = Math.max(0, Math.min(3, tag.getInt("StanceIndex")));
            followups.clear();
            if (tag.contains("Followups")) {
                CompoundTag followupTag = tag.getCompound("Followups");
                for (String key : followupTag.getAllKeys()) {
                    setFollowupMove(key, followupTag.getString(key));
                }
            }
        }

        private void copyFrom(Preset other) {
            this.leftClickMove = other.leftClickMove;
            this.rightClickMove = other.rightClickMove;
            this.crouchRightClickMove = other.crouchRightClickMove;
            this.stanceIndex = other.stanceIndex;
            System.arraycopy(other.wheelMoves, 0, this.wheelMoves, 0, WHEEL_SLOT_COUNT);
            this.followups.clear();
            this.followups.putAll(other.followups);
        }

        private static String validOrEmpty(String moveId) {
            String normalized = CqcMoveCatalog.normalize(moveId);
            return normalized.isEmpty() || CqcMoveCatalog.contains(normalized) ? normalized : "";
        }
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
