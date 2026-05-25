package com.xirc.nichirin.common.system.perks;

import com.xirc.nichirin.common.config.NichirinModConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.*;

/**
 * Per-player perk state.
 *
 * <ul>
 *   <li>{@link #discovered} — set of perk IDs the player has unlocked (via scrolls/advancements).</li>
 *   <li>{@link #equippedPerks} — ordered map of equipped perk ID → current tier (max 5 slots).</li>
 *   <li>{@link #equippedFlaws} — set of equipped flaw IDs (required when equipping beyond {@code perksBeforeFlaws}).</li>
 *   <li>{@link #perksEnabled} — master toggle for the entire perk system per player.</li>
 *   <li>{@link #presets} — saved loadout presets.</li>
 * </ul>
 *
 * <p>Serialisation/deserialisation is handled via {@link #save()} and {@link #load(CompoundTag)}.</p>
 */
public class PerkData {


    /** All perk IDs the player has discovered (not necessarily equipped). */
    private final Set<String> discovered = new LinkedHashSet<>();

    /**
     * Equipped perks in slot order. LinkedHashMap preserves insertion order (= slot order).
     * Value is the perk's current tier for this player.
     */
    private final Map<String, PerkTier> equippedPerks = new LinkedHashMap<>();

    /** Equipped flaw IDs. */
    private final Set<String> equippedFlaws = new LinkedHashSet<>();

    /** Whether this player has the perk system enabled. */
    private boolean perksEnabled = true;

    /** How many perk slots this player has unlocked (starts at 5). */
    private int perkSlots = 5;

    /** Named preset loadouts. */
    private final List<PerkPreset> presets = new ArrayList<>();


    public boolean hasDiscovered(String perkId) {
        return discovered.contains(perkId);
    }

    /**
     * Adds a perk to the discovered set.
     * @return true if it was newly added (false if already known).
     */
    public boolean discover(String perkId) {
        return discovered.add(perkId);
    }

    public Set<String> getDiscoveredIds() {
        return Collections.unmodifiableSet(discovered);
    }


    public boolean isEquipped(String perkId) {
        return equippedPerks.containsKey(perkId);
    }

    public int equippedCount() {
        return equippedPerks.size();
    }

    public PerkTier getTier(String perkId) {
        return equippedPerks.getOrDefault(perkId, null);
    }

    /**
     * Equips a discovered perk at the given tier.
     * Caller is responsible for enforcing slot limits and flaw requirements.
     */
    public void equip(String perkId, PerkTier tier) {
        equippedPerks.put(perkId, tier);
    }

    public void unequip(String perkId) {
        equippedPerks.remove(perkId);
    }

    /**
     * Upgrades an equipped perk to the next tier.
     * @return the new tier, or null if already at max or not equipped.
     */
    public PerkTier upgradeTier(String perkId) {
        PerkTier current = equippedPerks.get(perkId);
        if (current == null) return null;
        PerkTier next = current.next();
        if (next == null) return null;
        equippedPerks.put(perkId, next);
        return next;
    }

    /** Snapshot of equipped perks (ID → tier), unmodifiable. */
    public Map<String, PerkTier> getEquippedPerks() {
        return Collections.unmodifiableMap(equippedPerks);
    }


    public boolean hasFlaw(String flawId) {
        return equippedFlaws.contains(flawId);
    }

    public void equipFlaw(String flawId) {
        equippedFlaws.add(flawId);
    }

    public void unequipFlaw(String flawId) {
        equippedFlaws.remove(flawId);
    }

    public int equippedFlawCount() {
        return equippedFlaws.size();
    }

    public Set<String> getEquippedFlaws() {
        return Collections.unmodifiableSet(equippedFlaws);
    }


    public boolean isPerksEnabled() {
        return perksEnabled;
    }

    public void setPerksEnabled(boolean enabled) {
        this.perksEnabled = enabled;
    }

    public int getPerkSlots() {
        return perkSlots;
    }

    /** Increases perk slots up to the global config maximum. */
    public void setPerkSlots(int slots) {
        int cap = NichirinModConfig.get().perks.maxEquippedPerks;
        this.perkSlots = Math.max(1, Math.min(slots, cap));
    }

    /** Grants one additional perk slot if below the cap. Returns true if a slot was added. */
    public boolean unlockPerkSlot() {
        int cap = NichirinModConfig.get().perks.maxEquippedPerks;
        if (perkSlots >= cap) return false;
        perkSlots++;
        return true;
    }


