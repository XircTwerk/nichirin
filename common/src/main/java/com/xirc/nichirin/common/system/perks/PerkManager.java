package com.xirc.nichirin.common.system.perks;

import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side perk effect dispatcher.
 *
 * <p>Perk effects are applied <em>at the callsite</em> (e.g. in damage events,
 * attribute calculation, etc.) by calling the static helper methods here.
 * This class does NOT tick — it is purely a stateless utility layer over
 * {@link PerkData}.</p>
 *
 * <h2>Equip / Unequip</h2>
 * Use {@link #tryEquip} and {@link #tryUnequip} for server-authoritative
 * changes. These validate limits from {@link NichirinModConfig.PerkConfig}
 * and return a {@link Result} with a human-readable failure reason.</p>
 */
public final class PerkManager {


    /** Remaining ticks of second_wind boost after stamina depletion. */
    private static final Map<UUID, Integer> secondWindTicks = new HashMap<>();
    /** Whether stamina was at zero last tick (for second_wind trigger detection). */
    private static final Map<UUID, Boolean> wasStaminaDepleted = new HashMap<>();
    /** Game-time tick when unbreakable last procced (per player). */
    private static final Map<UUID, Long> unbreakableCooldownEnd = new HashMap<>();
    /** Game-time tick when vermilion_soul last revived (per player), for 24000-tick cooldown. */
    private static final Map<UUID, Long> vermilionLastRevive = new HashMap<>();
    /** Current moonlit_fury kill stacks (reset at dawn each in-game day). */
    private static final Map<UUID, Integer> moonlitFuryStacks = new HashMap<>();
    /** Last in-game day when moonlit_fury stacks were reset. */
    private static final Map<UUID, Long> moonlitFuryResetDay = new HashMap<>();


    /**
     * Attempts to equip a discovered perk for the player.
     *
     * <p>Validation checks (all from {@link NichirinModConfig.PerkConfig}):
     * <ul>
     *   <li>Perk system must be enabled globally and per-player.</li>
     *   <li>Perk must be discovered.</li>
     *   <li>Perk must not already be equipped.</li>
     *   <li>Perk ID must not be in the config's disabled list.</li>
     *   <li>Slot count must be below the fixed perk slot cap.</li>
     *   <li>If going beyond the free slots, enough flaws must already be equipped.</li>
     * </ul>
     */
    public static Result tryEquip(ServerPlayer player, String perkId, PerkTier tier) {
        NichirinModConfig.PerkConfig cfg = NichirinModConfig.get().perks;
        if (!cfg.enablePerks) return Result.fail("Perk system is disabled.");

        PerkDefinition def = NichirinPerkRegistry.getPerk(perkId);
        if (def == null) return Result.fail("Unknown perk: " + perkId);
        if (cfg.disabledPerkIds.contains(perkId)) return Result.fail("This perk has been disabled by the server.");

        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!data.isPerksEnabled()) return Result.fail("You have disabled your perks.");
        if (!data.hasDiscovered(perkId)) return Result.fail("You haven't discovered this perk yet.");
        if (data.isEquipped(perkId)) return Result.fail("This perk is already equipped.");
        int effectiveMax = data.getPerkSlots();
        if (data.equippedCount() >= effectiveMax) return Result.fail("No free perk slots (max " + effectiveMax + ").");

        int newCount = data.equippedCount() + 1;
        if (newCount > PerkData.FREE_PERK_SLOTS) {
            int requiredFlaws = newCount - PerkData.FREE_PERK_SLOTS;
            if (data.equippedFlawCount() < requiredFlaws) {
                return Result.fail("You must equip " + requiredFlaws + " flaw(s) before adding another perk.");
            }
        }

        data.equip(perkId, tier);
        return Result.ok();
    }

    public static Result tryUnequip(ServerPlayer player, String perkId) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!data.isEquipped(perkId)) return Result.fail("Perk is not equipped.");
        data.unequip(perkId);
        return Result.ok();
    }


    /**
     * Attempts to upgrade an equipped perk to the next tier.
     * Checks that the player has the required items/XP and deducts them on success.
     */
    public static Result tryUpgrade(ServerPlayer player, String perkId) {
        NichirinModConfig.PerkConfig cfg = NichirinModConfig.get().perks;
        if (!cfg.enablePerks) return Result.fail("Perk system is disabled.");

        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        PerkDefinition def = NichirinPerkRegistry.getPerk(perkId);
        if (def == null) return Result.fail("Unknown perk: " + perkId);

        PerkTier currentTier = data.getTier(perkId);
        if (currentTier == null) return Result.fail("Perk is not equipped.");

        PerkUpgradeCost cost = def.getUpgradeCost(currentTier);
        if (cost == null) return Result.fail("This perk is already at maximum tier.");

        if (currentTier.level >= cfg.maxTier) return Result.fail("Server configuration caps perks at tier " + cfg.maxTier + ".");

        // Check XP
        if (player.experienceLevel < cost.xpLevels) {
            return Result.fail("Not enough XP levels (need " + cost.xpLevels + ", have " + player.experienceLevel + ").");
        }

        // Check items
        for (ItemStack required : cost.requiredItems) {
            int found = countItem(player, required.getItem());
            if (found < required.getCount()) {
                return Result.fail("Missing " + required.getCount() + "x " + required.getHoverName().getString() + ".");
            }
        }

        // Deduct XP
        player.giveExperienceLevels(-cost.xpLevels);

        // Deduct items
        for (ItemStack required : cost.requiredItems) {
            removeItem(player, required.getItem(), required.getCount());
        }

        data.upgradeTier(perkId);
        return Result.ok();
    }


    public static Result tryEquipFlaw(ServerPlayer player, String flawId) {
        NichirinModConfig.PerkConfig cfg = NichirinModConfig.get().perks;
        if (!cfg.enableFlawSystem) return Result.fail("Flaw system is disabled.");

        FlawDefinition flaw = NichirinPerkRegistry.getFlaw(flawId);
        if (flaw == null) return Result.fail("Unknown flaw: " + flawId);

        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (data.hasFlaw(flawId)) return Result.fail("Flaw already equipped.");
        if (data.equippedFlawCount() >= cfg.maxFlaws) return Result.fail("Cannot equip more than " + cfg.maxFlaws + " flaws.");

        data.equipFlaw(flawId);
        return Result.ok();
    }

    public static Result tryUnequipFlaw(ServerPlayer player, String flawId) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        NichirinModConfig.PerkConfig cfg = NichirinModConfig.get().perks;

        int requiredFlaws = Math.max(0, data.equippedCount() - PerkData.FREE_PERK_SLOTS);
        if (data.equippedFlawCount() - 1 < requiredFlaws) {
            return Result.fail("Cannot remove this flaw — you need " + requiredFlaws + " flaw(s) for your current perks.");
        }

        if (!data.hasFlaw(flawId)) return Result.fail("Flaw is not equipped.");
        data.unequipFlaw(flawId);
        cleanupFlawEffects(player, flawId);
        return Result.ok();
    }

    public static void cleanupRemovedFlaws(ServerPlayer player, Set<String> previousFlaws, Set<String> currentFlaws) {
        for (String flawId : previousFlaws) {
            if (!currentFlaws.contains(flawId)) {
                cleanupFlawEffects(player, flawId);
            }
        }
    }

    public static void cleanupFlawEffects(ServerPlayer player, String flawId) {
        switch (flawId) {
            case "cursed_eyes" -> {
                MobEffectInstance blindness = player.getEffect(MobEffects.BLINDNESS);
                if (blindness != null && blindness.getDuration() > 100000) {
                    player.removeEffect(MobEffects.BLINDNESS);
                }
            }
            case "daywalker" -> {
                removeShortAmplifiedEffect(player, MobEffects.WEAKNESS, 1);
                removeShortAmplifiedEffect(player, MobEffects.MOVEMENT_SLOWDOWN, 1);
                removeShortAmplifiedEffect(player, MobEffects.DIG_SLOWDOWN, 1);
            }
            default -> {
            }
        }
    }


    /**
     * Grants discovery of a perk (e.g. from reading a scroll). Safe to call even if already
     * discovered.
     * @return true if newly discovered.
     */
    public static boolean discover(ServerPlayer player, String perkId) {
        PerkDefinition def = NichirinPerkRegistry.getPerk(perkId);
        if (def == null) return false;
        return PlayerDataProvider.getData(player).getPerkData().discover(perkId);
    }


    /**
     * Returns the total outgoing damage multiplier from all equipped perks.
     * Returns 1.0 if perks are disabled or none apply.
     */
    public static float getDamageMultiplier(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return 1.0f;

        float mult = 1.0f;
        for (Map.Entry<String, PerkTier> entry : data.getEquippedPerks().entrySet()) {
            String id = entry.getKey();
            PerkTier tier = entry.getValue();
            mult += switch (id) {
                // breath_focus: stun-focused, small damage bonus as secondary effect
                case "breath_focus"    -> tier == PerkTier.UNCOMMON ? 0.05f : tier == PerkTier.RARE ? 0.10f : tier == PerkTier.EPIC ? 0.18f : 0.28f;
                // glass_cannon: +18/28/40% damage, matches registry description
                case "glass_cannon"    -> tier == PerkTier.RARE ? 0.18f : tier == PerkTier.EPIC ? 0.28f : 0.40f;
                // killer_instinct: applied separately via getKillerInstinctMultiplier
                case "killer_instinct" -> 0f;
                // slayers_resolve: base passive bonus, full scaling handled via getSlayersResolveMultiplier
                case "slayers_resolve" -> 0f;
                default -> 0f;
            };
        }

        // Resonance: if 3+ perks share a tag, amplify all perk bonuses by 15%
        if (data.isEquipped("resonance") && hasResonance(data)) {
            mult = 1.0f + (mult - 1.0f) * 1.15f;
        }

        return mult;
    }

    /**
     * Damage multiplier vs a target that is below the killer_instinct threshold (40% HP).
     */
    public static float getKillerInstinctMultiplier(ServerPlayer player, float targetHealthFraction) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return 1.0f;
        if (!data.isEquipped("killer_instinct")) return 1.0f;
        if (targetHealthFraction > 0.40f) return 1.0f;
        PerkTier tier = data.getTier("killer_instinct");
        return tier == PerkTier.UNCOMMON ? 1.10f : tier == PerkTier.RARE ? 1.18f : tier == PerkTier.EPIC ? 1.28f : 1.40f;
    }

    /**
     * Damage multiplier from Slayer's Resolve — scales with missing HP.
     * 0.5% bonus per 1% HP missing (e.g. at 50% HP → +25%, at 10% HP → +45%).
     */
    public static float getSlayersResolveMultiplier(ServerPlayer player, float currentHealthFraction) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return 1.0f;
        if (!data.isEquipped("slayers_resolve")) return 1.0f;
        float missingFraction = 1.0f - Math.max(0f, Math.min(1f, currentHealthFraction));
        return 1.0f + missingFraction * 0.50f;
    }

    /**
     * Damage multiplier from Adrenaline Rush — scales with missing HP per 10% bracket.
     */
    public static float getAdrenalineRushMultiplier(ServerPlayer player, float currentHealthFraction) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return 1.0f;
        if (!data.isEquipped("adrenaline_rush")) return 1.0f;
        PerkTier tier = data.getTier("adrenaline_rush");
        float missingFraction = 1.0f - Math.max(0f, Math.min(1f, currentHealthFraction));
        float bonusPerTen = tier == PerkTier.UNCOMMON ? 0.015f : tier == PerkTier.RARE ? 0.025f : tier == PerkTier.EPIC ? 0.035f : 0.040f;
        // brackets: floor to nearest 10% (e.g. 67% missing → 6 brackets)
        int brackets = (int) (missingFraction * 10);
        return 1.0f + brackets * bonusPerTen;
    }

    /**
     * Incoming damage multiplier (values > 1 mean taking MORE damage, < 1 mean resistance).
     */
    public static float getIncomingDamageMultiplier(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return 1.0f;

        float mult = 1.0f;
        for (Map.Entry<String, PerkTier> entry : data.getEquippedPerks().entrySet()) {
            String id = entry.getKey();
            PerkTier tier = entry.getValue();
            mult += switch (id) {
                // glass_cannon: takes +18/22/28% more damage
                case "glass_cannon" -> tier == PerkTier.RARE ? 0.18f : tier == PerkTier.EPIC ? 0.22f : 0.28f;
                // iron_will: reduces damage below 35% HP — applied separately via getIronWillMultiplier
                case "iron_will"    -> 0f;
                // juggernaut: +15/25/35% flat resistance
                case "juggernaut"   -> tier == PerkTier.RARE ? -0.15f : tier == PerkTier.EPIC ? -0.25f : -0.35f;
                default -> 0f;
            };
        }
        return Math.max(0.1f, mult);
    }

    /**
     * Incoming damage multiplier when the player is below 35% HP (iron_will).
     * Returns 1.0 if the condition is not met.
     */
    public static float getIronWillMultiplier(ServerPlayer player, float currentHealthFraction) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return 1.0f;
        if (!data.isEquipped("iron_will")) return 1.0f;
        if (currentHealthFraction > 0.35f) return 1.0f;
        PerkTier tier = data.getTier("iron_will");
        return tier == PerkTier.COMMON ? 0.92f : tier == PerkTier.UNCOMMON ? 0.85f : tier == PerkTier.RARE ? 0.76f : tier == PerkTier.EPIC ? 0.65f : 0.52f;
    }

    /**
     * Stamina cost multiplier — values below 1 reduce stamina drain.
     * Covers: enduring_spirit (drain reduction), lightfoot (sprint stamina), stamina_seeker flaw (+cost).
     */
    public static float getStaminaCostMultiplier(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return 1.0f;

        float mult = 1.0f;
        if (data.isEquipped("enduring_spirit")) {
            PerkTier tier = data.getTier("enduring_spirit");
            mult -= tier == PerkTier.COMMON ? 0.08f : tier == PerkTier.UNCOMMON ? 0.16f : tier == PerkTier.RARE ? 0.27f : tier == PerkTier.EPIC ? 0.38f : 0.50f;
        }
        if (data.isEquipped("lightfoot")) {
            PerkTier tier = data.getTier("lightfoot");
            mult -= tier == PerkTier.COMMON ? 0.08f : tier == PerkTier.UNCOMMON ? 0.16f : tier == PerkTier.RARE ? 0.25f : 0.35f;
        }
        if (data.hasFlaw("stamina_seeker")) mult += 0.20f;
        return Math.max(0.05f, mult);
    }

    /**
     * Breath cost multiplier — values below 1 reduce breathing technique cost.
     * Covers: breath_efficiency, zen_mastery (extra reduction below 50% HP).
     * @param currentHealthFraction player's current HP / max HP (0.0–1.0)
     */
    public static float getBreathCostMultiplier(ServerPlayer player, float currentHealthFraction) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return 1.0f;

        float mult = 1.0f;
        if (data.isEquipped("breath_efficiency")) {
            PerkTier tier = data.getTier("breath_efficiency");
            mult -= tier == PerkTier.COMMON ? 0.08f : tier == PerkTier.UNCOMMON ? 0.15f : tier == PerkTier.RARE ? 0.25f : tier == PerkTier.EPIC ? 0.38f : 0.50f;
        }
        if (data.isEquipped("zen_mastery") && currentHealthFraction <= 0.50f) {
            PerkTier tier = data.getTier("zen_mastery");
            mult -= tier == PerkTier.EPIC ? 0.25f : 0.45f;
        }
        return Math.max(0.05f, mult);
    }

    /**
     * Breath regen multiplier for idle recovery.
     * Covers: breath_recovery, zen_mastery LEGENDARY (extra 30% below 50% HP).
     * @param currentHealthFraction player's current HP / max HP (0.0–1.0)
     */
    public static float getBreathRegenMultiplier(ServerPlayer player, float currentHealthFraction) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return 1.0f;

        float mult = 1.0f;
        if (data.isEquipped("breath_recovery")) {
            PerkTier tier = data.getTier("breath_recovery");
            mult += tier == PerkTier.COMMON ? 0.15f : tier == PerkTier.UNCOMMON ? 0.28f : tier == PerkTier.RARE ? 0.45f : tier == PerkTier.EPIC ? 0.65f : 0.85f;
        }
        if (data.isEquipped("zen_mastery") && data.getTier("zen_mastery") == PerkTier.LEGENDARY && currentHealthFraction <= 0.50f) {
            mult += 0.30f;
        }
        return mult;
    }

    /**
     * Probability (0.0–1.0) that breath_overflow procs on a breath technique use,
     * negating the breath cost entirely.
     */
    public static float getBreathOverflowChance(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("breath_overflow")) return 0f;
        PerkTier tier = data.getTier("breath_overflow");
        return tier == PerkTier.EPIC ? 0.10f : 0.18f;
    }

    /**
     * Returns the amount of breath to restore on a breath_overflow LEGENDARY proc (18% of max).
     * Returns 0 for EPIC (no restore bonus, just free cast).
     */
    public static boolean isBreathOverflowLegendary(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return false;
        PerkTier tier = data.getTier("breath_overflow");
        return tier == PerkTier.LEGENDARY;
    }

    /**
     * Stamina regen multiplier.
     * Covers: iron_core (delayed but faster regen), enduring_spirit LEGENDARY (+15%),
     * second_wind (temporary burst after depletion).
     */
    public static float getStaminaRegenMultiplier(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return 1.0f;

        float mult = 1.0f;
        if (data.isEquipped("iron_core")) {
            PerkTier tier = data.getTier("iron_core");
            mult += tier == PerkTier.UNCOMMON ? 0.25f : tier == PerkTier.RARE ? 0.50f : tier == PerkTier.EPIC ? 0.80f : 1.20f;
        }
        if (data.isEquipped("enduring_spirit") && data.getTier("enduring_spirit") == PerkTier.LEGENDARY) {
            mult += 0.15f;
        }
        // second_wind boost while active
        if (isSecondWindActive(player)) {
            PerkTier tier = data.getTier("second_wind");
            mult += tier == PerkTier.UNCOMMON ? 0.20f : tier == PerkTier.RARE ? 0.40f : tier == PerkTier.EPIC ? 0.65f : 0.80f;
        }
        return mult;
    }

    /**
     * Extra regen delay ticks added by iron_core (delays regen start but regen is faster once it kicks in).
     */
    public static int getIronCoreDelayBonus(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("iron_core")) return 0;
        PerkTier tier = data.getTier("iron_core");
        return tier == PerkTier.UNCOMMON ? 10 : tier == PerkTier.RARE ? 20 : tier == PerkTier.EPIC ? 30 : 40;
    }

    /** Returns true if second_wind is currently boosting regen for this player. */
    public static boolean isSecondWindActive(ServerPlayer player) {
        return secondWindTicks.getOrDefault(player.getUUID(), 0) > 0;
    }

    /**
     * Called by StaminaManager when stamina reaches zero; triggers second_wind if equipped.
     * For LEGENDARY, also instantly refills 15% stamina (caller handles the refill).
     * @return the instant refill amount (0 unless LEGENDARY).
     */
    public static float triggerSecondWind(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("second_wind")) return 0f;
        PerkTier tier = data.getTier("second_wind");
        int duration = tier == PerkTier.UNCOMMON ? 100 : tier == PerkTier.RARE ? 140 : tier == PerkTier.EPIC ? 180 : 200;
        secondWindTicks.put(player.getUUID(), duration);
        return tier == PerkTier.LEGENDARY ? 15f : 0f;
    }

    /** Decrements second_wind timer each tick. Call from StaminaManager.tick(). */
    public static void tickSecondWind(ServerPlayer player) {
        UUID id = player.getUUID();
        int ticks = secondWindTicks.getOrDefault(id, 0);
        if (ticks > 0) secondWindTicks.put(id, ticks - 1);
    }

    /** Returns true if stamina was zero last tick (used by StaminaManager to detect depletion). */
    public static boolean checkAndUpdateDepletionState(ServerPlayer player, boolean currentlyDepleted) {
        UUID id = player.getUUID();
        boolean wasDepleted = wasStaminaDepleted.getOrDefault(id, false);
        wasStaminaDepleted.put(id, currentlyDepleted);
        return !wasDepleted && currentlyDepleted; // true = just depleted this tick
    }

    /**
     * Knockback resistance value (0.0–1.0) from steadfast.
     * Stacks additively with natural knockback resistance.
     */
    public static float getSteadfastKnockbackResistance(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("steadfast")) return 0f;
        PerkTier tier = data.getTier("steadfast");
        if (tier == PerkTier.LEGENDARY) return 1.0f;
        return tier == PerkTier.COMMON ? 0.15f : tier == PerkTier.UNCOMMON ? 0.30f : tier == PerkTier.RARE ? 0.50f : 0.70f;
    }

    /** Speed bonus multiplier from night_prowler (only applies at night). Returns 0 if not night. */
    public static float getNightProwlerSpeedBonus(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("night_prowler")) return 0f;
        long dayTime = player.level().getDayTime() % 24000L;
        boolean isNight = dayTime >= 13000L && dayTime <= 23000L;
        if (!isNight) return 0f;
        PerkTier tier = data.getTier("night_prowler");
        return tier == PerkTier.UNCOMMON ? 0.06f : tier == PerkTier.RARE ? 0.12f : 0.20f;
    }

    /** Returns true if night_prowler should grant night vision (RARE+, and currently night). */
    public static boolean hasNightProwlerNightVision(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("night_prowler")) return false;
        PerkTier tier = data.getTier("night_prowler");
        if (tier == null || tier.level < PerkTier.UNCOMMON.level) return false;
        long dayTime = player.level().getDayTime() % 24000L;
        return dayTime >= 13000L && dayTime <= 23000L;
    }

    /** Speed bonus from lightfoot. */
    public static float getLightfootSpeedBonus(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("lightfoot")) return 0f;
        PerkTier tier = data.getTier("lightfoot");
        return tier == PerkTier.COMMON ? 0.04f : tier == PerkTier.UNCOMMON ? 0.08f : tier == PerkTier.RARE ? 0.13f : 0.18f;
    }

    /** Speed penalty from juggernaut (negative value). */
    public static float getJuggernautSpeedPenalty(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("juggernaut")) return 0f;
        PerkTier tier = data.getTier("juggernaut");
        return tier == PerkTier.RARE ? -0.12f : tier == PerkTier.EPIC ? -0.08f : -0.04f;
    }

    /** Detection range (blocks) for hunters_instinct glow effect. 0 if not equipped. */
    public static double getHuntersInstinctRange(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("hunters_instinct")) return 0;
        PerkTier tier = data.getTier("hunters_instinct");
        return tier == PerkTier.COMMON ? 12 : tier == PerkTier.UNCOMMON ? 20 : tier == PerkTier.RARE ? 30 : tier == PerkTier.EPIC ? 48 : 64;
    }

    /**
     * Passive breath regen per tick from breath_efficiency LEGENDARY (0.5 breath/second = 0.025/tick).
     */
    public static float getBreathEfficiencyPassiveRegen(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return 0f;
        PerkTier tier = data.getTier("breath_efficiency");
        return tier == PerkTier.LEGENDARY ? 0.025f : 0f;
    }

    /** Extra i-frame ticks from ghost_step. 0 if not equipped. */
    public static int getGhostStepIFrameBonus(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("ghost_step")) return 0;
        PerkTier tier = data.getTier("ghost_step");
        return tier == PerkTier.COMMON ? 2 : tier == PerkTier.UNCOMMON ? 4 : tier == PerkTier.RARE ? 6 : tier == PerkTier.EPIC ? 8 : 10;
    }


    /**
     * Checks whether unbreakable can absorb a fatal hit and records the cooldown if so.
     * @param gameTime current level game time in ticks
     * @return true if the hit was absorbed (player should be set to 1 HP)
     */
    public static boolean tryConsumeUnbreakable(ServerPlayer player, long gameTime) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("unbreakable")) return false;
        long cooldownEnd = unbreakableCooldownEnd.getOrDefault(player.getUUID(), 0L);
        if (gameTime < cooldownEnd) return false;
        PerkTier tier = data.getTier("unbreakable");
        long cooldown = tier == PerkTier.EPIC ? 7200L : 4800L; // 6 min or 4 min
        unbreakableCooldownEnd.put(player.getUUID(), gameTime + cooldown);
        if (tier == PerkTier.LEGENDARY) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, false, false));
        }
        return true;
    }


    /**
     * Checks whether vermilion_soul can revive this player and records the cooldown.
     * @param gameTime current level game time
     * @return true if revive is allowed (caller should cancel death and restore HP)
     */
    public static boolean tryConsumeVermilionRevive(ServerPlayer player, long gameTime) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("vermilion_soul")) return false;
        long last = vermilionLastRevive.getOrDefault(player.getUUID(), -24000L);
        if (gameTime - last < 24000L) return false;
        vermilionLastRevive.put(player.getUUID(), gameTime);
        PerkTier tier = data.getTier("vermilion_soul");
        float reviveHp = player.getMaxHealth() * (tier == PerkTier.EPIC ? 0.40f : 0.50f);
        player.setHealth(reviveHp);
        if (tier == PerkTier.LEGENDARY) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 1, false, false));
        }
        return true;
    }


    /** Applies bloodthirst effects on kill. Call when the player's hit kills a living entity. */
    public static void applyBloodthirst(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("bloodthirst")) return;
        PerkTier tier = data.getTier("bloodthirst");
        int amplifier = (tier == PerkTier.EPIC || tier == PerkTier.LEGENDARY) ? 1 : 0;
        int duration = tier == PerkTier.RARE ? 60 : tier == PerkTier.EPIC ? 80 : 100;
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, amplifier, false, false));
        if (tier == PerkTier.LEGENDARY) {
            player.heal(2.0f);
        }
    }


    /** Returns the current moonlit_fury damage bonus multiplier (1.0 + stacks * 0.02 or 0.025). */
    public static float getMoonlitFuryDamageBonus(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("moonlit_fury")) return 1.0f;
        int stacks = moonlitFuryStacks.getOrDefault(player.getUUID(), 0);
        if (stacks == 0) return 1.0f;
        PerkTier tier = data.getTier("moonlit_fury");
        float perStack = tier == PerkTier.LEGENDARY ? 0.025f : 0.02f;
        return 1.0f + stacks * perStack;
    }

    /** Called on kill at night to add a moonlit_fury stack. */
    public static void addMoonlitFuryStack(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("moonlit_fury")) return;
        long dayTime = player.level().getDayTime() % 24000L;
        boolean isNight = dayTime >= 13000L && dayTime <= 23000L;
        if (!isNight) return;
        PerkTier tier = data.getTier("moonlit_fury");
        int maxStacks = tier == PerkTier.EPIC ? 12 : 15;
        UUID id = player.getUUID();
        int current = moonlitFuryStacks.getOrDefault(id, 0);
        moonlitFuryStacks.put(id, Math.min(maxStacks, current + 1));
    }

    /** Resets moonlit_fury stacks at dawn. Call every tick from PlayerTickHandler. */
    public static void tickMoonlitFury(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data) || !data.isEquipped("moonlit_fury")) return;
        UUID id = player.getUUID();
        long currentDay = player.level().getDayTime() / 24000L;
        long lastReset = moonlitFuryResetDay.getOrDefault(id, -1L);
        long dayTime = player.level().getDayTime() % 24000L;
        // Reset at dawn (0–1000 ticks into day)
        if (dayTime < 1000 && lastReset != currentDay) {
            moonlitFuryStacks.put(id, 0);
            moonlitFuryResetDay.put(id, currentDay);
        }
    }

    /** Clean up state when a player disconnects. */
    public static void cleanupPlayer(ServerPlayer player) {
        UUID id = player.getUUID();
        secondWindTicks.remove(id);
        wasStaminaDepleted.remove(id);
        unbreakableCooldownEnd.remove(id);
        vermilionLastRevive.remove(id);
        moonlitFuryStacks.remove(id);
        moonlitFuryResetDay.remove(id);
    }

    /**
     * Fall damage multiplier. Featherweight reduces/negates it; brittle_bones doubles it.
     */
    public static float getFallDamageMultiplier(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return 1.0f;

        float mult = 1.0f;
        if (data.isEquipped("featherweight")) {
            PerkTier tier = data.getTier("featherweight");
            if (tier == PerkTier.EPIC) return 0f; // immune
            mult -= tier == PerkTier.COMMON ? 0.20f : tier == PerkTier.UNCOMMON ? 0.40f : 0.65f;
        }
        if (data.hasFlaw("brittle_bones")) mult *= 2.0f;
        return Math.max(0f, mult);
    }


    private static boolean isPerkSystemActive(ServerPlayer player, PerkData data) {
        return NichirinModConfig.get().perks.enablePerks && data.isPerksEnabled();
    }

    /** Returns true if 3 or more equipped perks share at least one PerkTag. */
    private static boolean hasResonance(PerkData data) {
        int[] tagCounts = new int[PerkTag.values().length];
        for (String id : data.getEquippedPerks().keySet()) {
            PerkDefinition def = NichirinPerkRegistry.getPerk(id);
            if (def == null) continue;
            for (PerkTag tag : def.tags) tagCounts[tag.ordinal()]++;
        }
        for (int count : tagCounts) {
            if (count >= 3) return true;
        }
        return false;
    }

    private static int countItem(ServerPlayer player, Item item) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static void removeItem(ServerPlayer player, Item item, int amount) {
        int remaining = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item) && remaining > 0) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    private static void removeShortAmplifiedEffect(ServerPlayer player, MobEffect effect, int minAmplifier) {
        MobEffectInstance instance = player.getEffect(effect);
        if (instance != null && instance.getAmplifier() >= minAmplifier && instance.getDuration() <= 80) {
            player.removeEffect(effect);
        }
    }


    public static final class Result {
        public final boolean success;
        public final String message;

        private Result(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static Result ok() { return new Result(true, null); }
        public static Result fail(String reason) { return new Result(false, reason); }
    }

    private PerkManager() {}
}
