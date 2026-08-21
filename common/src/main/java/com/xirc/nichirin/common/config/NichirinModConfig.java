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
 * <p>Saved to {@code .minecraft/config/nichirin-server.toml} and editable through
 * the Cloth Config GUI (accessible via the Mods screen on Fabric/NeoForge).</p>
 *
 * <p>Use {@link #get()} anywhere in the codebase to read current values.</p>
 */
@Config(name = "nichirin-server")
public class NichirinModConfig implements ConfigData {

    // Combat

    @ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
    public CombatConfig combat = new CombatConfig();

    public static class CombatConfig {

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 30)
        public int parryWindowTicks = 10;

        @ConfigEntry.Gui.Tooltip
        public boolean enableParrySystem = true;

        /**
         * How landing consecutive combo hits scales a move's damage & stun:
         * 0 = flat (every hit deals the same), 1 = falloff (each successive hit deals less),
         * 2 = rampup (each successive hit deals more — the classic combo bonus).
         */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 2)
        public int comboDamageMode = 2;

        /** Percent damage/stun change per point of combo, for falloff & rampup modes (15 = ±15% per hit). */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int comboDamageRatePercent = 15;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 25)
        public int npcAiLevel = 25;

        /** Ticks an NPC must wait between dash impulses (stops infinite dashing). */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 60)
        public int npcDashCooldownTicks = 18;

        /** Ticks an NPC waits between mobility moves used to scale up to elevated targets. */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 120)
        public int npcMobilityCooldownTicks = 40;

        /** Health a trainer has during a formal duel (the boss-bar fight). */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 20, max = 1000)
        public int npcDuelHealth = 100;

        /** Seconds a trainer must recover before it can be challenged to another duel. */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 600)
        public int npcDuelCooldownSeconds = 180;

        /** Trainer health while peaceful (also the base MAX_HEALTH attribute). */
        @ConfigEntry.Gui.Tooltip
        public double npcPeacefulHealth = 200.0;

        /** Trainer health floor at which a non-lethal duel ends (trainer can't be killed in a spar). */
        @ConfigEntry.Gui.Tooltip
        public double npcDuelWinHpThreshold = 1.0;

        /** Minimum health a trainer leaves a defeated opponent at (mercy; 0.5 = half a heart). */
        @ConfigEntry.Gui.Tooltip
        public double npcPlayerDuelMinHealth = 0.5;

        /** Ticks a self-defense fight persists after the target is lost before reverting to peaceful. */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 400)
        public int npcSelfDefenseGraceTicks = 100;

        /** @deprecated moved to {@link StaminaConfig#staminaRegenRate}. */
        @Deprecated
        @ConfigEntry.Gui.Excluded
        public int staminaRegenRate = 8;
    }

    @ConfigEntry.Gui.CollapsibleObject
    public DamageConfig damage = new DamageConfig();

    public static class DamageConfig {

        @ConfigEntry.Gui.Tooltip
        public double baseDamageMultiplier = 1.0;

        @ConfigEntry.Gui.Tooltip
        public boolean percentageDamage = true;
    }

    // Movement

    @ConfigEntry.Gui.CollapsibleObject
    public MovementConfig movement = new MovementConfig();

    public static class MovementConfig {

        /** Horizontal impulse applied by a dash. */
        @ConfigEntry.Gui.Tooltip
        public double dashForce = 2.0;

        /** Upward velocity of a double jump. */
        @ConfigEntry.Gui.Tooltip
        public double doubleJumpVelocity = 0.63;

        /** Maximum distance (blocks) a backstep travels. */
        @ConfigEntry.Gui.Tooltip
        public double backstepDistance = 3.0;

        /** Horizontal burst distance of an air dodge. */
        @ConfigEntry.Gui.Tooltip
        public double airDodgeDistance = 0.5;

        /** Sprint movement-speed multiplier. */
        @ConfigEntry.Gui.Tooltip
        public double sprintMultiplier = 1.3;
    }

    // Blocking

    @ConfigEntry.Gui.CollapsibleObject
    public BlockingConfig blocking = new BlockingConfig();

    public static class BlockingConfig {

        /** Stance drained per tick while holding a block. */
        @ConfigEntry.Gui.Tooltip
        public double blockStanceDrain = 0.8;

        /** Stance consumed by a parry attempt. */
        @ConfigEntry.Gui.Tooltip
        public double parryStanceCost = 15.0;

        /** Ticks before you can block again after dropping guard. */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 60)
        public int blockCooldownTicks = 15;

        /** Self-stun ticks for releasing a block too early (failed parry). */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 40)
        public int earlyReleaseStunTicks = 4;

        /** Cone half-angle (degrees) behind the defender that counts as a backstab (ignores block). */
        @ConfigEntry.Gui.Tooltip
        public double backstabAngle = 90.0;
    }

    // Stamina

    @ConfigEntry.Gui.CollapsibleObject
    public StaminaConfig stamina = new StaminaConfig();

    public static class StaminaConfig {

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
        public int staminaRegenRate = 8;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 30)
        public int lightAttackStaminaCost = 10;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 50)
        public int heavyAttackStaminaCost = 20;

        @ConfigEntry.Gui.Tooltip
        public boolean unlimitedStamina = false;
    }

    // Breathing

    @ConfigEntry.Gui.CollapsibleObject
    public BreathingConfig breathing = new BreathingConfig();

    public static class BreathingConfig {

        @ConfigEntry.Gui.Tooltip
        public boolean infiniteBreath = false;

        @ConfigEntry.Gui.Tooltip
        public boolean freeBreathMoves = false;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 1000)
        public int maxBreath = 100;

        /** Per-tick regen (so 1 = 20/sec). */
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 50)
        public int breathRegenRate = 1;

        /** @deprecated moved to {@link StaminaConfig#lightAttackStaminaCost}. */
        @Deprecated
        @ConfigEntry.Gui.Excluded
        public int lightAttackStaminaCost = 10;

        /** @deprecated moved to {@link StaminaConfig#heavyAttackStaminaCost}. */
        @Deprecated
        @ConfigEntry.Gui.Excluded
        public int heavyAttackStaminaCost = 20;

        /** @deprecated moved to {@link StaminaConfig#unlimitedStamina}. */
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
        public boolean burnEntitiesInSunlight = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 20)
        public int sunDamagePerSecond = 2;

        @ConfigEntry.Gui.Tooltip
        public boolean wisteriaDamagesDemons = true;

        @ConfigEntry.Gui.Tooltip
        public boolean upperMoonPact = true;

        @ConfigEntry.Gui.Tooltip
        public boolean bloodDrainEnabled = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 1, max = 100)
        public int bloodDrainIntervalSeconds = 30;

        @ConfigEntry.Gui.Tooltip
        public boolean peacefulModeMaxBlood = true;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 100, max = 300)
        public int demonMaxBreathPercent = 150;

        /**
         * When true, dying as a demon strips demon status on respawn. Off by default so the
         * choice to drink demon blood is genuinely permanent — turn this on for servers that
         * want death to function as a "redemption arc" reset.
         */
        @ConfigEntry.Gui.Tooltip
        public boolean removeDemonOnDeath = false;
    }

    @ConfigEntry.Gui.CollapsibleObject
    public MoveInterruptConfig moveInterrupts = new MoveInterruptConfig();

    public static class MoveInterruptConfig {

        @ConfigEntry.Gui.Tooltip
        public boolean fireDamageInterruptsMoves = false;

        @ConfigEntry.Gui.Tooltip
        public boolean fallDamageInterruptsMoves = false;

        @ConfigEntry.Gui.Tooltip
        public boolean projectileDamageInterruptsMoves = true;

        @ConfigEntry.Gui.Tooltip
        public boolean explosionDamageInterruptsMoves = true;

        @ConfigEntry.Gui.Tooltip
        public boolean magicDamageInterruptsMoves = true;

        @ConfigEntry.Gui.Tooltip
        public boolean drowningDamageInterruptsMoves = true;

        @ConfigEntry.Gui.Tooltip
        public boolean starvationDamageInterruptsMoves = true;
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

    @ConfigEntry.Gui.Excluded
    public PerkConfig perks = new PerkConfig();

    public static class PerkConfig {

        @ConfigEntry.Gui.Tooltip
        public boolean enablePerks = true;

        @ConfigEntry.Gui.Tooltip
        public boolean enableFlawSystem = true;

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
            return NichirinServerConfig.get();
        } catch (Exception ignored) {
        }
        try {
            return AutoConfig.getConfigHolder(NichirinModConfig.class).getConfig();
        } catch (Exception e) {
            return new NichirinModConfig();
        }
    }
}
