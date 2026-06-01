# Nichirin 1.0.4

Released: 2026-06-01

This release is a major gameplay and polish update for Nichirin. We've managed to fix over 130 bugs/issues and add lots of cool gameplay features.

## Highlights

- Added Beast Breathing and Mist Breathing as full playable styles.
- Added the trainer NPC framework, including dialogue and combat AI improvements.
- Added katana sheathing/unsheathing
- Overhauled combat feel across breathing styles, demons, blocking, parrying, movement, animations, and hitboxes.
- Reworked the main GUI and cooldown HUD for clearer scaling and move feedback.

## Added

- Beast Breathing moves, unlock progression, katanas, and armor
- Mist Breathing moves, unlock progression, katana assets, clone visuals, and an AWESOME Obscuring Clouds ultimate.
- Breathing trainer entities for Thunder and Water breathing, with dialogue screens and smarter attack behavior.
- New character assets and equipment for Inosuke, Muichiro, Jigoro, Sabito, Giyu, and Urokodaki.
- Demon move icons, improved demon movement abilities, blood management updates, and night vision behavior.
- Server-side JSON configuration support and additional gameplay config options.
- Urokodaki spawns in his house.
- New recipes, spawn eggs, language entries, animations, sounds, and visual assets.

## Changed

- Rebalanced damage, breath costs, stamina regeneration, cooldowns, windups, hitbox sizes, knockback, and movement across the whole mod.
- Reduced attack damage globally during the combat rebalance pass.
- Unified attack execution and animation handling so default katana attacks follow the same systems as breathing moves.
- Improved dash and teleport movement to reduce snapping, block clipping, and inconsistent hit registration.
- Improved hopping attacks so they drag targets more consistently.
- Smoke bomb and flash bomb can now be thrown out of dispensers, creating cool and unique traps.
- Reworked player animations, including multiplayer visibility, interruption behavior, and first-person presentation.
- Improved NPC systems to support moveset-capable trainers and more deliberate combat AI.
- Improved GUI scaling, cooldown icons, combo display behavior, and skills-page navigation.
- Centralized commands and cleaned up registries, move definitions, embedded library files, and stale project metadata.
- Updated Rengoku armor to look cooler/more polished.

## Fixed

- Fixed Water Breathing issues affecting Water Wheel, Whirlpool, Flowing Dance, Drop Ripple Thrust, Dead Calm, Waterfall Basin, and style selection.
- Fixed Flame Breathing issues affecting Flame Tiger, Rengoku, Blooming Flame Undulation, burning damage, and dash direction.
- Fixed Insect Breathing hitbox spacing and directional control for Butterfly, Centipede, and Dragonfly.
- Fixed Sound and Thunder Breathing damage, hitboxes, knockback, windups, and lingering hit timing.
- Fixed attack interruption, parry stun, combo reset, animation stutter, and invalid animation playback when attacks fail to execute.
- Fixed demon blood loss, creative and peaceful-mode behavior, regeneration stalls, move-wheel state, and delayed inputs.
- Fixed config storage, Forge compatibility regressions, cape rendering, GUI scaling, duplicate cooldowns, missing language entries

## Known Issues

- [#8](https://github.com/XircTwerk/nichirin/issues/8): GUI layout still needs additional cleanup.
- [#46](https://github.com/XircTwerk/nichirin/issues/46): M2 inputs can open trainer dialogue during combat.
- [#93](https://github.com/XircTwerk/nichirin/issues/93): Attacks can consume armor durability too quickly.
- [#136](https://github.com/XircTwerk/nichirin/issues/136): Jumping moves do not currently multihit.

## Contributors

Thanks you Mika and Bob for the assets, and thank you Nacho for doing some extra balancing.

## Full History

- [Compare `1.0.3` starting point to `1.0.4`](https://github.com/XircTwerk/nichirin/compare/f6842bc873b3797f1d71b9769449a3da5b940fb6...9a6aafc96108150370fee1c63403117056898ea8)
- [Resolved and open issues](https://github.com/XircTwerk/nichirin/issues)
