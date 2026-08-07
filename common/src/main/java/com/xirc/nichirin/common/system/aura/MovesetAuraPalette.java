package com.xirc.nichirin.common.system.aura;

import com.xirc.nichirin.common.util.enums.BreathingStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Per-moveset aura colour/radius palette.
 *
 * <p>Breathing styles pull their colour straight from {@link BreathingStyle} so the aura matches
 * the canonical style colour used by every other UI surface (HUD, cooldowns, particles). Demon
 * arts have a fixed deep-red default. Destructive Death is explicitly excluded — it has its own
 * dynamic ticker.</p>
 */
public final class MovesetAuraPalette {

    public static final Set<String> SKIP_IDS = Set.of(
            "destructive_death"
    );

    private static final float DEFAULT_ALPHA = 1.0f;
    private static final float DEFAULT_RADIUS = 2.0f;
    private static final float DEFAULT_JITTER = 2.4f;

    private static final Map<String, Entry> OVERRIDES = new HashMap<>();
    static {
        OVERRIDES.put("default_demon",       rgb(0.85f, 0.05f, 0.10f));
        // Breathing-style auras — explicit RGB so the aura colour never depends on whatever bit
        // order the BreathingStyle.color int happens to use for HUD/cooldowns.
        OVERRIDES.put("water_breathing",     rgb(0.05f, 0.50f, 1.00f));
        OVERRIDES.put("flame_breathing",     rgb(1.00f, 0.22f, 0.00f));
        OVERRIDES.put("thunder_breathing",   rgb(1.00f, 0.82f, 0.00f));
        OVERRIDES.put("mist_breathing",      rgb(0.55f, 0.80f, 1.00f));
        OVERRIDES.put("insect_breathing",    rgb(0.65f, 0.10f, 0.90f));
        OVERRIDES.put("sound_breathing",     rgb(1.00f, 0.20f, 0.65f));
        OVERRIDES.put("beast_breathing",     rgb(0.35f, 0.65f, 0.90f));
        OVERRIDES.put("wind_breathing",      rgb(0.60f, 0.98f, 0.60f));
        OVERRIDES.put("stone_breathing",     rgb(0.50f, 0.50f, 0.50f));
        OVERRIDES.put("love_breathing",      rgb(1.00f, 0.41f, 0.71f));
        OVERRIDES.put("serpent_breathing",   rgb(0.55f, 0.00f, 0.55f));
        OVERRIDES.put("moon_breathing",      rgb(0.10f, 0.10f, 0.50f));
        OVERRIDES.put("sun_breathing",       rgb(1.00f, 0.65f, 0.00f));
        // NPC Akaza uses the exact same pure-blue / pure-red aura palette as player DD.
        OVERRIDES.put("akaza",               rgb(0.00f, 0.00f, 1.00f));
        OVERRIDES.put("akaza_overdrive",      rgb(1.00f, 0.00f, 0.05f));
    }

    private static Entry rgb(float r, float g, float b) {
        return new Entry(r, g, b, DEFAULT_ALPHA, DEFAULT_RADIUS, DEFAULT_JITTER);
    }

    private MovesetAuraPalette() {}

    public static Entry get(String movesetId) {
        if (movesetId == null) return null;
        Entry override = OVERRIDES.get(movesetId);
        if (override != null) return override;

        BreathingStyle style = BreathingStyle.fromMovesetId(movesetId);
        if (style == null) return null;
        float[] rgb = style.getColorAsFloat();
        return new Entry(rgb[0], rgb[1], rgb[2], DEFAULT_ALPHA, DEFAULT_RADIUS, DEFAULT_JITTER);
    }

    public record Entry(float r, float g, float b, float alpha, float radius, float jitter) {}
}
