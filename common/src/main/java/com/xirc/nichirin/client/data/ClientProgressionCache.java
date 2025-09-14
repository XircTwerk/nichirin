package com.xirc.nichirin.client.data;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-side cache for breathing style unlock status
 */
public class ClientProgressionCache {

    private static Set<String> unlockedStyles = new HashSet<>();

    public static void setUnlockedStyles(Set<String> styles) {
        unlockedStyles.clear();
        unlockedStyles.addAll(styles);
    }

    public static boolean isUnlocked(String styleId) {
        return unlockedStyles.contains(styleId);
    }

    public static void clear() {
        unlockedStyles.clear();
    }
}