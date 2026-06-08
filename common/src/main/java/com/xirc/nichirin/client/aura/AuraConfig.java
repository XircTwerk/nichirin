package com.xirc.nichirin.client.aura;

/**
 * Runtime-tunable aura visuals shared across all auras unless an individual AuraInstance
 * overrides the field. Designed so the debug command can hot-set values via setByName.
 *
 * Pure data holder — no client-only API references — so it's safe to load on dedicated
 * servers when the command tree references {@link #knownKeys()} for tab-completion.
 */
public final class AuraConfig {
    public static float shellThickness     = 0.22f;
    public static float opacityMultiplier  = 1.0f;
    public static float brightness         = 1.15f;
    public static float pulseAmplitude     = 0.08f;
    public static float waveAnimAmplitude  = 0.8f;
    public static float waveBase           = 0.6f;
    public static float lobeCountLow       = 4.0f;
    public static float lobeCountHigh      = 7.0f;
    public static float animationSpeed     = 1.0f;
    public static float pixelBlockSize     = 4.0f;
    public static int   posterizationLevels = 6;
    public static boolean pixelizationEnabled = false;

    private AuraConfig() {}

    /** Sets a config value by string name; returns true on success. */
    public static boolean setByName(String name, String value) {
        try {
            switch (name) {
                case "shellThickness"      -> shellThickness = Float.parseFloat(value);
                case "opacityMultiplier"   -> opacityMultiplier = Float.parseFloat(value);
                case "brightness"          -> brightness = Float.parseFloat(value);
                case "pulseAmplitude"      -> pulseAmplitude = Float.parseFloat(value);
                case "waveAnimAmplitude"   -> waveAnimAmplitude = Float.parseFloat(value);
                case "waveBase"            -> waveBase = Float.parseFloat(value);
                case "lobeCountLow"        -> lobeCountLow = Float.parseFloat(value);
                case "lobeCountHigh"       -> lobeCountHigh = Float.parseFloat(value);
                case "animationSpeed"      -> animationSpeed = Float.parseFloat(value);
                case "pixelBlockSize"      -> pixelBlockSize = Float.parseFloat(value);
                case "posterizationLevels" -> posterizationLevels = Integer.parseInt(value);
                case "pixelizationEnabled" -> pixelizationEnabled = Boolean.parseBoolean(value);
                default -> { return false; }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String[] knownKeys() {
        return new String[] {
                "shellThickness", "opacityMultiplier", "brightness",
                "pulseAmplitude", "waveAnimAmplitude", "waveBase",
                "lobeCountLow", "lobeCountHigh", "animationSpeed",
                "pixelBlockSize", "posterizationLevels", "pixelizationEnabled"
        };
    }
}
