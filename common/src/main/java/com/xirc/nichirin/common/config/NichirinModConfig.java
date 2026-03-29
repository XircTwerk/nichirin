package com.xirc.nichirin.common.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * Cloth Config / AutoConfig-backed configuration for Breath of Nichirin.
 *
 * <p>Saved to {@code .minecraft/config/nichirin.json} and editable through
 * the Cloth Config GUI (accessible via the Mods screen on Fabric/Forge).</p>
 *
 * <p>Use {@link #get()} anywhere in the codebase to read current values.</p>
 */
@Config(name = "nichirin")
public class NichirinModConfig implements ConfigData {

    // =========================================================================
    // Combat
    // =========================================================================

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public CombatConfig combat = new CombatConfig();

    public static class CombatConfig {

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 5, max = 100)
        public int comboWindowTicks = 20;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 30)
        public int parryWindowTicks = 10;

        @ConfigEntry.Gui.Tooltip
        public boolean enableParrySystem = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
        public int staminaRegenRate = 2;
    }

    // =========================================================================
    // Breathing
    // =========================================================================

    @ConfigEntry.Gui.CollapsibleObject
    public BreathingConfig breathing = new BreathingConfig();

    public static class BreathingConfig {

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 30)
        public int lightAttackStaminaCost = 10;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 50)
        public int heavyAttackStaminaCost = 20;

        @ConfigEntry.Gui.Tooltip
        public boolean unlimitedStamina = false;
    }

    // =========================================================================
    // Demon
    // =========================================================================

    @ConfigEntry.Gui.CollapsibleObject
    public DemonConfig demon = new DemonConfig();

    public static class DemonConfig {

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 5)
        public int bloodPointsOnKill = 1;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 5, max = 50)
        public int maxBloodPoints = 10;

        @ConfigEntry.Gui.Tooltip
        public boolean burnInSunlight = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
        public int sunDamagePerSecond = 2;

        @ConfigEntry.Gui.Tooltip
        public boolean bloodDrainEnabled = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
        public int bloodDrainIntervalSeconds = 30;
    }

    // =========================================================================
    // Kill Rewards
    // =========================================================================

    @ConfigEntry.Gui.CollapsibleObject
    public KillRewardsConfig killRewards = new KillRewardsConfig();

    public static class KillRewardsConfig {

        @ConfigEntry.Gui.Tooltip
        public boolean healOnKillPlayer = false;

        @ConfigEntry.Gui.Tooltip
        public boolean healOnKillPlayerAsMaxHealth = false;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 40)
        public int healOnKillPlayerAmount = 4;

        @ConfigEntry.Gui.Tooltip
        public boolean healOnKillMob = false;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
        public int healOnKillMobAmount = 2;

        @ConfigEntry.Gui.Tooltip
        public boolean resetCooldownsOnKill = false;

        @ConfigEntry.Gui.Tooltip
        public boolean resetStaminaOnKill = false;

        @ConfigEntry.Gui.Tooltip
        public boolean resetBreathOnKill = false;

        @ConfigEntry.Gui.Tooltip
        public boolean resetStanceOnKill = false;
    }

    // =========================================================================
    // Blood Moon
    // =========================================================================

    @ConfigEntry.Gui.CollapsibleObject
    public BloodMoonConfig bloodMoon = new BloodMoonConfig();

    public static class BloodMoonConfig {

        @ConfigEntry.Gui.Tooltip
        public boolean enabled = true;

        /** 1-in-N chance a blood moon occurs each night. Lower = rarer. */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 200)
        public int chancePerNight = 50;

        /** Percent attack damage boost applied to demons during a blood moon. */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int demonAttackBoostPercent = 30;

        /** Percent movement speed boost applied to demons during a blood moon. */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int demonSpeedBoostPercent = 15;

        /** Extra spawn weight added to demons during a blood moon (percent). */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int demonSpawnBoostPercent = 50;
    }

    // =========================================================================
    // Accessor
    // =========================================================================

    /**
     * Returns the live config instance. Safe to call at any time; returns a
     * default instance if AutoConfig hasn't been registered yet (e.g. during
     * very early start-up or if cloth-config isn't installed).
     */
    public static NichirinModConfig get() {
        try {
            return AutoConfig.getConfigHolder(NichirinModConfig.class).getConfig();
        } catch (Exception e) {
            return new NichirinModConfig();
        }
    }
}
