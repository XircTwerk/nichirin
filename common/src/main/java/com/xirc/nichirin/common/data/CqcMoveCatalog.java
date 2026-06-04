package com.xirc.nichirin.common.data;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Server-authoritative catalog of CQC moves that can be assigned to presets.
 */
public final class CqcMoveCatalog {

    private static final Map<String, Definition> MOVES = new LinkedHashMap<>();

    static {
        add("jab", "Jab", 0.36f, 2.0f, 1.45f, 0.10f, 5, 12, "Quick straight punch.");
        add("cross", "Cross", 0.46f, 3.0f, 1.55f, 0.20f, 7, 18, "Committed straight punch.");
        add("lefthook", "Left Hook", 0.46f, 3.25f, 1.6f, 0.35f, 8, 20, "Short hook with stronger stagger.");
        add("roundhouse_fast", "Roundhouse Fast", 0.42f, 3.5f, 1.9f, 0.45f, 8, 24, "Fast kick with reach.");
        add("eye_poke", "Eye Poke", 0.34f, 1.5f, 1.25f, 0.05f, 14, 28, "Low damage interrupt with long stun.");
        add("throat_chop", "Throat Chop", 0.40f, 2.5f, 1.35f, 0.15f, 16, 30, "Close interrupt that briefly locks down a target.");
        add("headkick", "Headkick", 0.70f, 5.5f, 2.0f, 0.65f, 10, 46, "Heavy high kick.");
        add("spinning_backfist", "Spinning Backfist", 0.60f, 4.75f, 1.75f, 0.55f, 10, 42, "Spinning strike with solid knockback.");
        add("overhand_right", "Overhand Right", 0.50f, 4.0f, 1.55f, 0.45f, 9, 34, "Heavy downward punch.");
        add("uppercut", "Uppercut", 0.50f, 4.25f, 1.55f, 0.25f, 12, 38, 5.0f, "Dashing launcher. Dashes 5 blocks.");
        add("knee_strike", "Knee Strike", 0.60f, 4.0f, 1.3f, 0.2f, 12, 34, "Close-range knee with strong stun.");
        add("elbow_strike", "Elbow Strike", 0.40f, 3.25f, 1.25f, 0.25f, 10, 24, "Compact elbow strike.");
        add("spinning_heel_kick", "Spinning Heel Kick", 0.80f, 6.5f, 2.15f, 0.9f, 10, 56, "Slow, wide, heavy kick.");
        add("knee", "Knee", 0.50f, 3.5f, 1.25f, 0.25f, 12, 28, "Short knee attack.");
        add("axe_kick", "Axe Kick", 0.66f, 5.75f, 1.75f, 0.75f, 9, 48, "Heavy vertical kick.");
        add("low_kick", "Low Kick", 0.38f, 2.75f, 1.65f, 0.55f, 7, 22, "Fast leg kick with knockback.");
        add("superman_punch", "Superman Punch", 0.60f, 4.5f, 2.05f, 0.55f, 9, 44, "Leaping punch with reach.");
        add("double_palm", "Double Palm", 0.46f, 3.75f, 1.45f, 0.8f, 8, 32, "Two-handed shove.");
        add("backhand_slap", "Backhand Slap", 0.52f, 2.75f, 1.4f, 0.35f, 9, 24, "Fast backhand counter.");
    }

    private CqcMoveCatalog() {}

    public static Collection<Definition> all() {
        return MOVES.values().stream()
                .sorted(Comparator.comparing(Definition::displayName))
                .toList();
    }

    public static Definition get(String id) {
        return MOVES.get(normalize(id));
    }

    public static boolean contains(String id) {
        return get(id) != null;
    }

    public static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase().replace(' ', '_');
    }

    private static void add(String id, String displayName, float animationLengthSeconds,
                            float damage, float range, float knockback, int hitStun, int cooldown, String description) {
        add(id, displayName, id, animationLengthSeconds, damage, range, knockback, hitStun, cooldown, 0f, description);
    }

    private static void add(String id, String displayName, String animationName, float animationLengthSeconds,
                            float damage, float range, float knockback, int hitStun, int cooldown, String description) {
        add(id, displayName, animationName, animationLengthSeconds, damage, range, knockback, hitStun, cooldown, 0f, description);
    }

    private static void add(String id, String displayName, float animationLengthSeconds,
                            float damage, float range, float knockback, int hitStun, int cooldown,
                            float dashDistance, String description) {
        add(id, displayName, id, animationLengthSeconds, damage, range, knockback, hitStun, cooldown, dashDistance, description);
    }

    private static void add(String id, String displayName, String animationName, float animationLengthSeconds,
                            float damage, float range, float knockback, int hitStun, int cooldown,
                            float dashDistance, String description) {
        String normalized = normalize(id);
        MOVES.put(normalized, new Definition(normalized, displayName, animationName,
                Math.max(1, (int) Math.ceil(animationLengthSeconds * 20.0f)),
                damage, range, knockback, hitStun, cooldown, dashDistance, description));
    }

    public record Definition(String id, String displayName, String animationName, int durationTicks,
                             float damage, float range, float knockback, int hitStun, int cooldown,
                             float dashDistance, String description) {}
}
