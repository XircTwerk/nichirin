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

        // ── Breathing perks ───────────────────────────────────────────────────

        register(PerkDefinition.builder("breath_efficiency", "Breath Efficiency")
                .description("Reduced breathing cost for techniques.")
                .icon(Items.SUGAR)
                .tiers(PerkTier.COMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.BREATH, PerkTag.STAMINA)
                .unlockHint("Found in perk scrolls hidden inside ancient temple structures.")
                .locationHint("Temple chests, early-tier dungeon loot.")
                .tierDesc(PerkTier.COMMON,    "Breathing techniques cost 10% less.")
                .tierDesc(PerkTier.UNCOMMON,  "Breathing techniques cost 20% less.")
                .tierDesc(PerkTier.RARE,      "Breathing techniques cost 35% less.")
                .tierDesc(PerkTier.EPIC,      "Breathing techniques cost 50% less.")
                .tierDesc(PerkTier.LEGENDARY, "Breathing techniques cost 65% less. Passively recover 1 breath per second.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(3).item(Items.SUGAR, 8).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(6).item(Items.SUGAR, 16).item(Items.BLAZE_POWDER, 4).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(10).item(Items.BLAZE_POWDER, 8).item(Items.ENDER_PEARL, 2).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(16).item(Items.BLAZE_ROD, 4).item(Items.ENDER_PEARL, 4).item(Items.GHAST_TEAR, 1).build())
                .build());

        register(PerkDefinition.builder("breath_recovery", "Breath Recovery")
                .description("Breathing regenerates faster when idle.")
                .icon(Items.GOLDEN_CARROT)
                .tiers(PerkTier.COMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.BREATH, PerkTag.STAMINA)
                .unlockHint("Granted by meditation-focused trainers or found in scroll caches.")
                .locationHint("Training ground ruins, buried near old dojo sites.")
                .tierDesc(PerkTier.COMMON,    "Idle breath recovery is 20% faster.")
                .tierDesc(PerkTier.UNCOMMON,  "Idle breath recovery is 35% faster.")
                .tierDesc(PerkTier.RARE,      "Idle breath recovery is 55% faster. Slow walking counts as idle.")
                .tierDesc(PerkTier.EPIC,      "Idle breath recovery is 75% faster. Walking counts as idle.")
                .tierDesc(PerkTier.LEGENDARY, "Breath recovery is near-instant while idle. Sprinting no longer pauses recovery.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(3).item(Items.GOLDEN_CARROT, 4).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(6).item(Items.GOLDEN_CARROT, 8).item(Items.SUGAR, 8).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(10).item(Items.GOLDEN_APPLE, 1).item(Items.BLAZE_POWDER, 6).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(16).item(Items.BLAZE_ROD, 4).item(Items.GOLDEN_APPLE, 2).item(Items.GHAST_TEAR, 1).build())
                .build());

        register(PerkDefinition.builder("breath_focus", "Breath Focus")
                .description("Breathing techniques have more stun on hit.")
                .icon(Items.BLAZE_POWDER)
                .tiers(PerkTier.UNCOMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.BREATH, PerkTag.COMBAT)
                .unlockHint("Awarded to slayers who demonstrate mastery of multiple breath forms.")
                .locationHint("Hidden in deeper temple floors, behind locked rooms.")
                .tierDesc(PerkTier.UNCOMMON,  "Breath hits stun enemies for 0.2s longer.")
                .tierDesc(PerkTier.RARE,      "Breath hits stun enemies for 0.4s longer.")
                .tierDesc(PerkTier.EPIC,      "Breath hits stun enemies for 0.7s longer. Finisher stuns have +50% duration.")
                .tierDesc(PerkTier.LEGENDARY, "Breath hits stun for 1s longer. Finishers can briefly root enemies in place.")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(5).item(Items.BLAZE_POWDER, 8).item(Items.IRON_INGOT, 4).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(10).item(Items.BLAZE_ROD, 2).item(Items.GOLD_INGOT, 4).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(18).item(Items.BLAZE_ROD, 6).item(Items.DIAMOND, 2).item(Items.GHAST_TEAR, 1).build())
                .build());

        register(PerkDefinition.builder("flow_state", "Flow State")
                .description("Hitting 5 consecutive breathing skills without missing boosts damage for 10 seconds.")
                .icon(Items.EXPERIENCE_BOTTLE)
                .tiers(PerkTier.RARE, PerkTier.LEGENDARY)
                .tags(PerkTag.BREATH, PerkTag.COMBAT)
                .unlockHint("Discovered deep within ancient libraries. Requires defeating a named demon to access.")
                .locationHint("Locked vaults beneath demon-occupied ruins.")
                .tierDesc(PerkTier.RARE,      "5 consecutive breath hits: +15% damage for 10s.")
                .tierDesc(PerkTier.EPIC,      "5 consecutive breath hits: +25% damage for 10s. Missing resets only 2 stacks.")
                .tierDesc(PerkTier.LEGENDARY, "5 consecutive breath hits: +35% damage for 10s. Missing resets 0 stacks for 3 seconds.")
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(12).item(Items.BLAZE_ROD, 3).item(Items.GOLD_INGOT, 6).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(20).item(Items.BLAZE_ROD, 8).item(Items.DIAMOND, 3).item(Items.ENDER_PEARL, 2).build())
                .build());

        register(PerkDefinition.builder("zen_mastery", "Zen Mastery")
                .description("Reduced breathing cost when under 50% HP.")
                .icon(Items.LILY_OF_THE_VALLEY)
                .tiers(PerkTier.EPIC, PerkTier.LEGENDARY)
                .tags(PerkTag.BREATH, PerkTag.DEFENSE)
                .unlockHint("Bestowed only by Pillars or found in high-level corps archives.")
                .locationHint("Pillar-tier reward chests, hidden deep in sacred grounds.")
                .tierDesc(PerkTier.EPIC,      "Below 50% HP: breathing costs 40% less.")
                .tierDesc(PerkTier.LEGENDARY, "Below 50% HP: breathing costs 70% less and recovers 50% faster.")
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(25).item(Items.BLAZE_ROD, 10).item(Items.DIAMOND, 4).item(Items.NETHER_STAR, 1).build())
                .build());

        // ── Combat perks ───────────────────────────────────────────────────────

        register(PerkDefinition.builder("iron_will", "Iron Will")
                .description("Take reduced damage when below 30% HP.")
                .icon(Items.IRON_INGOT)
                .tiers(PerkTier.COMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT, PerkTag.DEFENSE)
                .unlockHint("Common scroll drop — one of the first perks a slayer can find.")
                .locationHint("Abandoned outposts, early dungeon rooms.")
                .tierDesc(PerkTier.COMMON,    "Below 30% HP: take 10% less damage.")
                .tierDesc(PerkTier.UNCOMMON,  "Below 30% HP: take 18% less damage.")
                .tierDesc(PerkTier.RARE,      "Below 30% HP: take 28% less damage.")
                .tierDesc(PerkTier.EPIC,      "Below 30% HP: take 40% less damage.")
                .tierDesc(PerkTier.LEGENDARY, "Below 30% HP: take 55% less damage. Brief speed boost on proc.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(4).item(Items.IRON_INGOT, 8).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(8).item(Items.IRON_INGOT, 16).item(Items.GOLD_INGOT, 4).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.GOLD_INGOT, 8).item(Items.DIAMOND, 2).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.DIAMOND, 4).item(Items.NETHER_STAR, 1).build())
                .build());

        register(PerkDefinition.builder("killer_instinct", "Killer Instinct")
                .description("Deal more damage to enemies under half health.")
                .icon(Items.IRON_SWORD)
                .tiers(PerkTier.UNCOMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT)
                .unlockHint("Unlocked by defeating a set number of demons. Tracks your kill count.")
                .locationHint("Corps mission reward for reaching a demon kill milestone.")
                .tierDesc(PerkTier.UNCOMMON,  "Deal +12% damage to enemies below 50% HP.")
                .tierDesc(PerkTier.RARE,      "Deal +22% damage to enemies below 50% HP.")
                .tierDesc(PerkTier.EPIC,      "Deal +35% damage to enemies below 50% HP.")
                .tierDesc(PerkTier.LEGENDARY, "Deal +50% damage to enemies below 50% HP. Critical hit chance doubled vs low-HP targets.")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(6).item(Items.IRON_SWORD, 1).item(Items.GOLD_INGOT, 4).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(12).item(Items.DIAMOND_SWORD, 1).item(Items.GOLD_INGOT, 8).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(20).item(Items.DIAMOND, 6).item(Items.WITHER_SKELETON_SKULL, 1).build())
                .build());

        register(PerkDefinition.builder("relentless_assault", "Relentless Assault")
                .description("Attacks have no windup for 7 seconds after landing 3 consecutive hits.")
                .icon(Items.DIAMOND_SWORD)
                .tiers(PerkTier.UNCOMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT)
                .unlockHint("Scroll tucked away in sparring grounds and war-worn ruins.")
                .locationHint("Old battlefield ruins, near worn-down sword racks.")
                .tierDesc(PerkTier.UNCOMMON,  "3 consecutive hits: no attack windup for 5s.")
                .tierDesc(PerkTier.RARE,      "3 consecutive hits: no attack windup for 7s.")
                .tierDesc(PerkTier.EPIC,      "3 consecutive hits: no attack windup for 10s. Missing resets to 2 hits instead of 0.")
                .tierDesc(PerkTier.LEGENDARY, "3 consecutive hits: no attack windup for 14s. Missing never fully resets the counter.")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(6).item(Items.IRON_INGOT, 8).item(Items.BLAZE_POWDER, 4).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(12).item(Items.GOLD_INGOT, 6).item(Items.BLAZE_ROD, 2).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(20).item(Items.DIAMOND, 4).item(Items.BLAZE_ROD, 6).item(Items.ENDER_PEARL, 2).build())
                .build());

        register(PerkDefinition.builder("bloodthirst", "Bloodthirst")
                .description("Regen for 3 seconds on kill.")
                .icon(Items.SPIDER_EYE)
                .tiers(PerkTier.RARE, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT, PerkTag.SURVIVAL)
                .unlockHint("A dark art — these scrolls aren't sold. They're taken from those who fell.")
                .locationHint("Found on slain named demons, or deep in blood-stained lairs.")
                .tierDesc(PerkTier.RARE,      "On kill: Regen I for 3 seconds.")
                .tierDesc(PerkTier.EPIC,      "On kill: Regen II for 3 seconds.")
                .tierDesc(PerkTier.LEGENDARY, "On kill: Regen III for 3 seconds. Bonus: killing blow restores 2 HP instantly.")
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(12).item(Items.ROTTEN_FLESH, 16).item(Items.SPIDER_EYE, 8).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(20).item(Items.WITHER_SKELETON_SKULL, 1).item(Items.GHAST_TEAR, 2).item(Items.NETHER_WART, 16).build())
                .build());

        register(PerkDefinition.builder("ghost_step", "Ghost Step")
                .description("Slightly increased i-frames on dodges.")
                .icon(Items.PHANTOM_MEMBRANE)
                .tiers(PerkTier.COMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.MOVEMENT, PerkTag.COMBAT)
                .unlockHint("Commonly found in temple side rooms and rooftop chests.")
                .locationHint("Temple rooftops, high-up hidden alcoves.")
                .tierDesc(PerkTier.COMMON,    "Dodge i-frames +2 ticks.")
                .tierDesc(PerkTier.UNCOMMON,  "Dodge i-frames +4 ticks.")
                .tierDesc(PerkTier.RARE,      "Dodge i-frames +6 ticks. Slight speed boost during dodge.")
                .tierDesc(PerkTier.EPIC,      "Dodge i-frames +9 ticks. Speed boost during dodge increased.")
                .tierDesc(PerkTier.LEGENDARY, "Dodge i-frames +12 ticks. Leaving i-frames briefly boosts attack speed.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(3).item(Items.FEATHER, 8).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(6).item(Items.FEATHER, 12).item(Items.PHANTOM_MEMBRANE, 1).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(10).item(Items.PHANTOM_MEMBRANE, 4).item(Items.ENDER_PEARL, 2).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(18).item(Items.PHANTOM_MEMBRANE, 6).item(Items.ENDER_PEARL, 4).build())
                .build());

        // ── Stamina / Movement perks ───────────────────────────────────────────

        register(PerkDefinition.builder("enduring_spirit", "Enduring Spirit")
                .description("Reduced stamina drain from anything.")
                .icon(Items.IRON_CHESTPLATE)
                .tiers(PerkTier.COMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.STAMINA, PerkTag.SURVIVAL)
                .unlockHint("One of the most common scrolls — early corps recruits are often given one.")
                .locationHint("Corps supply caches, starting dungeon loot.")
                .tierDesc(PerkTier.COMMON,    "All stamina drain reduced by 10%.")
                .tierDesc(PerkTier.UNCOMMON,  "All stamina drain reduced by 20%.")
                .tierDesc(PerkTier.RARE,      "All stamina drain reduced by 32%.")
                .tierDesc(PerkTier.EPIC,      "All stamina drain reduced by 45%.")
                .tierDesc(PerkTier.LEGENDARY, "All stamina drain reduced by 60%. Stamina regenerates 15% faster.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(3).item(Items.IRON_INGOT, 4).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(7).item(Items.IRON_INGOT, 12).item(Items.GOLDEN_CARROT, 2).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(13).item(Items.GOLD_INGOT, 6).item(Items.DIAMOND, 1).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(20).item(Items.DIAMOND, 3).item(Items.NETHER_STAR, 1).build())
                .build());

        register(PerkDefinition.builder("lightfoot", "Lightfoot")
                .description("Faster sprint speed with less stamina use.")
                .icon(Items.FEATHER)
                .tiers(PerkTier.COMMON, PerkTier.EPIC)
                .tags(PerkTag.MOVEMENT, PerkTag.STAMINA)
                .unlockHint("Basic movement training — one of the more widely available scrolls.")
                .locationHint("Road-side ruins, village abandoned houses.")
                .tierDesc(PerkTier.COMMON,    "Sprint speed +5%, sprint stamina cost -10%.")
                .tierDesc(PerkTier.UNCOMMON,  "Sprint speed +10%, sprint stamina cost -20%.")
                .tierDesc(PerkTier.RARE,      "Sprint speed +16%, sprint stamina cost -30%.")
                .tierDesc(PerkTier.EPIC,      "Sprint speed +22%, sprint stamina cost -45%. Sprint starts instantly.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(3).item(Items.FEATHER, 8).item(Items.LEATHER, 4).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(7).item(Items.FEATHER, 16).item(Items.RABBIT_FOOT, 2).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(12).item(Items.RABBIT_FOOT, 4).item(Items.ENDER_PEARL, 3).build())
                .build());

        register(PerkDefinition.builder("second_wind", "Second Wind")
                .description("Stamina regenerates faster after being fully depleted.")
                .icon(Items.GLISTERING_MELON_SLICE)
                .tiers(PerkTier.UNCOMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.STAMINA, PerkTag.SURVIVAL)
                .unlockHint("Earned through surviving close-call combat encounters. Rare scroll reward.")
                .locationHint("Hidden behind destructible walls in mid-tier dungeons.")
                .tierDesc(PerkTier.UNCOMMON,  "After full depletion, stamina recovers 25% faster for 5s.")
                .tierDesc(PerkTier.RARE,      "After full depletion, stamina recovers 50% faster for 7s.")
                .tierDesc(PerkTier.EPIC,      "After full depletion, stamina recovers 80% faster for 9s.")
                .tierDesc(PerkTier.LEGENDARY, "After full depletion, stamina instantly refills 20% and then recovers 100% faster.")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(5).item(Items.SUGAR, 8).item(Items.GOLDEN_CARROT, 2).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(10).item(Items.GHAST_TEAR, 2).item(Items.GOLDEN_CARROT, 4).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(18).item(Items.GHAST_TEAR, 4).item(Items.GOLDEN_APPLE, 1).item(Items.NETHER_STAR, 1).build())
                .build());

        register(PerkDefinition.builder("steadfast", "Steadfast")
                .description("Reduced knockback from enemy attacks.")
                .icon(Items.OBSIDIAN)
                .tiers(PerkTier.COMMON, PerkTier.EPIC)
                .tags(PerkTag.DEFENSE)
                .unlockHint("Foundational defense training — easy to come across in early temples.")
                .locationHint("Guard post ruins, blockhouse rubble.")
                .tierDesc(PerkTier.COMMON,    "Knockback resistance +20%.")
                .tierDesc(PerkTier.UNCOMMON,  "Knockback resistance +40%.")
                .tierDesc(PerkTier.RARE,      "Knockback resistance +60%.")
                .tierDesc(PerkTier.EPIC,      "Full knockback immunity.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(3).item(Items.IRON_INGOT, 8).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(7).item(Items.IRON_INGOT, 16).item(Items.OBSIDIAN, 4).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.OBSIDIAN, 8).item(Items.DIAMOND, 3).build())
                .build());

        register(PerkDefinition.builder("iron_core", "Iron Core")
                .description("Increased stamina regeneration delay, but overall regen rate is higher.")
                .icon(Items.IRON_BLOCK)
                .tiers(PerkTier.UNCOMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.STAMINA, PerkTag.DEFENSE)
                .unlockHint("Forged through physical discipline — found in blacksmith ruins and forge sites.")
                .locationHint("Collapsed forge complexes, smithing districts of ruined towns.")
                .tierDesc(PerkTier.UNCOMMON,  "Regen delay +1s, but regen rate +30%.")
                .tierDesc(PerkTier.RARE,      "Regen delay +1.5s, but regen rate +55%.")
                .tierDesc(PerkTier.EPIC,      "Regen delay +2s, but regen rate +85%.")
                .tierDesc(PerkTier.LEGENDARY, "Regen delay +2.5s, but regen rate +130%. Regen cannot be interrupted by light hits.")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(6).item(Items.IRON_INGOT, 12).item(Items.OBSIDIAN, 2).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(12).item(Items.DIAMOND, 3).item(Items.OBSIDIAN, 6).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(20).item(Items.DIAMOND, 6).item(Items.NETHERITE_SCRAP, 2).build())
                .build());

        // ── Awareness perks ────────────────────────────────────────────────────

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
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(3).item(Items.SPIDER_EYE, 8).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(6).item(Items.FERMENTED_SPIDER_EYE, 4).item(Items.ENDER_EYE, 2).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(10).item(Items.ENDER_EYE, 6).item(Items.ENDER_PEARL, 4).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(18).item(Items.ENDER_EYE, 10).item(Items.DRAGON_BREATH, 2).build())
                .build());

        register(PerkDefinition.builder("night_prowler", "Night Prowler")
                .description("Increased movement speed at night.")
                .icon(Items.INK_SAC)
                .tiers(PerkTier.UNCOMMON, PerkTier.EPIC)
                .tags(PerkTag.MOVEMENT, PerkTag.SPECIAL)
                .unlockHint("Discovered in sealed crypts and night-focused demon shrines.")
                .locationHint("Underground shrines, sealed crypts only accessible at night.")
                .tierDesc(PerkTier.UNCOMMON,  "At night: movement speed +8%. Night Vision passive.")
                .tierDesc(PerkTier.RARE,      "At night: movement speed +15%. Night Vision passive.")
                .tierDesc(PerkTier.EPIC,      "At night: movement speed +25%. Night Vision passive. Slight invisibility in shadows.")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(7).item(Items.ENDER_PEARL, 4).item(Items.PHANTOM_MEMBRANE, 2).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.PHANTOM_MEMBRANE, 6).item(Items.ENDER_EYE, 4).build())
                .build());

        register(PerkDefinition.builder("featherweight", "Featherweight")
                .description("Reduced fall damage.")
                .icon(Items.RABBIT_FOOT)
                .tiers(PerkTier.COMMON, PerkTier.EPIC)
                .tags(PerkTag.MOVEMENT)
                .unlockHint("Developed by acrobatic slayer lineages. Found in many surface caches.")
                .locationHint("High-altitude structures, cliff-edge ruins.")
                .tierDesc(PerkTier.COMMON,    "Fall damage reduced by 25%.")
                .tierDesc(PerkTier.UNCOMMON,  "Fall damage reduced by 50%.")
                .tierDesc(PerkTier.RARE,      "Fall damage reduced by 75%.")
                .tierDesc(PerkTier.EPIC,      "Fall damage immunity.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(3).item(Items.FEATHER, 8).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(6).item(Items.FEATHER, 16).item(Items.RABBIT_FOOT, 2).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(11).item(Items.RABBIT_FOOT, 4).item(Items.PHANTOM_MEMBRANE, 2).build())
                .build());

        register(PerkDefinition.builder("swift_recovery", "Swift Recovery")
                .description("Heal slightly faster when eating food.")
                .icon(Items.GOLDEN_APPLE)
                .tiers(PerkTier.COMMON, PerkTier.EPIC)
                .tags(PerkTag.SURVIVAL)
                .unlockHint("Basic slayer resilience training. Found in early-tier loot fairly often.")
                .locationHint("Medic posts, healer outpost ruins near corps supply lines.")
                .tierDesc(PerkTier.COMMON,    "Food heals 15% more HP over its duration.")
                .tierDesc(PerkTier.UNCOMMON,  "Food heals 30% more HP over its duration.")
                .tierDesc(PerkTier.RARE,      "Food heals 50% more HP. Eating speed increased by 20%.")
                .tierDesc(PerkTier.EPIC,      "Food heals 70% more HP. Eating speed increased by 40%. Saturation bonus doubled.")
                .upgradeCost(PerkTier.UNCOMMON,  PerkUpgradeCost.builder(3).item(Items.GOLDEN_CARROT, 4).build())
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(7).item(Items.GOLDEN_CARROT, 8).item(Items.GOLDEN_APPLE, 1).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(13).item(Items.GOLDEN_APPLE, 2).item(Items.DIAMOND, 1).build())
                .build());

        register(PerkDefinition.builder("silent_step", "Silent Step")
                .description("Lower enemy detection range. Mobs only focus you when you're very close.")
                .icon(Items.SLIME_BALL)
                .tiers(PerkTier.UNCOMMON, PerkTier.RARE)
                .tags(PerkTag.MOVEMENT, PerkTag.SPECIAL)
                .unlockHint("Taught by rogue slayers who prefer stealth over force.")
                .locationHint("Hidden chambers in heavily demon-patrolled ruins.")
                .tierDesc(PerkTier.UNCOMMON,  "Mob detection range halved. Mobs de-aggro faster.")
                .tierDesc(PerkTier.RARE,      "Mob detection range reduced by 75%. Mobs will not aggro you unless within 5 blocks.")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(8).item(Items.PHANTOM_MEMBRANE, 4).item(Items.SLIME_BALL, 8).build())
                .build());

        // ── Wildcard perks ─────────────────────────────────────────────────────

        register(PerkDefinition.builder("glass_cannon", "Glass Cannon")
                .description("Increased attack power but takes more damage.")
                .icon(Items.GLASS_BOTTLE)
                .tiers(PerkTier.RARE, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT, PerkTag.SPECIAL)
                .unlockHint("Not sold or gifted — only found in the most dangerous areas.")
                .locationHint("End-room loot in mid-tier demon strongholds.")
                .tierDesc(PerkTier.RARE,      "+20% damage dealt. +15% damage taken.")
                .tierDesc(PerkTier.EPIC,      "+32% damage dealt. +20% damage taken.")
                .tierDesc(PerkTier.LEGENDARY, "+45% damage dealt. +25% damage taken. Criticals deal double the bonus damage.")
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(12).item(Items.DIAMOND, 3).item(Items.BLAZE_ROD, 4).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.DIAMOND, 6).item(Items.WITHER_SKELETON_SKULL, 1).item(Items.NETHER_STAR, 1).build())
                .build());

        register(PerkDefinition.builder("juggernaut", "Juggernaut")
                .description("Reduced movement speed but higher defense.")
                .icon(Items.NETHERITE_INGOT)
                .tiers(PerkTier.RARE, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT, PerkTag.DEFENSE)
                .unlockHint("Earned by demonstrating raw strength — hidden in training pits and arena ruins.")
                .locationHint("Underground fighting pits, gladiatorial ruins.")
                .tierDesc(PerkTier.RARE,      "-10% movement speed. +15% damage resistance.")
                .tierDesc(PerkTier.EPIC,      "-7% movement speed. +25% damage resistance.")
                .tierDesc(PerkTier.LEGENDARY, "No speed penalty. +35% damage resistance. Knockback immunity.")
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(14).item(Items.NETHERITE_SCRAP, 1).item(Items.IRON_INGOT, 16).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(24).item(Items.NETHERITE_INGOT, 1).item(Items.DIAMOND, 4).item(Items.WITHER_SKELETON_SKULL, 1).build())
                .build());

        register(PerkDefinition.builder("blade_dancer", "Blade Dancer")
                .description("Slightly reduced damage per hit, but zero windup for attacks.")
                .icon(Items.GOLD_INGOT)
                .tiers(PerkTier.RARE, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT, PerkTag.MOVEMENT)
                .unlockHint("Developed by agile slayer lineages. Found among elegant, high-tier ruins.")
                .locationHint("Noble district ruins, ornate temple sub-chambers.")
                .tierDesc(PerkTier.RARE,      "-8% damage per hit. Zero attack windup.")
                .tierDesc(PerkTier.EPIC,      "-5% damage per hit. Zero attack windup. Faster combo window.")
                .tierDesc(PerkTier.LEGENDARY, "-3% damage per hit. Zero attack windup. Combo resets give a brief damage bonus.")
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(12).item(Items.FEATHER, 16).item(Items.GOLD_INGOT, 6).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(22).item(Items.PHANTOM_MEMBRANE, 4).item(Items.DIAMOND, 4).item(Items.ENDER_PEARL, 4).build())
                .build());

        register(PerkDefinition.builder("adrenaline_rush", "Adrenaline Rush")
                .description("Deal more damage the lower your HP is.")
                .icon(Items.BLAZE_ROD)
                .tiers(PerkTier.UNCOMMON, PerkTier.LEGENDARY)
                .tags(PerkTag.COMBAT, PerkTag.SURVIVAL)
                .unlockHint("A reactive warrior's perk — found where battles were frequent.")
                .locationHint("Old warfront ruins, mass-grave sites overtaken by demons.")
                .tierDesc(PerkTier.UNCOMMON,  "For every 10% HP missing, deal +2% more damage.")
                .tierDesc(PerkTier.RARE,      "For every 10% HP missing, deal +3.5% more damage.")
                .tierDesc(PerkTier.EPIC,      "For every 10% HP missing, deal +5% more damage.")
                .tierDesc(PerkTier.LEGENDARY, "For every 10% HP missing, deal +7% more damage. Below 20% HP: bonus doubles.")
                .upgradeCost(PerkTier.RARE,      PerkUpgradeCost.builder(5).item(Items.BLAZE_POWDER, 8).item(Items.SUGAR, 8).build())
                .upgradeCost(PerkTier.EPIC,      PerkUpgradeCost.builder(10).item(Items.BLAZE_ROD, 3).item(Items.GOLDEN_CARROT, 4).build())
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(18).item(Items.BLAZE_ROD, 6).item(Items.GOLDEN_APPLE, 1).item(Items.GHAST_TEAR, 1).build())
                .build());

        // ── Legendary / Special perks ──────────────────────────────────────────

        register(PerkDefinition.builder("vermilion_soul", "Vermilion Soul")
                .description("If you die, revive once per night with half HP.")
                .icon(Items.TOTEM_OF_UNDYING)
                .tiers(PerkTier.EPIC, PerkTier.LEGENDARY)
                .tags(PerkTag.SPECIAL, PerkTag.SURVIVAL)
                .unlockHint("Tied to a rare soul trait — only some slayers can awaken it.")
                .locationHint("Sealed chambers in fire-corrupted ruins, deep nether-adjacent structures.")
                .tierDesc(PerkTier.EPIC,      "Once per night: on death, revive with 50% HP. 24 hour cooldown.")
                .tierDesc(PerkTier.LEGENDARY, "Once per night: on death, revive with 50% HP and brief Resistance II. 12 hour cooldown.")
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(28).item(Items.TOTEM_OF_UNDYING, 1).item(Items.NETHER_STAR, 1).item(Items.BLAZE_ROD, 8).build())
                .build());

        register(PerkDefinition.builder("moonlit_fury", "Moonlit Fury")
                .description("At night, gain a stacking damage boost for every enemy killed (resets at dawn).")
                .icon(Items.PHANTOM_MEMBRANE)
                .tiers(PerkTier.EPIC, PerkTier.LEGENDARY)
                .tags(PerkTag.SPECIAL, PerkTag.COMBAT)
                .unlockHint("Said to only reveal itself under a blood moon. Keep watch.")
                .locationHint("Spawns during blood moon events — check unusual spots after one ends.")
                .tierDesc(PerkTier.EPIC,      "Each kill at night: +2% damage (stacks up to 15x, resets at dawn).")
                .tierDesc(PerkTier.LEGENDARY, "Each kill at night: +3% damage (stacks up to 20x, resets at dawn). Stacks glow visibly.")
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(28).item(Items.PHANTOM_MEMBRANE, 8).item(Items.DIAMOND, 6).item(Items.NETHER_STAR, 1).build())
                .build());

        register(PerkDefinition.builder("unbreakable", "Unbreakable")
                .description("Once per fight, survive a fatal blow with 1 HP. Has a cooldown.")
                .icon(Items.SHIELD)
                .tiers(PerkTier.EPIC, PerkTier.LEGENDARY)
                .tags(PerkTag.DEFENSE, PerkTag.SPECIAL)
                .unlockHint("Given only to slayers who have survived the impossible.")
                .locationHint("Deep within the most treacherous demon strongholds. Few return.")
                .tierDesc(PerkTier.EPIC,      "Once per fight: survive a fatal blow with 1 HP. 5 minute cooldown.")
                .tierDesc(PerkTier.LEGENDARY, "Once per fight: survive a fatal blow with 1 HP. 3 minute cooldown. Brief Resistance II on proc.")
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(30).item(Items.NETHERITE_INGOT, 2).item(Items.TOTEM_OF_UNDYING, 1).item(Items.NETHER_STAR, 1).build())
                .build());

        register(PerkDefinition.builder("breath_overflow", "Breath Overflow")
                .description("Occasionally cast a breathing technique without consuming breathing.")
                .icon(Items.WATER_BUCKET)
                .tiers(PerkTier.EPIC, PerkTier.LEGENDARY)
                .tags(PerkTag.BREATH, PerkTag.SPECIAL)
                .unlockHint("A physical anomaly — this scroll chooses those with exceptional breathing capacity.")
                .locationHint("Sealed meditation chambers, off the beaten path in ancient temples.")
                .tierDesc(PerkTier.EPIC,      "8% chance per breath technique use to consume no breathing.")
                .tierDesc(PerkTier.LEGENDARY, "15% chance per breath technique use to consume no breathing. Proc also regens 5% breath.")
                .upgradeCost(PerkTier.LEGENDARY, PerkUpgradeCost.builder(28).item(Items.BLAZE_ROD, 10).item(Items.GOLDEN_APPLE, 2).item(Items.NETHER_STAR, 1).build())
                .build());

        register(PerkDefinition.builder("slayers_resolve", "Slayer's Resolve")
                .description("Deal extra damage proportional to your missing HP.")
                .icon(Items.NETHER_STAR)
                .tiers(PerkTier.LEGENDARY, PerkTier.LEGENDARY)
                .tags(PerkTag.SPECIAL, PerkTag.COMBAT, PerkTag.SURVIVAL)
                .unlockHint("Cannot be found — must be earned. A certain advancement will reveal the way.")
                .locationHint("The path is the key. Push further than any slayer has before.")
                .tierDesc(PerkTier.LEGENDARY, "For every 1% HP missing, deal 0.5% more damage. At 50% HP: +25% damage. At 10% HP: +45% damage.")
                .build());

        // ── Flaws ──────────────────────────────────────────────────────────────

        registerFlaw(new FlawDefinition("brittle_bones",  "Brittle Bones",  "Your bones fracture more easily. Fall damage is doubled and knockback sends you further."));
        registerFlaw(new FlawDefinition("demon_bait",     "Demon Bait",     "Demons are innately drawn to you. You have a permanent 30% aggro increase from all demons."));
        registerFlaw(new FlawDefinition("slow_recovery",  "Slow Recovery",  "Your body heals sluggishly. Natural health regeneration is 40% slower."));
        registerFlaw(new FlawDefinition("frail_mind",     "Frail Mind",     "Mental fortitude is lacking. Stun and stagger durations are doubled."));
        registerFlaw(new FlawDefinition("heavy_feet",     "Heavy Feet",     "Your feet feel leaden. Movement speed is reduced by 15% and sprint stamina drains faster."));
        registerFlaw(new FlawDefinition("brittle_blade",  "Brittle Blade",  "Your katana chips easily. Durability drains twice as fast and attack damage is reduced by 8%."));
        registerFlaw(new FlawDefinition("bloodlust",      "Bloodlust",      "Once you land three consecutive hits you cannot stop attacking, continuing the combo involuntarily."));
        registerFlaw(new FlawDefinition("cursed_eyes",    "Cursed Eyes",    "Your vision is blighted. You have permanent Blindness I and cannot benefit from Night Vision."));
        registerFlaw(new FlawDefinition("reckless",       "Reckless",       "You lunge into every attack without caution. Dodge cooldown is increased by 30%."));
        registerFlaw(new FlawDefinition("stamina_seeker", "Stamina Seeker", "Your breath is insatiable. Stamina costs for all actions are increased by 20%."));
    }

    private static void register(PerkDefinition def) { PERKS.put(def.id, def); }
    private static void registerFlaw(FlawDefinition def) { FLAWS.put(def.id, def); }

    public static PerkDefinition getPerk(String id) { return PERKS.get(id); }
    public static FlawDefinition getFlaw(String id) { return FLAWS.get(id); }
    public static Collection<PerkDefinition> allPerks() { return Collections.unmodifiableCollection(PERKS.values()); }
    public static Collection<FlawDefinition> allFlaws() { return Collections.unmodifiableCollection(FLAWS.values()); }

    private NichirinPerkRegistry() {}
}