    public List<PerkPreset> getPresets() {
        return Collections.unmodifiableList(presets);
    }

    public void savePreset(PerkPreset preset) {
        // Replace existing preset with same name
        presets.removeIf(p -> p.name.equals(preset.name));
        presets.add(preset);
    }

    public void deletePreset(String name) {
        presets.removeIf(p -> p.name.equals(name));
    }

    /**
     * Builds a preset snapshot of the current loadout with the given name.
     * Does NOT save it automatically — call {@link #savePreset(PerkPreset)} to persist.
     */
    public PerkPreset snapshotAsPreset(String name) {
        PerkPreset preset = new PerkPreset(name);
        for (Map.Entry<String, PerkTier> entry : equippedPerks.entrySet()) {
            preset.perkIds.add(entry.getKey());
            preset.perkTiers.add(entry.getValue());
        }
        preset.flawIds.addAll(equippedFlaws);
        return preset;
    }

    /**
     * Applies a preset's loadout, replacing the current equipped state.
     * Only perks that have been discovered are applied.
     * Caller must validate flaw requirements externally.
     */
    public void applyPreset(PerkPreset preset) {
        equippedPerks.clear();
        equippedFlaws.clear();
        for (int i = 0; i < preset.perkIds.size(); i++) {
            String id = preset.perkIds.get(i);
            if (discovered.contains(id)) {
                equippedPerks.put(id, preset.perkTiers.get(i));
            }
        }
        for (String flawId : preset.flawIds) {
            equippedFlaws.add(flawId);
        }
    }


    public void copyFrom(PerkData other) {
        this.discovered.clear();
        this.discovered.addAll(other.discovered);
        this.equippedPerks.clear();
        this.equippedPerks.putAll(other.equippedPerks);
        this.equippedFlaws.clear();
        this.equippedFlaws.addAll(other.equippedFlaws);
        this.perksEnabled = other.perksEnabled;
        this.perkSlots = other.perkSlots;
        this.presets.clear();
        for (PerkPreset p : other.presets) {
            this.presets.add(PerkPreset.load(p.save()));
        }
    }


    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean("PerksEnabled", perksEnabled);
        tag.putInt("PerkSlots", perkSlots);

        ListTag discoveredTag = new ListTag();
        for (String id : discovered) discoveredTag.add(StringTag.valueOf(id));
        tag.put("Discovered", discoveredTag);

        ListTag equippedTag = new ListTag();
        for (Map.Entry<String, PerkTier> e : equippedPerks.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", e.getKey());
            entry.putInt("Tier", e.getValue().level);
            equippedTag.add(entry);
        }
        tag.put("Equipped", equippedTag);

        ListTag flawsTag = new ListTag();
        for (String fid : equippedFlaws) flawsTag.add(StringTag.valueOf(fid));
        tag.put("Flaws", flawsTag);

        ListTag presetsTag = new ListTag();
        for (PerkPreset p : presets) presetsTag.add(p.save());
        tag.put("Presets", presetsTag);

        return tag;
    }

    public void load(CompoundTag tag) {
        perksEnabled = !tag.contains("PerksEnabled") || tag.getBoolean("PerksEnabled");
        perkSlots = tag.contains("PerkSlots") ? Math.max(5, tag.getInt("PerkSlots")) : 5;

        discovered.clear();
        ListTag discoveredTag = tag.getList("Discovered", Tag.TAG_STRING);
        for (int i = 0; i < discoveredTag.size(); i++) discovered.add(discoveredTag.getString(i));

        equippedPerks.clear();
        ListTag equippedTag = tag.getList("Equipped", Tag.TAG_COMPOUND);
        for (int i = 0; i < equippedTag.size(); i++) {
            CompoundTag entry = equippedTag.getCompound(i);
            equippedPerks.put(entry.getString("Id"), PerkTier.fromLevel(entry.getInt("Tier")));
        }

        equippedFlaws.clear();
        ListTag flawsTag = tag.getList("Flaws", Tag.TAG_STRING);
        for (int i = 0; i < flawsTag.size(); i++) equippedFlaws.add(flawsTag.getString(i));

        presets.clear();
        ListTag presetsTag = tag.getList("Presets", Tag.TAG_COMPOUND);
        for (int i = 0; i < presetsTag.size(); i++) presets.add(PerkPreset.load(presetsTag.getCompound(i)));
    }
}
