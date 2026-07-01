# Nichirin 1.0.5

Released: 2026-07-01

This release ports Nichirin to Minecraft 1.21.1, moves the Forge build to NeoForge, and adds a major round of combat, animation, visual, weapon, config, and stability work.

## Highlights

- Ported the mod to Minecraft 1.21.1.
- Replaced the Forge build with NeoForge.
- Added Genya's Double-Barrel as a new shotgun weapon with two-shell firing, reload behavior, hitscan pellets, spread, recoil, screen feedback, and distance falloff.
- Added Genya Double-Barrel moves, including Gun Bash and Grab.
- Added a new aura and cel-shaded outline system for equipped styles.
- Added afterimages for fast movement attacks.
- Reworked blocking into a hold-to-guard system with block-based follow-up attacks.
- Added stronger demon survival mechanics, including blood bars, blood loss visuals, regeneration feedback, sunlight danger, and food-based blood recovery.
- Reworked Thunderclap Flash into a charge-and-release dash attack and added Godspeed.

## Added

- Genya's Double-Barrel weapon, animations, item state handling, gun input flow, gun animation sync packet, and firing/reload presentation.
- Gun Bash, a fast dash into a heavy-stun gun strike.
- Grab, allowing point-blank shotgun follow-ups before launching the target.
- Passive aura rendering with style-colored visual identity.
- Cel-shaded outlines that match the equipped style.
- Afterimage visuals for high-speed movement.
- Slammed status effect with slowed movement and screen wobble during heavy slam hitstun.
- Custom death messages for mod damage sources.
- Movement icons for air dodge, backstep, dash, and dodge.
- Giyu's katana.
- Updated Flame and Insect katana models/textures with sheath support.
- Updated Sabito, Muichiro, Rengoku, Tengen, Jigoro, and Urokodaki visual assets.
- `/nichirin breathing set <player> random`.
- Dedicated gun equip and swing-suppression client mixins.

## Changed

- Updated the build to Minecraft 1.21.1 and modernized the loader dependency set for Fabric and NeoForge.
- Updated Fabric Loader, NeoForge, AzureLib, Shadow, Guava, JOML, and Lombok.
- Reworked blocking so right-click guards, while previous right-click attacks are now accessed through block + left-click.
- Improved parry tuning and guard animation recovery.
- Made hit reactions more consistent across combat.
- Tuned overall combat balance, including attack damage, hitstun, recovery, cooldowns, and combo behavior.
- Moved attack sound playback to the post-windup timing instead of playing immediately on input.
- Right-click now cancels the active breathing attack.
- Improved combo scaling so supported attacks can scale damage and hitstun more consistently through shared execution logic.
- Added shared attack infrastructure for common timing, hitbox, interrupt, stun, movement, and self-ticking behavior.
- Nichirin katana, demon, and related combat damage now wears armor at a reduced rate instead of leaving armor effectively untouched.
- Improved aura and outline rendering polish.
- Improved katana holder behavior, blocking input, combo tracking, movement handling, NPC behavior paths, and projectile handling.
- Updated the mod icon.

## Fixed

- Fixed several porting issues from the Minecraft 1.21.1 update.
- Fixed several NeoForge compatibility issues.
- Fixed trainers randomly stopping damage intake during duels.
- Fixed trainer dialogue failing to open or rendering incorrectly.
- Fixed armor rendering in the inventory screen while in first person.
- Fixed Striking Tide and Constant Resounding Slashes hitting at the wrong speed.
- Fixed Shifting Flow Slash velocity not applying correctly to players.
- Fixed GUI and player model lighting issues.
- Fixed armor durability being effectively invincible against Nichirin combat damage.
- Fixed block message text rendering incorrectly.
- Fixed several attack interruption, cooldown, combo, hitbox, and input reliability issues.
- Fixed several NPC, projectile, and movement cleanup issues.

## Known Issues

- Some archived beta content remains registered internally for later development, but is not intended to be obtainable through normal survival progression in this release.
- Please report any Fabric or NeoForge runtime issues at the issue tracker.

## Contributors

Thank you to everyone who helped test the 1.21.1 beta builds and report issues during the port.

## Full History

- [Resolved and open issues](https://github.com/XircTwerk/nichirin/issues)
