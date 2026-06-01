package com.xirc.nichirin.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xirc.nichirin.BreathOfNichirin;
import me.shedaniel.cloth.clothconfig.shadowed.com.moandjiezana.toml.Toml;
import me.shedaniel.autoconfig.AutoConfig;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class NichirinServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Paths.get("config", "nichirin-server.toml");
    private static final Path LEGACY_SERVER_JSON_PATH = Paths.get("config", "nichirin-server.json");
    private static final Path LEGACY_MOD_JSON_PATH = Paths.get("config", "nichirin.json");

    private static NichirinModConfig config;

    private NichirinServerConfig() {
    }

    public static void load() {
        try {
            config = loadConfig();
        } catch (Exception e) {
            BreathOfNichirin.LOGGER.warn("Failed to load {}, using defaults.", PATH, e);
            config = new NichirinModConfig();
        }
    }

    public static NichirinModConfig loadConfig() {
        try {
            Files.createDirectories(PATH.getParent());
            NichirinModConfig loaded = null;
            if (Files.exists(PATH)) {
                loaded = readToml(PATH);
            } else if (Files.exists(LEGACY_MOD_JSON_PATH)) {
                loaded = readJson(LEGACY_MOD_JSON_PATH);
                if (loaded != null) {
                    config = loaded;
                    save(config);
                    Files.deleteIfExists(LEGACY_MOD_JSON_PATH);
                    Files.deleteIfExists(LEGACY_SERVER_JSON_PATH);
                    BreathOfNichirin.LOGGER.info("Migrated legacy config {} to {}.", LEGACY_MOD_JSON_PATH, PATH);
                }
            } else if (Files.exists(LEGACY_SERVER_JSON_PATH)) {
                loaded = readJson(LEGACY_SERVER_JSON_PATH);
                if (loaded != null) {
                    config = loaded;
                    save(config);
                    Files.deleteIfExists(LEGACY_SERVER_JSON_PATH);
                    BreathOfNichirin.LOGGER.info("Migrated legacy config {} to {}.", LEGACY_SERVER_JSON_PATH, PATH);
                }
            }
            if (loaded == null) {
                loaded = new NichirinModConfig();
                config = loaded;
                save(config);
            }
            config = loaded;
        } catch (Exception e) {
            BreathOfNichirin.LOGGER.warn("Failed to load {}, using defaults.", PATH, e);
            config = new NichirinModConfig();
        }
        return config;
    }

    public static NichirinModConfig get() {
        try {
            config = AutoConfig.getConfigHolder(NichirinModConfig.class).getConfig();
            return config;
        } catch (Exception ignored) {
        }
        if (config == null) {
            load();
        }
        return config;
    }

    public static void save() {
        try {
            config = AutoConfig.getConfigHolder(NichirinModConfig.class).getConfig();
        } catch (Exception ignored) {
        }
        save(config);
    }

    public static void save(NichirinModConfig configToSave) {
        config = configToSave;
        if (config == null) return;
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                writer.write(toCommentedToml(config));
            }
        } catch (IOException e) {
            BreathOfNichirin.LOGGER.warn("Failed to save {}.", PATH, e);
        }
    }

    private static NichirinModConfig readToml(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return new Toml().read(reader).to(NichirinModConfig.class);
        }
    }

    private static NichirinModConfig readJson(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, NichirinModConfig.class);
        }
    }

    private static String toCommentedToml(NichirinModConfig cfg) {
        NichirinModConfig defaults = new NichirinModConfig();
        StringBuilder out = new StringBuilder(4096);
        out.append("# Breath of Nichirin server config\n");
        out.append("# Change the value before each inline comment. The comment shows the default.\n\n");
        appendCombat(out, cfg.combat, defaults.combat);
        out.append('\n');
        appendMovement(out, cfg.movement, defaults.movement);
        out.append('\n');
        appendBlocking(out, cfg.blocking, defaults.blocking);
        out.append('\n');
        appendStamina(out, cfg.stamina, defaults.stamina);
        out.append('\n');
        appendBreathing(out, cfg.breathing, defaults.breathing);
        out.append('\n');
        appendDemon(out, cfg.demon, defaults.demon);
        out.append('\n');
        appendMoveInterrupts(out, cfg.moveInterrupts, defaults.moveInterrupts);
        out.append('\n');
        appendKillRewards(out, cfg.killRewards, defaults.killRewards);
        out.append('\n');
        appendBloodMoon(out, cfg.bloodMoon, defaults.bloodMoon);
        out.append('\n');
        appendPerks(out, cfg.perks, defaults.perks);
        return out.toString();
    }

    private static void appendCombat(StringBuilder out, NichirinModConfig.CombatConfig cfg, NichirinModConfig.CombatConfig defaults) {
        out.append("[combat]\n");
        appendValue(out, 4, "comboWindowTicks", cfg.comboWindowTicks, defaults.comboWindowTicks, true);
        appendValue(out, 4, "parryWindowTicks", cfg.parryWindowTicks, defaults.parryWindowTicks, true);
        appendValue(out, 4, "enableParrySystem", cfg.enableParrySystem, defaults.enableParrySystem, true);
        appendValue(out, 4, "npcAiLevel", cfg.npcAiLevel, defaults.npcAiLevel, true);
        appendValue(out, 4, "npcDashCooldownTicks", cfg.npcDashCooldownTicks, defaults.npcDashCooldownTicks, true);
        appendValue(out, 4, "npcMobilityCooldownTicks", cfg.npcMobilityCooldownTicks, defaults.npcMobilityCooldownTicks, true);
        appendValue(out, 4, "npcDuelHealth", cfg.npcDuelHealth, defaults.npcDuelHealth, true);
        appendValue(out, 4, "npcDuelCooldownSeconds", cfg.npcDuelCooldownSeconds, defaults.npcDuelCooldownSeconds, true);
        appendValue(out, 4, "npcPeacefulHealth", cfg.npcPeacefulHealth, defaults.npcPeacefulHealth, true);
        appendValue(out, 4, "npcDuelWinHpThreshold", cfg.npcDuelWinHpThreshold, defaults.npcDuelWinHpThreshold, true);
        appendValue(out, 4, "npcPlayerDuelMinHealth", cfg.npcPlayerDuelMinHealth, defaults.npcPlayerDuelMinHealth, true);
        appendValue(out, 4, "npcSelfDefenseGraceTicks", cfg.npcSelfDefenseGraceTicks, defaults.npcSelfDefenseGraceTicks, true);
        appendValue(out, 4, "staminaRegenRate", cfg.staminaRegenRate, defaults.staminaRegenRate, false);
    }

    private static void appendMovement(StringBuilder out, NichirinModConfig.MovementConfig cfg, NichirinModConfig.MovementConfig defaults) {
        out.append("[movement]\n");
        appendValue(out, 4, "dashForce", cfg.dashForce, defaults.dashForce, true);
        appendValue(out, 4, "doubleJumpVelocity", cfg.doubleJumpVelocity, defaults.doubleJumpVelocity, true);
        appendValue(out, 4, "backstepDistance", cfg.backstepDistance, defaults.backstepDistance, true);
        appendValue(out, 4, "airDodgeDistance", cfg.airDodgeDistance, defaults.airDodgeDistance, true);
        appendValue(out, 4, "sprintMultiplier", cfg.sprintMultiplier, defaults.sprintMultiplier, false);
    }

    private static void appendBlocking(StringBuilder out, NichirinModConfig.BlockingConfig cfg, NichirinModConfig.BlockingConfig defaults) {
        out.append("[blocking]\n");
        appendValue(out, 4, "blockStanceDrain", cfg.blockStanceDrain, defaults.blockStanceDrain, true);
        appendValue(out, 4, "parryStanceCost", cfg.parryStanceCost, defaults.parryStanceCost, true);
        appendValue(out, 4, "blockCooldownTicks", cfg.blockCooldownTicks, defaults.blockCooldownTicks, true);
        appendValue(out, 4, "earlyReleaseStunTicks", cfg.earlyReleaseStunTicks, defaults.earlyReleaseStunTicks, true);
        appendValue(out, 4, "backstabAngle", cfg.backstabAngle, defaults.backstabAngle, false);
    }

    private static void appendStamina(StringBuilder out, NichirinModConfig.StaminaConfig cfg, NichirinModConfig.StaminaConfig defaults) {
        out.append("[stamina]\n");
        appendValue(out, 4, "staminaRegenRate", cfg.staminaRegenRate, defaults.staminaRegenRate, true);
        appendValue(out, 4, "lightAttackStaminaCost", cfg.lightAttackStaminaCost, defaults.lightAttackStaminaCost, true);
        appendValue(out, 4, "heavyAttackStaminaCost", cfg.heavyAttackStaminaCost, defaults.heavyAttackStaminaCost, true);
        appendValue(out, 4, "unlimitedStamina", cfg.unlimitedStamina, defaults.unlimitedStamina, false);
    }

    private static void appendBreathing(StringBuilder out, NichirinModConfig.BreathingConfig cfg, NichirinModConfig.BreathingConfig defaults) {
        out.append("[breathing]\n");
        appendValue(out, 4, "infiniteBreath", cfg.infiniteBreath, defaults.infiniteBreath, true);
        appendValue(out, 4, "freeBreathMoves", cfg.freeBreathMoves, defaults.freeBreathMoves, true);
        appendValue(out, 4, "maxBreath", cfg.maxBreath, defaults.maxBreath, true);
        appendValue(out, 4, "breathRegenRate", cfg.breathRegenRate, defaults.breathRegenRate, true);
        appendValue(out, 4, "lightAttackStaminaCost", cfg.lightAttackStaminaCost, defaults.lightAttackStaminaCost, true);
        appendValue(out, 4, "heavyAttackStaminaCost", cfg.heavyAttackStaminaCost, defaults.heavyAttackStaminaCost, true);
        appendValue(out, 4, "unlimitedStamina", cfg.unlimitedStamina, defaults.unlimitedStamina, false);
    }

    private static void appendDemon(StringBuilder out, NichirinModConfig.DemonConfig cfg, NichirinModConfig.DemonConfig defaults) {
        out.append("[demon]\n");
        appendValue(out, 4, "bloodPointsOnKill", cfg.bloodPointsOnKill, defaults.bloodPointsOnKill, true);
        appendValue(out, 4, "maxBloodPoints", cfg.maxBloodPoints, defaults.maxBloodPoints, true);
        appendValue(out, 4, "burnInSunlight", cfg.burnInSunlight, defaults.burnInSunlight, true);
        appendValue(out, 4, "sunDamagePerSecond", cfg.sunDamagePerSecond, defaults.sunDamagePerSecond, true);
        appendValue(out, 4, "bloodDrainEnabled", cfg.bloodDrainEnabled, defaults.bloodDrainEnabled, true);
        appendValue(out, 4, "bloodDrainIntervalSeconds", cfg.bloodDrainIntervalSeconds, defaults.bloodDrainIntervalSeconds, true);
        appendValue(out, 4, "peacefulModeMaxBlood", cfg.peacefulModeMaxBlood, defaults.peacefulModeMaxBlood, true);
        appendValue(out, 4, "demonMaxBreathPercent", cfg.demonMaxBreathPercent, defaults.demonMaxBreathPercent, false);
    }

    private static void appendMoveInterrupts(StringBuilder out, NichirinModConfig.MoveInterruptConfig cfg, NichirinModConfig.MoveInterruptConfig defaults) {
        out.append("[moveInterrupts]\n");
        appendValue(out, 4, "fireDamageInterruptsMoves", cfg.fireDamageInterruptsMoves, defaults.fireDamageInterruptsMoves, true);
        appendValue(out, 4, "fallDamageInterruptsMoves", cfg.fallDamageInterruptsMoves, defaults.fallDamageInterruptsMoves, true);
        appendValue(out, 4, "projectileDamageInterruptsMoves", cfg.projectileDamageInterruptsMoves, defaults.projectileDamageInterruptsMoves, true);
        appendValue(out, 4, "explosionDamageInterruptsMoves", cfg.explosionDamageInterruptsMoves, defaults.explosionDamageInterruptsMoves, true);
        appendValue(out, 4, "magicDamageInterruptsMoves", cfg.magicDamageInterruptsMoves, defaults.magicDamageInterruptsMoves, true);
        appendValue(out, 4, "drowningDamageInterruptsMoves", cfg.drowningDamageInterruptsMoves, defaults.drowningDamageInterruptsMoves, true);
        appendValue(out, 4, "starvationDamageInterruptsMoves", cfg.starvationDamageInterruptsMoves, defaults.starvationDamageInterruptsMoves, false);
    }

    private static void appendKillRewards(StringBuilder out, NichirinModConfig.KillRewardsConfig cfg, NichirinModConfig.KillRewardsConfig defaults) {
        out.append("[killRewards]\n");
        appendValue(out, 4, "healOnKillPlayer", cfg.healOnKillPlayer, defaults.healOnKillPlayer, true);
        appendValue(out, 4, "healOnKillPlayerAsMaxHealth", cfg.healOnKillPlayerAsMaxHealth, defaults.healOnKillPlayerAsMaxHealth, true);
        appendValue(out, 4, "healOnKillPlayerAmount", cfg.healOnKillPlayerAmount, defaults.healOnKillPlayerAmount, true);
        appendValue(out, 4, "healOnKillMob", cfg.healOnKillMob, defaults.healOnKillMob, true);
        appendValue(out, 4, "healOnKillMobAmount", cfg.healOnKillMobAmount, defaults.healOnKillMobAmount, true);
        appendValue(out, 4, "resetCooldownsOnKill", cfg.resetCooldownsOnKill, defaults.resetCooldownsOnKill, true);
        appendValue(out, 4, "resetStaminaOnKill", cfg.resetStaminaOnKill, defaults.resetStaminaOnKill, true);
        appendValue(out, 4, "resetBreathOnKill", cfg.resetBreathOnKill, defaults.resetBreathOnKill, true);
        appendValue(out, 4, "resetStanceOnKill", cfg.resetStanceOnKill, defaults.resetStanceOnKill, false);
    }

    private static void appendBloodMoon(StringBuilder out, NichirinModConfig.BloodMoonConfig cfg, NichirinModConfig.BloodMoonConfig defaults) {
        out.append("[bloodMoon]\n");
        appendValue(out, 4, "enabled", cfg.enabled, defaults.enabled, true);
        appendValue(out, 4, "chancePerNight", cfg.chancePerNight, defaults.chancePerNight, true);
        appendValue(out, 4, "demonAttackBoostPercent", cfg.demonAttackBoostPercent, defaults.demonAttackBoostPercent, true);
        appendValue(out, 4, "demonSpeedBoostPercent", cfg.demonSpeedBoostPercent, defaults.demonSpeedBoostPercent, true);
        appendValue(out, 4, "demonSpawnBoostPercent", cfg.demonSpawnBoostPercent, defaults.demonSpawnBoostPercent, false);
    }

    private static void appendPerks(StringBuilder out, NichirinModConfig.PerkConfig cfg, NichirinModConfig.PerkConfig defaults) {
        out.append("[perks]\n");
        appendValue(out, 4, "enablePerks", cfg.enablePerks, defaults.enablePerks, true);
        appendValue(out, 4, "enableFlawSystem", cfg.enableFlawSystem, defaults.enableFlawSystem, true);
        appendValue(out, 4, "maxFlaws", cfg.maxFlaws, defaults.maxFlaws, true);
        appendValue(out, 4, "maxTier", cfg.maxTier, defaults.maxTier, true);
        appendList(out, 4, "disabledPerkIds", cfg.disabledPerkIds, defaults.disabledPerkIds, false);
    }

    private static void appendValue(StringBuilder out, int spaces, String key, int value, int defaultValue, boolean comma) {
        appendLine(out, spaces, key, String.valueOf(value), String.valueOf(defaultValue), comma);
    }

    private static void appendValue(StringBuilder out, int spaces, String key, boolean value, boolean defaultValue, boolean comma) {
        appendLine(out, spaces, key, String.valueOf(value), String.valueOf(defaultValue), comma);
    }

    private static void appendValue(StringBuilder out, int spaces, String key, double value, double defaultValue, boolean comma) {
        appendLine(out, spaces, key, String.valueOf(value), String.valueOf(defaultValue), comma);
    }

    private static void appendList(StringBuilder out, int spaces, String key, List<String> values, List<String> defaults, boolean comma) {
        appendLine(out, spaces, key, GSON.toJson(values), GSON.toJson(defaults), comma);
    }

    private static void appendLine(StringBuilder out, int spaces, String key, String value, String defaultValue, boolean comma) {
        out.append(key).append(" = ").append(value).append(" # Default: ").append(defaultValue).append('\n');
    }
}
