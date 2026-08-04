package com.xirc.nichirin.common.system.perks;

import net.minecraft.world.item.Items;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central registry of all {@link PerkDefinition}s and {@link FlawDefinition}s.
 */
public final class NichirinPerkRegistry {

    private static final Map<String, PerkDefinition> PERKS = new LinkedHashMap<>();
    private static final Map<String, FlawDefinition> FLAWS = new LinkedHashMap<>();

    static {

        // BREATH
        register(PerkDefinition.builder("breath_efficiency", "Breath Efficiency")
                .description("Reduced breathing cost for techniques.")
                .icon(Items.SUGAR)
                .tiers(PerkTier.COMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.BREATH, PerkTag.STAMINA)
                .unlockHint("Found in perk scrolls hidden inside ancient temple structures.")
                .locationHint("Temple chests, early-tier dungeon loot.")
                .tierDesc(PerkTier.COMMON,    "Breathing techniques cost 5% less.")
                .tierDesc(PerkTier.UNCOMMON,  "Breathing techniques cost 10% less.")
                .tierDesc(PerkTier.RARE,      "Breathing techniques cost 18% less.")
                .tierDesc(PerkTier.EPIC,      "Breathing techniques cost 27% less.")
                .tierDesc(PerkTier.LEGENDARY, "Breathing techniques cost 35% less.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(5).item(Items.SUGAR, 8).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.SUGAR, 16).item(Items.BLAZE_POWDER, 4).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.BLAZE_POWDER, 8).item(Items.ENDER_PEARL, 2).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.BLAZE_ROD, 4).item(Items.ENDER_PEARL, 4).item(Items.GHAST_TEAR, 1).build())
                .build());

        register(PerkDefinition.builder("breath_recovery", "Breath Recovery")
                .description("Breathing regenerates faster when idle.")
                .icon(Items.GOLDEN_CARROT)
                .tiers(PerkTier.COMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.BREATH, PerkTag.STAMINA)
                .unlockHint("Granted by meditation-focused trainers or found in scroll caches.")
                .locationHint("Training ground ruins, buried near old dojo sites.")
                .tierDesc(PerkTier.COMMON,    "Idle breath recovery is 10% faster.")
                .tierDesc(PerkTier.UNCOMMON,  "Idle breath recovery is 18% faster.")
                .tierDesc(PerkTier.RARE,      "Idle breath recovery is 30% faster. Slow walking counts as idle.")
                .tierDesc(PerkTier.EPIC,      "Idle breath recovery is 45% faster. Slow walking counts as idle.")
                .tierDesc(PerkTier.LEGENDARY, "Idle breath recovery is 60% faster. Sprinting still pauses recovery.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(5).item(Items.GOLDEN_CARROT, 4).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.GOLDEN_CARROT, 8).item(Items.SUGAR, 8).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.GOLDEN_APPLE, 1).item(Items.BLAZE_POWDER, 6).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.BLAZE_ROD, 4).item(Items.GOLDEN_APPLE, 2).item(Items.GHAST_TEAR, 1).build())
                .build());

        register(PerkDefinition.builder("breath_focus", "Breath Focus")
                .description("Breathing techniques have more stun on hit.")
                .icon(Items.BLAZE_POWDER)
                .tiers(PerkTier.UNCOMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.BREATH, PerkTag.COMBAT)
                .unlockHint("Awarded to slayers who demonstrate mastery of multiple breath forms.")
                .locationHint("Hidden in deeper temple floors, behind locked rooms.")
                .tierDesc(PerkTier.UNCOMMON,  "Breath hits stun enemies for 0.1s longer.")
                .tierDesc(PerkTier.RARE,      "Breath hits stun enemies for 0.2s longer.")
                .tierDesc(PerkTier.EPIC,      "Breath hits stun enemies for 0.3s longer. Finisher stuns have +20% duration.")
                .tierDesc(PerkTier.LEGENDARY, "Breath hits stun for 0.4s longer. Finishers have +30% stun duration.")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.BLAZE_POWDER, 8).item(Items.IRON_INGOT, 4).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.BLAZE_ROD, 2).item(Items.GOLD_INGOT, 4).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.BLAZE_ROD, 6).item(Items.DIAMOND, 2).item(Items.GHAST_TEAR, 1).build())
                .build());

        register(PerkDefinition.builder("flow_state", "Flow State")
                .description("Hitting 5 consecutive breathing skills without missing boosts damage for 8 seconds.")
                .icon(Items.EXPERIENCE_BOTTLE)
                .tiers(PerkTier.RARE, PerkTier.LEGENDARY)
                .tags(PerkTag.BREATH, PerkTag.COMBAT)
                .unlockHint("Discovered deep within ancient libraries. Requires defeating a named demon to access.")
                .locationHint("Locked vaults beneath demon-occupied ruins.")
                .tierDesc(PerkTier.RARE,      "5 consecutive breath hits: +8% damage for 8s.")
                .tierDesc(PerkTier.EPIC,      "5 consecutive breath hits: +14% damage for 8s.")
                .tierDesc(PerkTier.LEGENDARY, "5 consecutive breath hits: +20% damage for 8s. Missing resets 1 stack.")
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.BLAZE_ROD, 3).item(Items.GOLD_INGOT, 6).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.BLAZE_ROD, 8).item(Items.DIAMOND, 3).item(Items.ENDER_PEARL, 2).build())
                .build());

        register(PerkDefinition.builder("zen_mastery", "Zen Mastery")
                .description("Reduced breathing cost when under 40% HP.")
                .icon(Items.LILY_OF_THE_VALLEY)
                .tiers(PerkTier.EPIC, PerkTier.LEGENDARY)
                .tags(PerkTag.BREATH, PerkTag.DEFENSE)
                .unlockHint("Bestowed only by Pillars or found in high-level corps archives.")
                .locationHint("Pillar-tier reward chests, hidden deep in sacred grounds.")
                .tierDesc(PerkTier.EPIC,      "Below 40% HP: breathing costs 15% less.")
                .tierDesc(PerkTier.LEGENDARY, "Below 40% HP: breathing costs 25% less and recovers 15% faster.")
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.BLAZE_ROD, 10).item(Items.DIAMOND, 4).item(Items.NETHER_STAR, 1).build())
                .build());

        register(PerkDefinition.builder("breath_overflow", "Breath Overflow")
                .description("Occasionally cast a breathing technique without consuming breathing.")
                .icon(Items.WATER_BUCKET)
                .tiers(PerkTier.EPIC, PerkTier.LEGENDARY)
                .tags(PerkTag.BREATH, PerkTag.SPECIAL)
                .unlockHint("A physical anomaly — this scroll chooses those with exceptional breathing capacity.")
                .locationHint("Sealed meditation chambers, off the beaten path in ancient temples.")
                .tierDesc(PerkTier.EPIC,      "6% chance per breath technique use to consume no breathing.")
                .tierDesc(PerkTier.LEGENDARY, "10% chance per breath technique use to consume no breathing.")
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.BLAZE_ROD, 10).item(Items.GOLDEN_APPLE, 2).item(Items.NETHER_STAR, 1).build())
                .build());

        // COMBAT
        register(PerkDefinition.builder("iron_will", "Iron Will")
                .description("Take reduced damage when below 30% HP.")
                .icon(Items.IRON_INGOT)
                .tiers(PerkTier.COMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT, PerkTag.DEFENSE)
                .unlockHint("Common scroll drop — one of the first perks a slayer can find.")
                .locationHint("Abandoned outposts, early dungeon rooms.")
                .tierDesc(PerkTier.COMMON,    "Below 30% HP: take 8% less damage.")
                .tierDesc(PerkTier.UNCOMMON,  "Below 30% HP: take 14% less damage.")
                .tierDesc(PerkTier.RARE,      "Below 30% HP: take 18% less damage.")
                .tierDesc(PerkTier.EPIC,      "Below 30% HP: take 25% less damage.")
                .tierDesc(PerkTier.LEGENDARY, "Below 30% HP: take 35% less damage.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(5).item(Items.IRON_INGOT, 8).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.IRON_INGOT, 16).item(Items.GOLD_INGOT, 4).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.GOLD_INGOT, 8).item(Items.DIAMOND, 2).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.DIAMOND, 4).item(Items.NETHER_STAR, 1).build())
                .build());

        register(PerkDefinition.builder("killer_instinct", "Killer Instinct")
                .description("Deal more damage to enemies under 40% health.")
                .icon(Items.IRON_SWORD)
                .tiers(PerkTier.UNCOMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT)
                .unlockHint("Unlocked by defeating a set number of demons. Tracks your kill count.")
                .locationHint("Corps mission reward for reaching a demon kill milestone.")
                .tierDesc(PerkTier.UNCOMMON,  "Deal +7% damage to enemies below 40% HP.")
                .tierDesc(PerkTier.RARE,      "Deal +12% damage to enemies below 40% HP.")
                .tierDesc(PerkTier.EPIC,      "Deal +18% damage to enemies below 40% HP.")
                .tierDesc(PerkTier.LEGENDARY, "Deal +25% damage to enemies below 40% HP.")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.IRON_SWORD, 1).item(Items.GOLD_INGOT, 4).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.DIAMOND_SWORD, 1).item(Items.GOLD_INGOT, 8).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.DIAMOND, 6).item(Items.WITHER_SKELETON_SKULL, 1).build())
                .build());

        register(PerkDefinition.builder("relentless_assault", "Relentless Assault")
                .description("Attacks have no windup for a short time after landing 3 consecutive hits.")
                .icon(Items.DIAMOND_SWORD)
                .tiers(PerkTier.UNCOMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT)
                .unlockHint("Scroll tucked away in sparring grounds and war-worn ruins.")
                .locationHint("Old battlefield ruins, near worn-down sword racks.")
                .tierDesc(PerkTier.UNCOMMON,  "3 consecutive hits: no attack windup for 2s.")
                .tierDesc(PerkTier.RARE,      "3 consecutive hits: no attack windup for 3s.")
                .tierDesc(PerkTier.EPIC,      "3 consecutive hits: no attack windup for 4s.")
                .tierDesc(PerkTier.LEGENDARY, "3 consecutive hits: no attack windup for 6s. Missing resets to 1 stack.")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.IRON_INGOT, 8).item(Items.BLAZE_POWDER, 4).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.GOLD_INGOT, 6).item(Items.BLAZE_ROD, 2).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.DIAMOND, 4).item(Items.BLAZE_ROD, 6).item(Items.ENDER_PEARL, 2).build())
                .build());

        register(PerkDefinition.builder("bloodthirst", "Bloodthirst")
                .description("Regen for a few seconds on kill.")
                .icon(Items.SPIDER_EYE)
                .tiers(PerkTier.RARE, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT, PerkTag.SURVIVAL)
                .unlockHint("A dark art — these scrolls aren't sold. They're taken from those who fell.")
                .locationHint("Found on slain named demons, or deep in blood-stained lairs.")
                .tierDesc(PerkTier.RARE,      "On kill: Regen I for 3 seconds.")
                .tierDesc(PerkTier.EPIC,      "On kill: Regen I for 4 seconds.")
                .tierDesc(PerkTier.LEGENDARY, "On kill: Regen I for 5 seconds. Killing blow restores 1 HP.")
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.ROTTEN_FLESH, 16).item(Items.SPIDER_EYE, 8).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.WITHER_SKELETON_SKULL, 1).item(Items.GHAST_TEAR, 2).item(Items.NETHER_WART, 16).build())
                .build());

        register(PerkDefinition.builder("glass_cannon", "Glass Cannon")
                .description("Increased attack power but takes more damage.")
                .icon(Items.GLASS_BOTTLE)
                .tiers(PerkTier.RARE, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT, PerkTag.SPECIAL)
                .unlockHint("Not sold or gifted — only found in the most dangerous areas.")
                .locationHint("End-room loot in mid-tier demon strongholds.")
                .tierDesc(PerkTier.RARE,      "+15% damage dealt. +22% damage taken.")
                .tierDesc(PerkTier.EPIC,      "+22% damage dealt. +28% damage taken.")
                .tierDesc(PerkTier.LEGENDARY, "+30% damage dealt. +36% damage taken.")
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.DIAMOND, 3).item(Items.BLAZE_ROD, 4).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.DIAMOND, 6).item(Items.WITHER_SKELETON_SKULL, 1).item(Items.NETHER_STAR, 1).build())
                .build());

        register(PerkDefinition.builder("juggernaut", "Juggernaut")
                .description("Reduced movement speed but higher defense.")
                .icon(Items.NETHERITE_INGOT)
                .tiers(PerkTier.RARE, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT, PerkTag.DEFENSE)
                .unlockHint("Earned by demonstrating raw strength — hidden in training pits and arena ruins.")
                .locationHint("Underground fighting pits, gladiatorial ruins.")
                .tierDesc(PerkTier.RARE,      "-15% movement speed. +12% damage resistance.")
                .tierDesc(PerkTier.EPIC,      "-10% movement speed. +18% damage resistance.")
                .tierDesc(PerkTier.LEGENDARY, "-6% movement speed. +28% damage resistance. Knockback resistance +25%.")
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.NETHERITE_SCRAP, 1).item(Items.IRON_INGOT, 16).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.NETHERITE_INGOT, 1).item(Items.DIAMOND, 4).item(Items.WITHER_SKELETON_SKULL, 1).build())
                .build());

        register(PerkDefinition.builder("blade_dancer", "Blade Dancer")
                .description("Slightly reduced damage per hit, but zero windup for attacks.")
                .icon(Items.GOLD_INGOT)
                .tiers(PerkTier.RARE, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT, PerkTag.MOVEMENT)
                .unlockHint("Developed by agile slayer lineages. Found among elegant, high-tier ruins.")
                .locationHint("Noble district ruins, ornate temple sub-chambers.")
                .tierDesc(PerkTier.RARE,      "-15% damage per hit. Zero attack windup.")
                .tierDesc(PerkTier.EPIC,      "-10% damage per hit. Zero attack windup. Faster followups.")
                .tierDesc(PerkTier.LEGENDARY, "-6% damage per hit. Zero attack windup.")
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.FEATHER, 16).item(Items.GOLD_INGOT, 6).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.PHANTOM_MEMBRANE, 4).item(Items.DIAMOND, 4).item(Items.ENDER_PEARL, 4).build())
                .build());

        register(PerkDefinition.builder("adrenaline_rush", "Adrenaline Rush")
                .description("Deal more damage the lower your HP is.")
                .icon(Items.BLAZE_ROD)
                .tiers(PerkTier.UNCOMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT, PerkTag.SURVIVAL)
                .unlockHint("A reactive warrior's perk — found where battles were frequent.")
                .locationHint("Old warfront ruins, mass-grave sites overtaken by demons.")
                .tierDesc(PerkTier.UNCOMMON,  "For every 10% HP missing, deal +1% more damage. (max +9% at 10% HP)")
                .tierDesc(PerkTier.RARE,      "For every 10% HP missing, deal +1.5% more damage. (max +13.5% at 10% HP)")
                .tierDesc(PerkTier.EPIC,      "For every 10% HP missing, deal +2% more damage. (max +18% at 10% HP)")
                .tierDesc(PerkTier.LEGENDARY, "For every 10% HP missing, deal +2.5% more damage. (max +22.5% at 10% HP)")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.BLAZE_POWDER, 8).item(Items.SUGAR, 8).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.BLAZE_ROD, 3).item(Items.GOLDEN_CARROT, 4).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.BLAZE_ROD, 6).item(Items.GOLDEN_APPLE, 1).item(Items.GHAST_TEAR, 1).build())
                .build());

        // MOVEMENT & SURVIVAL
        register(PerkDefinition.builder("ghost_step", "Ghost Step")
                .description("Increased i-frames on dodges.")
                .icon(Items.PHANTOM_MEMBRANE)
                .tiers(PerkTier.COMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.MOVEMENT, PerkTag.COMBAT)
                .unlockHint("Commonly found in temple side rooms and rooftop chests.")
                .locationHint("Temple rooftops, high-up hidden alcoves.")
                .tierDesc(PerkTier.COMMON,    "Dodge i-frames +2 ticks.")
                .tierDesc(PerkTier.UNCOMMON,  "Dodge i-frames +4 ticks.")
                .tierDesc(PerkTier.RARE,      "Dodge i-frames +6 ticks. Slight speed boost during dodge.")
                .tierDesc(PerkTier.EPIC,      "Dodge i-frames +8 ticks. Speed boost during dodge increased.")
                .tierDesc(PerkTier.LEGENDARY, "Dodge i-frames +8 ticks. Speed boost during dodge increased.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(5).item(Items.FEATHER, 8).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.FEATHER, 12).item(Items.PHANTOM_MEMBRANE, 1).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.PHANTOM_MEMBRANE, 4).item(Items.ENDER_PEARL, 2).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.PHANTOM_MEMBRANE, 6).item(Items.ENDER_PEARL, 4).build())
                .build());

        register(PerkDefinition.builder("enduring_spirit", "Enduring Spirit")
                .description("Reduced stamina drain from anything.")
                .icon(Items.IRON_CHESTPLATE)
                .tiers(PerkTier.COMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.STAMINA, PerkTag.SURVIVAL)
                .unlockHint("One of the most common scrolls — early corps recruits are often given one.")
                .locationHint("Corps supply caches, starting dungeon loot.")
                .tierDesc(PerkTier.COMMON,    "All stamina drain reduced by 8%.")
                .tierDesc(PerkTier.UNCOMMON,  "All stamina drain reduced by 16%.")
                .tierDesc(PerkTier.RARE,      "All stamina drain reduced by 27%.")
                .tierDesc(PerkTier.EPIC,      "All stamina drain reduced by 28%.")
                .tierDesc(PerkTier.LEGENDARY, "All stamina drain reduced by 38%. Stamina regenerates 8% faster.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(5).item(Items.IRON_INGOT, 4).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.IRON_INGOT, 12).item(Items.GOLDEN_CARROT, 2).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.GOLD_INGOT, 6).item(Items.DIAMOND, 1).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.DIAMOND, 3).item(Items.NETHER_STAR, 1).build())
                .build());

        register(PerkDefinition.builder("lightfoot", "Lightfoot")
                .description("Faster sprint speed with less stamina use.")
                .icon(Items.FEATHER)
                .tiers(PerkTier.COMMON, PerkTier.EPIC)
                .tags(PerkTag.MOVEMENT, PerkTag.STAMINA)
                .unlockHint("Basic movement training — one of the more widely available scrolls.")
                .locationHint("Road-side ruins, village abandoned houses.")
                .tierDesc(PerkTier.COMMON,    "Sprint speed +4%, sprint stamina cost -8%.")
                .tierDesc(PerkTier.UNCOMMON,  "Sprint speed +8%, sprint stamina cost -16%.")
                .tierDesc(PerkTier.RARE,      "Sprint speed +12%, sprint stamina cost -22%.")
                .tierDesc(PerkTier.EPIC,      "Sprint speed +14%, sprint stamina cost -25%.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(5).item(Items.FEATHER, 8).item(Items.LEATHER, 4).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.FEATHER, 16).item(Items.RABBIT_FOOT, 2).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.RABBIT_FOOT, 4).item(Items.ENDER_PEARL, 3).build())
                .build());

        register(PerkDefinition.builder("second_wind", "Second Wind")
                .description("Stamina regenerates faster after being fully depleted.")
                .icon(Items.GLISTERING_MELON_SLICE)
                .tiers(PerkTier.UNCOMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.STAMINA, PerkTag.SURVIVAL)
                .unlockHint("Earned through surviving close-call combat encounters. Rare scroll reward.")
                .locationHint("Hidden behind destructible walls in mid-tier dungeons.")
                .tierDesc(PerkTier.UNCOMMON,  "After full depletion, stamina recovers 20% faster for 5s.")
                .tierDesc(PerkTier.RARE,      "After full depletion, stamina recovers 40% faster for 7s.")
                .tierDesc(PerkTier.EPIC,      "After full depletion, stamina recovers 65% faster for 9s.")
                .tierDesc(PerkTier.LEGENDARY, "After full depletion, stamina instantly refills 15% and then recovers 80% faster.")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.SUGAR, 8).item(Items.GOLDEN_CARROT, 2).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.GHAST_TEAR, 2).item(Items.GOLDEN_CARROT, 4).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.GHAST_TEAR, 4).item(Items.GOLDEN_APPLE, 1).item(Items.NETHER_STAR, 1).build())
                .build());

        register(PerkDefinition.builder("steadfast", "Steadfast")
                .description("Reduced knockback from enemy attacks.")
                .icon(Items.OBSIDIAN)
                .tiers(PerkTier.COMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.DEFENSE)
                .unlockHint("Foundational defense training — easy to come across in early temples.")
                .locationHint("Guard post ruins, blockhouse rubble.")
                .tierDesc(PerkTier.COMMON,    "Knockback resistance +15%.")
                .tierDesc(PerkTier.UNCOMMON,  "Knockback resistance +30%.")
                .tierDesc(PerkTier.RARE,      "Knockback resistance +50%.")
                .tierDesc(PerkTier.EPIC,      "Knockback resistance +70%.")
                .tierDesc(PerkTier.LEGENDARY, "Knockback resistance +80%.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(5).item(Items.IRON_INGOT, 8).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.IRON_INGOT, 16).item(Items.OBSIDIAN, 4).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.OBSIDIAN, 8).item(Items.DIAMOND, 3).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.OBSIDIAN, 12).item(Items.DIAMOND, 5).item(Items.NETHERITE_SCRAP, 1).build())
                .build());

        register(PerkDefinition.builder("iron_core", "Iron Core")
                .description("Increased stamina regeneration delay, but overall regen rate is higher.")
                .icon(Items.IRON_BLOCK)
                .tiers(PerkTier.UNCOMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.STAMINA, PerkTag.DEFENSE)
                .unlockHint("Forged through physical discipline — found in blacksmith ruins and forge sites.")
                .locationHint("Collapsed forge complexes, smithing districts of ruined towns.")
                .tierDesc(PerkTier.UNCOMMON,  "Regen delay +0.5s, but regen rate +25%.")
                .tierDesc(PerkTier.RARE,      "Regen delay +1s, but regen rate +50%.")
                .tierDesc(PerkTier.EPIC,      "Regen delay +1.5s, but regen rate +80%.")
                .tierDesc(PerkTier.LEGENDARY, "Regen delay +2s, but regen rate +120%. Regen cannot be interrupted by light hits.")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.IRON_INGOT, 12).item(Items.OBSIDIAN, 2).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.DIAMOND, 3).item(Items.OBSIDIAN, 6).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.DIAMOND, 6).item(Items.NETHERITE_SCRAP, 2).build())
                .build());

        register(PerkDefinition.builder("featherweight", "Featherweight")
                .description("Reduced fall damage.")
                .icon(Items.RABBIT_FOOT)
                .tiers(PerkTier.COMMON, PerkTier.EPIC)
                .tags(PerkTag.MOVEMENT)
                .unlockHint("Developed by acrobatic slayer lineages. Found in many surface caches.")
                .locationHint("High-altitude structures, cliff-edge ruins.")
                .tierDesc(PerkTier.COMMON,    "Fall damage reduced by 20%.")
                .tierDesc(PerkTier.UNCOMMON,  "Fall damage reduced by 40%.")
                .tierDesc(PerkTier.RARE,      "Fall damage reduced by 65%.")
                .tierDesc(PerkTier.EPIC,      "Fall damage reduced by 80%.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(5).item(Items.FEATHER, 8).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.FEATHER, 16).item(Items.RABBIT_FOOT, 2).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.RABBIT_FOOT, 4).item(Items.PHANTOM_MEMBRANE, 2).build())
                .build());

        register(PerkDefinition.builder("swift_recovery", "Swift Recovery")
                .description("Heal slightly faster when eating food.")
                .icon(Items.GOLDEN_APPLE)
                .tiers(PerkTier.COMMON, PerkTier.EPIC)
                .tags(PerkTag.SURVIVAL)
                .unlockHint("Basic slayer resilience training. Found in early-tier loot fairly often.")
                .locationHint("Medic posts, healer outpost ruins near corps supply lines.")
                .tierDesc(PerkTier.COMMON,    "Food heals 12% more HP over its duration.")
                .tierDesc(PerkTier.UNCOMMON,  "Food heals 25% more HP over its duration.")
                .tierDesc(PerkTier.RARE,      "Food heals 40% more HP. Eating speed increased by 15%.")
                .tierDesc(PerkTier.EPIC,      "Food heals 60% more HP. Eating speed increased by 30%. Saturation bonus doubled.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(5).item(Items.GOLDEN_CARROT, 4).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.GOLDEN_CARROT, 8).item(Items.GOLDEN_APPLE, 1).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.GOLDEN_APPLE, 2).item(Items.DIAMOND, 1).build())
                .build());

        // SPECIAL / SITUATIONAL
        register(PerkDefinition.builder("hunters_instinct", "Hunter's Instinct")
                .description("Passive enemy detection at short range.")
                .icon(Items.ENDER_EYE)
                .tiers(PerkTier.COMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.SPECIAL, PerkTag.SURVIVAL)
                .unlockHint("Standard-issue for new hunters. Easy to obtain early on.")
                .locationHint("Corps outpost supply rooms, near hunter lodges.")
                .tierDesc(PerkTier.COMMON,    "Demons glow faintly within 12 blocks.")
                .tierDesc(PerkTier.UNCOMMON,  "Demons glow within 20 blocks. Slight sound cue when one is near.")
                .tierDesc(PerkTier.RARE,      "Demons glow within 30 blocks. Compass-style awareness of closest demon.")
                .tierDesc(PerkTier.EPIC,      "Demons glow within 48 blocks. See demon HP bars through walls.")
                .tierDesc(PerkTier.LEGENDARY, "Permanent radar for all demons within 64 blocks. Instant alert on aggro.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(5).item(Items.SPIDER_EYE, 8).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.FERMENTED_SPIDER_EYE, 4).item(Items.ENDER_EYE, 2).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.ENDER_EYE, 6).item(Items.ENDER_PEARL, 4).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.ENDER_EYE, 10).item(Items.DRAGON_BREATH, 2).build())
                .build());

        register(PerkDefinition.builder("night_prowler", "Night Prowler")
                .description("Increased movement speed at night.")
                .icon(Items.INK_SAC)
                .tiers(PerkTier.UNCOMMON, PerkTier.EPIC)
                .tags(PerkTag.MOVEMENT, PerkTag.SPECIAL)
                .unlockHint("Discovered in sealed crypts and night-focused demon shrines.")
                .locationHint("Underground shrines, sealed crypts only accessible at night.")
                .tierDesc(PerkTier.UNCOMMON,  "At night: movement speed +6%. Night Vision passive.")
                .tierDesc(PerkTier.RARE,      "At night: movement speed +12%. Night Vision passive.")
                .tierDesc(PerkTier.EPIC,      "At night: movement speed +20%. Night Vision passive. Slight invisibility in shadows.")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.ENDER_PEARL, 4).item(Items.PHANTOM_MEMBRANE, 2).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.PHANTOM_MEMBRANE, 6).item(Items.ENDER_EYE, 4).build())
                .build());

        register(PerkDefinition.builder("silent_step", "Silent Step")
                .description("Lower enemy detection range. Mob AI is focused on you only when you're very close.")
                .icon(Items.SLIME_BALL)
                .tiers(PerkTier.UNCOMMON, PerkTier.RARE)
                .tags(PerkTag.MOVEMENT, PerkTag.SPECIAL)
                .unlockHint("Taught by rogue slayers who prefer stealth over force.")
                .locationHint("Hidden chambers in heavily demon-patrolled ruins.")
                .tierDesc(PerkTier.UNCOMMON,  "Mob detection range halved. Mobs de-aggro faster.")
                .tierDesc(PerkTier.RARE,      "Mob detection range reduced by 70%. Mobs won't aggro unless within 6 blocks.")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(9).item(Items.PHANTOM_MEMBRANE, 4).item(Items.SLIME_BALL, 8).build())
                .build());

        register(PerkDefinition.builder("vermilion_soul", "Vermilion Soul")
                .description("If you die, revive once per in-game day with reduced HP.")
                .icon(Items.TOTEM_OF_UNDYING)
                .tiers(PerkTier.EPIC, PerkTier.LEGENDARY)
                .tags(PerkTag.SPECIAL, PerkTag.SURVIVAL)
                .unlockHint("Tied to a rare soul trait — only some slayers can awaken it.")
                .locationHint("Sealed chambers in fire-corrupted ruins, deep nether-adjacent structures.")
                .tierDesc(PerkTier.EPIC,      "Once per in-game day: on death, revive with 25% HP.")
                .tierDesc(PerkTier.LEGENDARY, "Once per in-game day: on death, revive with 30% HP.")
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.TOTEM_OF_UNDYING, 1).item(Items.NETHER_STAR, 1).item(Items.BLAZE_ROD, 8).build())
                .build());

        register(PerkDefinition.builder("moonlit_fury", "Moonlit Fury")
                .description("At night, gain a stacking damage boost for every enemy killed (resets at dawn).")
                .icon(Items.PHANTOM_MEMBRANE)
                .tiers(PerkTier.EPIC, PerkTier.LEGENDARY)
                .tags(PerkTag.SPECIAL, PerkTag.COMBAT)
                .unlockHint("Said to only reveal itself under a blood moon. Keep watch.")
                .locationHint("Spawns during blood moon events — check unusual spots after one ends.")
                .tierDesc(PerkTier.EPIC,      "Each kill at night: +1.5% damage (stacks up to 8x, resets at dawn).")
                .tierDesc(PerkTier.LEGENDARY, "Each kill at night: +1.5% damage (stacks up to 10x, resets at dawn).")
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.PHANTOM_MEMBRANE, 8).item(Items.DIAMOND, 6).item(Items.NETHER_STAR, 1).build())
                .build());

        register(PerkDefinition.builder("unbreakable", "Unbreakable")
                .description("Once per fight, survive a fatal blow with 1 HP. Has a cooldown.")
                .icon(Items.SHIELD)
                .tiers(PerkTier.EPIC, PerkTier.LEGENDARY)
                .tags(PerkTag.DEFENSE, PerkTag.SPECIAL)
                .unlockHint("Given only to slayers who have survived the impossible.")
                .locationHint("Deep within the most treacherous demon strongholds. Few return.")
                .tierDesc(PerkTier.EPIC,      "Once per fight: survive a fatal blow with 1 HP. 10 minute cooldown.")
                .tierDesc(PerkTier.LEGENDARY, "Once per fight: survive a fatal blow with 1 HP. 8 minute cooldown.")
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.NETHERITE_INGOT, 2).item(Items.TOTEM_OF_UNDYING, 1).item(Items.NETHER_STAR, 1).build())
                .build());

        // PINNACLE
        register(PerkDefinition.builder("slayers_resolve", "Slayer's Resolve")
                .description("Deal extra damage proportional to your missing HP.")
                .icon(Items.NETHER_STAR)
                .tiers(PerkTier.LEGENDARY, PerkTier.LEGENDARY)
                .tags(PerkTag.SPECIAL, PerkTag.COMBAT, PerkTag.SURVIVAL)
                .unlockHint("Cannot be found — must be earned. A certain advancement will reveal the way.")
                .locationHint("The path is the key. Push further than any slayer has before.")
                .tierDesc(PerkTier.LEGENDARY, "For every 1% HP missing, deal 0.3% more damage. At 50% HP: +15% damage. At 10% HP: +27% damage.")
                .build());

        // FLAWS
        registerFlaw(new FlawDefinition("one_lung",       "One Lung",       "Your lungs are damaged. Maximum breath is halved."));
        registerFlaw(new FlawDefinition("atrophied",      "Atrophied",      "Your muscles have wasted away. Maximum stamina is reduced by 40%."));
        registerFlaw(new FlawDefinition("glass_stance",   "Glass Stance",   "Your guard shatters on contact. Your stance breaks instantly on the first hit, even while blocking."));
        registerFlaw(new FlawDefinition("cursed_eyes",    "Cursed Eyes",    "Your vision is blighted. You have permanent Blindness and cannot benefit from Night Vision."));
        registerFlaw(new FlawDefinition("daywalker",      "Daywalker",      "You belong to the day. At night, your damage, movement and attack speed are halved."));
        registerFlaw(new FlawDefinition("hollow",         "Hollow",         "Your body cannot store nourishment. Hunger drains rapidly and food gives no saturation."));
        registerFlaw(new FlawDefinition("sleep_deprived", "Sleep Deprived", "You cannot sleep. Beds refuse to accept you."));
        registerFlaw(new FlawDefinition("forsaken",       "Forsaken",       "Food can no longer mend you. Natural food-based regeneration is suppressed entirely."));
        registerFlaw(new FlawDefinition("anchored",       "Anchored",       "You are tethered to the ground. You cannot dodge or air-dodge while above 50% stamina."));
    }

    private static void register(PerkDefinition def) { PERKS.put(def.id, def); }
    private static void registerFlaw(FlawDefinition def) { FLAWS.put(def.id, def); }

    public static PerkDefinition getPerk(String id) { return PERKS.get(id); }
    public static FlawDefinition getFlaw(String id) { return FLAWS.get(id); }
    public static Collection<PerkDefinition> allPerks() { return Collections.unmodifiableCollection(PERKS.values()); }
    public static Collection<FlawDefinition> allFlaws() { return Collections.unmodifiableCollection(FLAWS.values()); }

    private NichirinPerkRegistry() {}
}
