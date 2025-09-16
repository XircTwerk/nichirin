package com.xirc.nichirin.client.data;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-side cache for moveset unlock status (breathing techniques and demon arts)
 */
public class ClientProgressionCache {

    private static Set<String> unlockedMovesets = new HashSet<>();

    public static void setUnlockedMovesets(Set<String> movesets) {
        unlockedMovesets.clear();
        unlockedMovesets.addAll(movesets);
    }

    public static boolean isUnlocked(String movesetId) {
        return unlockedMovesets.contains(movesetId);
    }

    public static boolean isBreathingStyleUnlocked(String movesetId) {
        return movesetId.contains("breathing") && isUnlocked(movesetId);
    }

    public static boolean isDemonArtUnlocked(String movesetId) {
        return movesetId.contains("demon") && isUnlocked(movesetId);
    }

    public static Set<String> getUnlockedMovesets() {
        return new HashSet<>(unlockedMovesets);
    }

    public static Set<String> getUnlockedBreathingStyles() {
        Set<String> breathingStyles = new HashSet<>();
        for (String moveset : unlockedMovesets) {
            if (moveset.contains("breathing")) {
                breathingStyles.add(moveset);
            }
        }
        return breathingStyles;
    }

    public static Set<String> getUnlockedDemonArts() {
        Set<String> demonArts = new HashSet<>();
        for (String moveset : unlockedMovesets) {
            if (moveset.contains("demon")) {
                demonArts.add(moveset);
            }
        }
        return demonArts;
    }

    public static void clear() {
        unlockedMovesets.clear();
    }

    // Legacy methods for backwards compatibility

    /**
     * @deprecated Use setUnlockedMovesets instead
     */
    @Deprecated
    public static void setUnlockedStyles(Set<String> styles) {
        setUnlockedMovesets(styles);
    }
}