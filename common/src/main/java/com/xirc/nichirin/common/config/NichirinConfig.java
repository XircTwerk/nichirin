package com.xirc.nichirin.common.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

/**
 * Lightweight runtime config for Breath of Nichirin.
 * Values live in memory and can be changed via {@code /nichirin config set}.
 * They reset to defaults on server restart (no file I/O by design — keep it simple).
 */
public class NichirinConfig {

    // Keys (string constants so the command can reference them)

    public static final String PARRY_WINDOW_TICKS      = "parry_window_ticks";
    public static final String STAMINA_REGEN_RATE      = "stamina_regen_rate";
    public static final String FIRE_DAMAGE_INTERRUPTS_MOVES = "fire_damage_interrupts_moves";
    public static final String FALL_DAMAGE_INTERRUPTS_MOVES = "fall_damage_interrupts_moves";
    public static final String PROJECTILE_DAMAGE_INTERRUPTS_MOVES = "projectile_damage_interrupts_moves";
    public static final String EXPLOSION_DAMAGE_INTERRUPTS_MOVES = "explosion_damage_interrupts_moves";
    public static final String MAGIC_DAMAGE_INTERRUPTS_MOVES = "magic_damage_interrupts_moves";
    public static final String DROWNING_DAMAGE_INTERRUPTS_MOVES = "drowning_damage_interrupts_moves";
    public static final String STARVATION_DAMAGE_INTERRUPTS_MOVES = "starvation_damage_interrupts_moves";
    public static final String WISTERIA_DAMAGES_DEMONS = "wisteria_damages_demons";

    // Defaults

    private static final Map<String, Entry> entries = new LinkedHashMap<>();

    static {
        register(PARRY_WINDOW_TICKS,      10,   1,  30, "How many ticks after raising block count as a parry window");
        register(STAMINA_REGEN_RATE,       8,   1,  20, "Stamina points regenerated per second");
        registerBool(FIRE_DAMAGE_INTERRUPTS_MOVES, true, "Whether fire damage should end moves early");
        registerBool(FALL_DAMAGE_INTERRUPTS_MOVES, true, "Whether fall damage should end moves early");
        registerBool(PROJECTILE_DAMAGE_INTERRUPTS_MOVES, true, "Whether projectile damage should end moves early");
        registerBool(EXPLOSION_DAMAGE_INTERRUPTS_MOVES, true, "Whether explosion damage should end moves early");
        registerBool(MAGIC_DAMAGE_INTERRUPTS_MOVES, true, "Whether magic damage should end moves early");
        registerBool(DROWNING_DAMAGE_INTERRUPTS_MOVES, true, "Whether drowning damage should end moves early");
        registerBool(STARVATION_DAMAGE_INTERRUPTS_MOVES, true, "Whether starvation damage should end moves early");
        registerBool(WISTERIA_DAMAGES_DEMONS, true, "Whether Wisteria's Grace damages demons");

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
                case PARRY_WINDOW_TICKS      -> cfg.combat.parryWindowTicks;
                case STAMINA_REGEN_RATE      -> cfg.stamina.staminaRegenRate;
                case FIRE_DAMAGE_INTERRUPTS_MOVES -> cfg.moveInterrupts.fireDamageInterruptsMoves ? 1 : 0;
                case FALL_DAMAGE_INTERRUPTS_MOVES -> cfg.moveInterrupts.fallDamageInterruptsMoves ? 1 : 0;
                case PROJECTILE_DAMAGE_INTERRUPTS_MOVES -> cfg.moveInterrupts.projectileDamageInterruptsMoves ? 1 : 0;
                case EXPLOSION_DAMAGE_INTERRUPTS_MOVES -> cfg.moveInterrupts.explosionDamageInterruptsMoves ? 1 : 0;
                case MAGIC_DAMAGE_INTERRUPTS_MOVES -> cfg.moveInterrupts.magicDamageInterruptsMoves ? 1 : 0;
                case DROWNING_DAMAGE_INTERRUPTS_MOVES -> cfg.moveInterrupts.drowningDamageInterruptsMoves ? 1 : 0;
                case STARVATION_DAMAGE_INTERRUPTS_MOVES -> cfg.moveInterrupts.starvationDamageInterruptsMoves ? 1 : 0;
                case WISTERIA_DAMAGES_DEMONS -> cfg.demon.wisteriaDamagesDemons ? 1 : 0;
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
        setServerValue(key, value);
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
        setServerValue(key, value);
        return true;
    }

