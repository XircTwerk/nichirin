package com.xirc.nichirin.common.system.perks;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.EnumMap;
import java.util.Map;

/**
 * Immutable definition of a single perk — its metadata, tiers it spans,
 * per-tier descriptions, and upgrade costs.
 *
 * <p>Actual gameplay effects are applied by {@link PerkManager} by inspecting
 * the player's equipped perks at the relevant event callsites.</p>
 */
public class PerkDefinition {

    public final String id;
    public final String name;
    /** Base description, possibly containing {value} placeholders. */
    public final String description;

    /** The lowest tier this perk starts at when first discovered. */
    public final PerkTier minTier;
    /** The highest tier this perk can reach. */
    public final PerkTier maxTier;

    /** Which gameplay categories this perk belongs to. */
    public final PerkTag[] tags;

    /**
     * Whether this is a cursed perk (obtained from cursed scrolls).
     * Cursed perks start at LEGENDARY but automatically assign a flaw.
     */
    public final boolean cursed;

    /**
     * If cursed, the ID of the flaw that gets assigned alongside this perk.
     * Null for normal perks.
     */
    public final String linkedFlawId;

    /** How to obtain/unlock this perk — shown in the GUI. */
    public final String unlockHint;

    /** Vague in-world location hint — shown in the GUI. */
    public final String locationHint;

    /** Item used as the icon in the perks grid. Defaults to a book if not set. */
    public final Item iconItem;

    /** Short description for each available tier. */
    private final Map<PerkTier, String> tierDescriptions;

    /** Upgrade costs keyed by the TARGET tier (i.e. cost to upgrade TO that tier). */
    private final Map<PerkTier, PerkUpgradeCost> upgradeCosts;

    private PerkDefinition(Builder b) {
        this.id               = b.id;
        this.name             = b.name;
        this.description      = b.description;
        this.minTier          = b.minTier;
        this.maxTier          = b.maxTier;
        this.tags             = b.tags;
        this.cursed           = b.cursed;
        this.linkedFlawId     = b.linkedFlawId;
        this.unlockHint       = b.unlockHint;
        this.locationHint     = b.locationHint;
        this.iconItem         = b.iconItem;
        this.tierDescriptions = Map.copyOf(b.tierDescriptions);
        this.upgradeCosts     = Map.copyOf(b.upgradeCosts);
    }

    public boolean hasTier(PerkTier tier) {
        return tier.level >= minTier.level && tier.level <= maxTier.level;
    }

    /** Description for a specific tier, falling back to base description. */
    public String getDescriptionForTier(PerkTier tier) {
        return tierDescriptions.getOrDefault(tier, description);
    }

    /**
     * Upgrade cost to advance FROM {@code current} to {@code current.next()}.
     * Returns null if this perk cannot be upgraded further.
     */
    public PerkUpgradeCost getUpgradeCost(PerkTier currentTier) {
        PerkTier next = currentTier.next();
        if (next == null || !hasTier(next)) return null;
        return upgradeCosts.get(next);
    }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    public static class Builder {
        private final String id;
        private final String name;
        private String description = "";
        private PerkTier minTier = PerkTier.COMMON;
        private PerkTier maxTier = PerkTier.LEGENDARY;
        private PerkTag[] tags = {};
        private boolean cursed = false;
        private String linkedFlawId = null;
        private String unlockHint = "Discovered through a perk scroll or special advancement.";
        private String locationHint = "Location unknown.";
        private Item iconItem = Items.BOOK;
        private final Map<PerkTier, String> tierDescriptions = new EnumMap<>(PerkTier.class);
        private final Map<PerkTier, PerkUpgradeCost> upgradeCosts = new EnumMap<>(PerkTier.class);

        Builder(String id, String name) { this.id = id; this.name = name; }

        public Builder description(String d) { this.description = d; return this; }
        public Builder tiers(PerkTier min, PerkTier max) { this.minTier = min; this.maxTier = max; return this; }
        public Builder tags(PerkTag... tags) { this.tags = tags; return this; }
        public Builder cursed(String linkedFlawId) { this.cursed = true; this.linkedFlawId = linkedFlawId; return this; }
        public Builder unlockHint(String hint) { this.unlockHint = hint; return this; }
        public Builder locationHint(String hint) { this.locationHint = hint; return this; }
        public Builder icon(Item item) { this.iconItem = item; return this; }

        public Builder tierDesc(PerkTier tier, String desc) {
            tierDescriptions.put(tier, desc);
            return this;
        }

        public Builder upgradeCost(PerkTier targetTier, PerkUpgradeCost cost) {
            upgradeCosts.put(targetTier, cost);
            return this;
        }

        public PerkDefinition build() { return new PerkDefinition(this); }
    }
}
