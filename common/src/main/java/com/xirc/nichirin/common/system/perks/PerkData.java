package com.xirc.nichirin.common.system.perks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Per-player perk state.
 *
 * <ul>
 *   <li>{@link #discovered} — set of perk IDs the player has unlocked (via scrolls/advancements).</li>
 *   <li>{@link #equippedPerks} — ordered map of equipped perk ID → current tier (max 5 slots).</li>
 *   <li>{@link #equippedFlaws} — set of equipped flaw IDs used to unlock perk slots past the free slots.</li>
 *   <li>{@link #perksEnabled} — master toggle for the entire perk system per player.</li>
 *   <li>{@link #presets} — saved loadout presets.</li>
 * </ul>
 *
 * <p>Serialisation/deserialisation is handled via {@link #save()} and {@link #load(CompoundTag)}.</p>
 */
public class PerkData {

    public static final int MAX_PERK_SLOTS = 5;
    public static final int FREE_PERK_SLOTS = 2;

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

    private int perkSlots = MAX_PERK_SLOTS;

    /** Named preset loadouts. */
    private final List<PerkPreset> presets = new ArrayList<>();


    public boolean hasDiscovered(String perkId) {
        if (PerkArchive.ARCHIVED) return false;
        return discovered.contains(perkId);
    }

    /**
     * Adds a perk to the discovered set.
     * @return true if it was newly added (false if already known).
     */
    public boolean discover(String perkId) {
        if (PerkArchive.ARCHIVED) return false;
        return discovered.add(perkId);
    }

    public Set<String> getDiscoveredIds() {
        if (PerkArchive.ARCHIVED) return Collections.emptySet();
        return Collections.unmodifiableSet(discovered);
    }


    public boolean isEquipped(String perkId) {
        if (PerkArchive.ARCHIVED) return false;
        return equippedPerks.containsKey(perkId);
    }

    public int equippedCount() {
        if (PerkArchive.ARCHIVED) return 0;
        return equippedPerks.size();
    }

    public PerkTier getTier(String perkId) {
        if (PerkArchive.ARCHIVED) return null;
        return equippedPerks.getOrDefault(perkId, null);
    }

    /**
     * Equips a discovered perk at the given tier.
     * Caller is responsible for enforcing slot limits and flaw requirements.
     */
    public void equip(String perkId, PerkTier tier) {
        if (PerkArchive.ARCHIVED) return;
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
        if (PerkArchive.ARCHIVED) return null;
        PerkTier current = equippedPerks.get(perkId);
        if (current == null) return null;
        PerkTier next = current.next();
        if (next == null) return null;
        equippedPerks.put(perkId, next);
        return next;
    }

    /** Snapshot of equipped perks (ID → tier), unmodifiable. */
    public Map<String, PerkTier> getEquippedPerks() {
        if (PerkArchive.ARCHIVED) return Collections.emptyMap();
        return Collections.unmodifiableMap(equippedPerks);
    }


    public boolean hasFlaw(String flawId) {
        if (PerkArchive.ARCHIVED) return false;
        return equippedFlaws.contains(flawId);
    }

    public void equipFlaw(String flawId) {
        if (PerkArchive.ARCHIVED) return;
        equippedFlaws.add(flawId);
    }

    public void unequipFlaw(String flawId) {
        equippedFlaws.remove(flawId);
    }

    public int equippedFlawCount() {
        if (PerkArchive.ARCHIVED) return 0;
        return equippedFlaws.size();
    }

    public Set<String> getEquippedFlaws() {
        if (PerkArchive.ARCHIVED) return Collections.emptySet();
        return Collections.unmodifiableSet(equippedFlaws);
    }


    public boolean isPerksEnabled() {
        if (PerkArchive.ARCHIVED) return false;
        return perksEnabled;
    }

    public void setPerksEnabled(boolean enabled) {
        if (PerkArchive.ARCHIVED) return;
        this.perksEnabled = enabled;
    }

    public int getPerkSlots() {
        if (PerkArchive.ARCHIVED) return 0;
        return MAX_PERK_SLOTS;
    }

    public void setPerkSlots(int slots) {
        this.perkSlots = MAX_PERK_SLOTS;
    }

    public boolean unlockPerkSlot() {
        perkSlots = MAX_PERK_SLOTS;
        return false;
    }


    public List<PerkPreset> getPresets() {
        if (PerkArchive.ARCHIVED) return Collections.emptyList();
        return Collections.unmodifiableList(presets);
    }

    public void savePreset(PerkPreset preset) {
        if (PerkArchive.ARCHIVED) return;
        for (int i = 0; i < presets.size(); i++) {
            if (presets.get(i).name.equals(preset.name)) {
                presets.set(i, preset);
                sortPresets();
                return;
            }
        }
        presets.add(preset);
        sortPresets();
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
        if (PerkArchive.ARCHIVED) return;
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
        this.perkSlots = MAX_PERK_SLOTS;
        this.presets.clear();
        for (PerkPreset p : other.presets) {
            this.presets.add(PerkPreset.load(p.save()));
        }
    }


    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean("PerksEnabled", perksEnabled);
        tag.putInt("PerkSlots", MAX_PERK_SLOTS);

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
        perkSlots = MAX_PERK_SLOTS;

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
        sortPresets();
    }

    private void sortPresets() {
        presets.sort(Comparator.comparingInt(p -> switch (p.name) {
            case "Preset I" -> 0;
            case "Preset II" -> 1;
            case "Preset III" -> 2;
            default -> 100;
        }));
    }
}