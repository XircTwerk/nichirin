package com.xirc.nichirin.common.system.perks;

import com.xirc.nichirin.common.config.NichirinModConfig;
import com.xirc.nichirin.common.data.PlayerData;
import com.xirc.nichirin.common.data.PlayerDataProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;

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

    // ── Equip / Unequip ───────────────────────────────────────────────────────

    /**
     * Attempts to equip a discovered perk for the player.
     *
     * <p>Validation checks (all from {@link NichirinModConfig.PerkConfig}):
     * <ul>
     *   <li>Perk system must be enabled globally and per-player.</li>
     *   <li>Perk must be discovered.</li>
     *   <li>Perk must not already be equipped.</li>
     *   <li>Perk ID must not be in the config's disabled list.</li>
     *   <li>Slot count must be below {@code maxEquippedPerks}.</li>
     *   <li>If going beyond {@code perksBeforeFlaws} slots, enough flaws must already be equipped.</li>
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
        int effectiveMax = Math.min(data.getPerkSlots(), cfg.maxEquippedPerks);
        if (data.equippedCount() >= effectiveMax) return Result.fail("No free perk slots (max " + effectiveMax + "). Earn more through training.");

        int newCount = data.equippedCount() + 1;
        if (newCount > cfg.perksBeforeFlaws) {
            int requiredFlaws = newCount - cfg.perksBeforeFlaws;
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

    // ── Upgrade ───────────────────────────────────────────────────────────────

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
        for (net.minecraft.world.item.ItemStack required : cost.requiredItems) {
            int found = countItem(player, required.getItem());
            if (found < required.getCount()) {
                return Result.fail("Missing " + required.getCount() + "x " + required.getHoverName().getString() + ".");
            }
        }

        // Deduct XP
        player.giveExperienceLevels(-cost.xpLevels);

        // Deduct items
        for (net.minecraft.world.item.ItemStack required : cost.requiredItems) {
            removeItem(player, required.getItem(), required.getCount());
        }

        data.upgradeTier(perkId);
        return Result.ok();
    }

    // ── Flaw equip / unequip ─────────────────────────────────────────────────

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

        // Cannot unequip if it would leave equipped perks without their required flaws
        int requiredFlaws = Math.max(0, data.equippedCount() - cfg.perksBeforeFlaws);
        if (data.equippedFlawCount() - 1 < requiredFlaws) {
            return Result.fail("Cannot remove this flaw — you need " + requiredFlaws + " flaw(s) for your current perks.");
        }

        if (!data.hasFlaw(flawId)) return Result.fail("Flaw is not equipped.");
        data.unequipFlaw(flawId);
        return Result.ok();
    }

    // ── Discovery ─────────────────────────────────────────────────────────────

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

    // ── Effect queries (called at damage/movement/etc callsites) ──────────────

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
                case "breath_focus"       -> tier == PerkTier.UNCOMMON ? 0.08f : tier == PerkTier.RARE ? 0.15f : tier == PerkTier.EPIC ? 0.25f : 0.40f;
                case "glass_cannon"       -> tier == PerkTier.RARE ? 0.20f : tier == PerkTier.EPIC ? 0.30f : 0.40f;
                case "juggernaut"         -> tier == PerkTier.RARE ? 0.15f : tier == PerkTier.EPIC ? 0.25f : 0.35f;
                case "killer_instinct"    -> 0f; // applied in a separate HP-threshold check
                case "slayers_resolve"    -> 0.10f;
                case "vermilion_soul"     -> tier == PerkTier.EPIC ? 0.10f : 0.20f;
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
     * Damage multiplier vs a target that is below the killer_instinct threshold.
     */
    public static float getKillerInstinctMultiplier(ServerPlayer player, float targetHealthFraction) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return 1.0f;
        if (!data.isEquipped("killer_instinct")) return 1.0f;
        PerkTier tier = data.getTier("killer_instinct");
        float threshold = tier == PerkTier.UNCOMMON ? 0.40f : tier == PerkTier.RARE ? 0.45f : 0.50f;
        if (targetHealthFraction > threshold) return 1.0f;
        return tier == PerkTier.UNCOMMON ? 1.12f : tier == PerkTier.RARE ? 1.20f : tier == PerkTier.EPIC ? 1.30f : 1.40f;
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
                case "glass_cannon"    -> tier == PerkTier.RARE ? 0.15f : tier == PerkTier.EPIC ? 0.20f : 0.25f;  // takes more
                case "iron_will"       -> -(tier == PerkTier.UNCOMMON ? 0.05f : tier == PerkTier.RARE ? 0.10f : tier == PerkTier.EPIC ? 0.15f : 0.20f);
                case "iron_core"       -> -(tier == PerkTier.RARE ? 0.03f : tier == PerkTier.EPIC ? 0.06f : 0.09f);
                case "juggernaut"      -> tier == PerkTier.EPIC ? -0.10f : tier == PerkTier.LEGENDARY ? -0.15f : 0f;
                case "slayers_resolve" -> -0.10f;
                default -> 0f;
            };
        }
        return Math.max(0.1f, mult);
    }

    /**
     * Stamina cost multiplier — values below 1 reduce stamina drain.
     */
    public static float getStaminaCostMultiplier(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return 1.0f;

        float mult = 1.0f;
        if (data.isEquipped("breath_efficiency")) {
            PerkTier tier = data.getTier("breath_efficiency");
            mult -= tier == PerkTier.COMMON ? 0.10f : tier == PerkTier.UNCOMMON ? 0.20f : tier == PerkTier.RARE ? 0.35f : tier == PerkTier.EPIC ? 0.50f : 0.65f;
        }
        // Flaw: stamina_seeker increases cost
        if (data.hasFlaw("stamina_seeker")) mult += 0.20f;
        return Math.max(0.05f, mult);
    }

    /**
     * Fall damage multiplier. Affected by ghost_step and brittle_bones flaw.
     */
    public static float getFallDamageMultiplier(ServerPlayer player) {
        PerkData data = PlayerDataProvider.getData(player).getPerkData();
        if (!isPerkSystemActive(player, data)) return 1.0f;

        float mult = 1.0f;
        if (data.isEquipped("ghost_step")) {
            PerkTier tier = data.getTier("ghost_step");
            if (tier == PerkTier.EPIC || tier == PerkTier.LEGENDARY) return 0f; // immune
            mult -= tier == PerkTier.COMMON ? 0.20f : tier == PerkTier.UNCOMMON ? 0.40f : 0.60f;
        }
        if (data.hasFlaw("brittle_bones")) mult *= 2.0f;
        return Math.max(0f, mult);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    private static int countItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        int total = 0;
        for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    private static void removeItem(ServerPlayer player, net.minecraft.world.item.Item item, int amount) {
        int remaining = amount;
        for (net.minecraft.world.item.ItemStack stack : player.getInventory().items) {
            if (stack.is(item) && remaining > 0) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    // ── Result ────────────────────────────────────────────────────────────────

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
