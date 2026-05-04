package com.xirc.nichirin.common.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight runtime config for Breath of Nichirin.
 * Values live in memory and can be changed via {@code /nichirin config set}.
 * They reset to defaults on server restart (no file I/O by design — keep it simple).
 */
public class NichirinConfig {

    // Keys (string constants so the command can reference them)

    public static final String COMBO_WINDOW_TICKS      = "combo_window_ticks";
    public static final String PARRY_WINDOW_TICKS      = "parry_window_ticks";
    public static final String STAMINA_REGEN_RATE      = "stamina_regen_rate";

    // Defaults

    private static final Map<String, Entry> entries = new LinkedHashMap<>();

    static {
        register(COMBO_WINDOW_TICKS,      20,   5, 100, "How long the STUNNED effect lasts (combo window in ticks)");
        register(PARRY_WINDOW_TICKS,      10,   1,  30, "How many ticks after raising block count as a parry window");
        register(STAMINA_REGEN_RATE,       2,   1,  20, "Stamina points regenerated per second");

    }

    private static void register(String key, int defaultValue, int min, int max, String description) {
        entries.put(key, new Entry(defaultValue, defaultValue, min, max, description, false));
    }

    private static void registerBool(String key, boolean defaultValue, String description) {
        int def = defaultValue ? 1 : 0;
        entries.put(key, new Entry(def, def, 0, 1, description, true));
    }

    // Accessors

    public static int getInt(String key) {
        // Prefer the file-backed Cloth Config value when available
        try {
            NichirinModConfig cfg = NichirinModConfig.get();
            Integer clothValue = switch (key) {
                case COMBO_WINDOW_TICKS      -> cfg.combat.comboWindowTicks;
                case PARRY_WINDOW_TICKS      -> cfg.combat.parryWindowTicks;
                case STAMINA_REGEN_RATE      -> cfg.combat.staminaRegenRate;
                default                      -> null;
            };
            if (clothValue != null) return clothValue;
        } catch (Exception ignored) {
            // AutoConfig not yet initialised — fall through to in-memory map
        }
        Entry e = entries.get(key);
        return e != null ? e.value : 0;
    }

    /**
     * Sets a config value from an int. Returns false if the key is unknown or value is out of range.
     */
    public static boolean set(String key, int value) {
        Entry e = entries.get(key);
        if (e == null) return false;
        if (value < e.min || value > e.max) return false;
        entries.put(key, new Entry(value, e.defaultValue, e.min, e.max, e.description, e.isBoolean));
        return true;
    }

    /**
     * Sets a config value from a string. Accepts "true"/"false" for boolean keys and integers
     * for all keys. Returns false if the key is unknown, the string is unparseable, or the
     * value is out of range.
     */
    public static boolean setString(String key, String raw) {
        Entry e = entries.get(key);
        if (e == null) return false;
        int value;
        if (raw.equalsIgnoreCase("true"))       value = 1;
        else if (raw.equalsIgnoreCase("false")) value = 0;
        else {
            try { value = Integer.parseInt(raw); }
            catch (NumberFormatException ex) { return false; }
        }
        if (value < e.min || value > e.max) return false;
        entries.put(key, new Entry(value, e.defaultValue, e.min, e.max, e.description, e.isBoolean));
        return true;
    }

    /** Resets a single key to its default. */
    public static boolean reset(String key) {
        Entry e = entries.get(key);
        if (e == null) return false;
        entries.put(key, new Entry(e.defaultValue, e.defaultValue, e.min, e.max, e.description, e.isBoolean));
        return true;
    }

    /** Resets every key to its default. */
    public static void resetAll() {
        entries.replaceAll((k, e) -> new Entry(e.defaultValue, e.defaultValue, e.min, e.max, e.description, e.isBoolean));
    }

    /** Returns an ordered snapshot of all entries for display. */
    public static Map<String, Entry> getAll() {
        return java.util.Collections.unmodifiableMap(entries);
    }

    public static boolean hasKey(String key) {
        return entries.containsKey(key);
    }

    // Entry record

    public record Entry(int value, int defaultValue, int min, int max, String description, boolean isBoolean) {
        /** Returns the display string: "true"/"false" for boolean entries, otherwise the raw int. */
        public String displayValue() {
            return isBoolean ? (value != 0 ? "true" : "false") : String.valueOf(value);
        }

        /** Returns the display string for the default value. */
        public String displayDefault() {
            return isBoolean ? (defaultValue != 0 ? "true" : "false") : String.valueOf(defaultValue);
        }
    }
}
