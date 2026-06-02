package com.xirc.nichirin.common.system.perks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * A saved perk loadout with a user-defined name.
 *
 * <p>Stores the names (IDs) of equipped perks and their tiers, plus flaw IDs,
 * but does NOT store the full definitions — those are always looked up from
 * {@link NichirinPerkRegistry} at runtime.</p>
 */
public class PerkPreset {

    public String name;
    /** Ordered list of perk IDs in equipped slots (max 5). */
    public final List<String> perkIds = new ArrayList<>();
    /** Per-perk tiers, parallel to perkIds. */
    public final List<PerkTier> perkTiers = new ArrayList<>();
    /** Equipped flaw IDs. */
    public final List<String> flawIds = new ArrayList<>();

    public PerkPreset(String name) {
        this.name = name;
    }


    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", name);

        ListTag perksTag = new ListTag();
        for (int i = 0; i < perkIds.size(); i++) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Id", perkIds.get(i));
            entry.putInt("Tier", perkTiers.get(i).level);
            perksTag.add(entry);
        }
        tag.put("Perks", perksTag);

        ListTag flawsTag = new ListTag();
        for (String fid : flawIds) {
            flawsTag.add(StringTag.valueOf(fid));
        }
        tag.put("Flaws", flawsTag);
        return tag;
    }

    public static PerkPreset load(CompoundTag tag) {
        PerkPreset preset = new PerkPreset(tag.getString("Name"));
        ListTag perksTag = tag.getList("Perks", Tag.TAG_COMPOUND);
        for (int i = 0; i < perksTag.size(); i++) {
            CompoundTag entry = perksTag.getCompound(i);
            preset.perkIds.add(entry.getString("Id"));
            preset.perkTiers.add(PerkTier.fromLevel(entry.getInt("Tier")));
        }
        ListTag flawsTag = tag.getList("Flaws", Tag.TAG_STRING);
        for (int i = 0; i < flawsTag.size(); i++) {
            preset.flawIds.add(flawsTag.getString(i));
        }
        return preset;
    }
}