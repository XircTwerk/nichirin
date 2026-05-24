package com.xirc.nichirin.common.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.ArrayList;
import java.util.List;

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

    // Combat

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

        /** @deprecated moved to {@link StaminaConfig#staminaRegenRate} (#82). */
        @Deprecated
        @ConfigEntry.Gui.Excluded
        public int staminaRegenRate = 40;
    }

    // Stamina (#82)

    @ConfigEntry.Gui.CollapsibleObject
    public StaminaConfig stamina = new StaminaConfig();

    public static class StaminaConfig {

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
        public int staminaRegenRate = 40;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 30)
        public int lightAttackStaminaCost = 10;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 50)
        public int heavyAttackStaminaCost = 20;

        @ConfigEntry.Gui.Tooltip
        public boolean unlimitedStamina = false;
    }

    // Breathing (#83)

    @ConfigEntry.Gui.CollapsibleObject
    public BreathingConfig breathing = new BreathingConfig();

    public static class BreathingConfig {

        @ConfigEntry.Gui.Tooltip
        public boolean infiniteBreath = false;

        @ConfigEntry.Gui.Tooltip
        public boolean freeBreathMoves = false;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
        public int maxBreath = 100;

        /** Per-tick regen (so 1 = 20/sec). */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 50)
        public int breathRegenRate = 1;

        /** @deprecated moved to {@link StaminaConfig#lightAttackStaminaCost} (#82). */
        @Deprecated
        @ConfigEntry.Gui.Excluded
        public int lightAttackStaminaCost = 10;

        /** @deprecated moved to {@link StaminaConfig#heavyAttackStaminaCost} (#82). */
        @Deprecated
        @ConfigEntry.Gui.Excluded
        public int heavyAttackStaminaCost = 20;

        /** @deprecated moved to {@link StaminaConfig#unlimitedStamina} (#82). */
        @Deprecated
        @ConfigEntry.Gui.Excluded
        public boolean unlimitedStamina = false;
    }

    // Demon

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

    // Kill Rewards

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

    // Blood Moon

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

    // Perks

    @ConfigEntry.Gui.CollapsibleObject
    public PerkConfig perks = new PerkConfig();

    public static class PerkConfig {

        @ConfigEntry.Gui.Tooltip
        public boolean enablePerks = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableFlawSystem = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
        public int maxEquippedPerks = 5;

        /** Number of perks a player can equip before they must also equip a flaw. */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
        public int perksBeforeFlaws = 3;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 5)
        public int maxFlaws = 3;

        /** Maximum tier level (0=Common … 4=Legendary) perks can be upgraded to. */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 4)
        public int maxTier = 4;

        /** List of perk IDs that are disabled and cannot be discovered or equipped. */
        @ConfigEntry.Gui.Tooltip
        public List<String> disabledPerkIds = new ArrayList<>();
    }

    // Accessor

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
