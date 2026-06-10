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
    private static final float DEFAULT_RADIUS = 1.25f;
    private static final float DEFAULT_JITTER = 2.4f;

    private static final Map<String, Entry> DEMON_OVERRIDES = new HashMap<>();
    static {
        DEMON_OVERRIDES.put("default_demon",
                new Entry(0.85f, 0.05f, 0.10f, DEFAULT_ALPHA, DEFAULT_RADIUS, DEFAULT_JITTER));
    }

    private MovesetAuraPalette() {}

    public static Entry get(String movesetId) {
        if (movesetId == null) return null;
        Entry demon = DEMON_OVERRIDES.get(movesetId);
        if (demon != null) return demon;

        BreathingStyle style = BreathingStyle.fromMovesetId(movesetId);
        if (style == null) return null;
        float[] rgb = style.getColorAsFloat();
        return new Entry(rgb[0], rgb[1], rgb[2], DEFAULT_ALPHA, DEFAULT_RADIUS, DEFAULT_JITTER);
    }

    public record Entry(float r, float g, float b, float alpha, float radius, float jitter) {}
}