    /** Resets a single key to its default. */
    public static boolean reset(String key) {
        Entry e = entries.get(key);
        if (e == null) return false;
        entries.put(key, new Entry(e.defaultValue, e.defaultValue, e.min, e.max, e.description, e.isBoolean));
        setServerValue(key, e.defaultValue);
        return true;
    }

    /** Resets every key to its default. */
    public static void resetAll() {
        entries.replaceAll((k, e) -> new Entry(e.defaultValue, e.defaultValue, e.min, e.max, e.description, e.isBoolean));
        for (Map.Entry<String, Entry> entry : entries.entrySet()) {
            setServerValue(entry.getKey(), entry.getValue().defaultValue);
        }
    }

    /** Returns an ordered snapshot of all entries for display. */
    public static Map<String, Entry> getAll() {
        syncEntriesFromServer();
        return Collections.unmodifiableMap(entries);
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

    private static void setServerValue(String key, int value) {
        try {
            NichirinModConfig cfg = NichirinServerConfig.get();
            switch (key) {
                case PARRY_WINDOW_TICKS -> cfg.combat.parryWindowTicks = value;
                case STAMINA_REGEN_RATE -> cfg.stamina.staminaRegenRate = value;
                case FIRE_DAMAGE_INTERRUPTS_MOVES -> cfg.moveInterrupts.fireDamageInterruptsMoves = value != 0;
                case FALL_DAMAGE_INTERRUPTS_MOVES -> cfg.moveInterrupts.fallDamageInterruptsMoves = value != 0;
                case PROJECTILE_DAMAGE_INTERRUPTS_MOVES -> cfg.moveInterrupts.projectileDamageInterruptsMoves = value != 0;
                case EXPLOSION_DAMAGE_INTERRUPTS_MOVES -> cfg.moveInterrupts.explosionDamageInterruptsMoves = value != 0;
                case MAGIC_DAMAGE_INTERRUPTS_MOVES -> cfg.moveInterrupts.magicDamageInterruptsMoves = value != 0;
                case DROWNING_DAMAGE_INTERRUPTS_MOVES -> cfg.moveInterrupts.drowningDamageInterruptsMoves = value != 0;
                case STARVATION_DAMAGE_INTERRUPTS_MOVES -> cfg.moveInterrupts.starvationDamageInterruptsMoves = value != 0;
                case WISTERIA_DAMAGES_DEMONS -> cfg.demon.wisteriaDamagesDemons = value != 0;
                default -> {
                    return;
                }
            }
            NichirinServerConfig.save();
        } catch (Exception ignored) {
        }
    }

    private static void syncEntriesFromServer() {
        try {
            NichirinModConfig cfg = NichirinModConfig.get();
            syncEntry(PARRY_WINDOW_TICKS, cfg.combat.parryWindowTicks);
            syncEntry(STAMINA_REGEN_RATE, cfg.stamina.staminaRegenRate);
            syncEntry(FIRE_DAMAGE_INTERRUPTS_MOVES, cfg.moveInterrupts.fireDamageInterruptsMoves ? 1 : 0);
            syncEntry(FALL_DAMAGE_INTERRUPTS_MOVES, cfg.moveInterrupts.fallDamageInterruptsMoves ? 1 : 0);
            syncEntry(PROJECTILE_DAMAGE_INTERRUPTS_MOVES, cfg.moveInterrupts.projectileDamageInterruptsMoves ? 1 : 0);
            syncEntry(EXPLOSION_DAMAGE_INTERRUPTS_MOVES, cfg.moveInterrupts.explosionDamageInterruptsMoves ? 1 : 0);
            syncEntry(MAGIC_DAMAGE_INTERRUPTS_MOVES, cfg.moveInterrupts.magicDamageInterruptsMoves ? 1 : 0);
            syncEntry(DROWNING_DAMAGE_INTERRUPTS_MOVES, cfg.moveInterrupts.drowningDamageInterruptsMoves ? 1 : 0);
            syncEntry(STARVATION_DAMAGE_INTERRUPTS_MOVES, cfg.moveInterrupts.starvationDamageInterruptsMoves ? 1 : 0);
            syncEntry(WISTERIA_DAMAGES_DEMONS, cfg.demon.wisteriaDamagesDemons ? 1 : 0);
        } catch (Exception ignored) {
        }
    }

    private static void syncEntry(String key, int value) {
        Entry e = entries.get(key);
        if (e != null) {
            entries.put(key, new Entry(value, e.defaultValue, e.min, e.max, e.description, e.isBoolean));
        }
    }
}
