# Nichirin 1.0.4.1

Released: 2026-07-09

Hotfix for the 1.20.1 Forge build.

## Fixed

- Fixed the Forge release jar crashing on launch with a mixin apply error (`@Shadow method getActiveEffectsMap ... was not located in the target class`). The production jar shipped without usable obfuscation mappings for its mixins; they are now remapped correctly during the build.
- Fixed a crash when opening the world selection screen (`Failed to load registries`). Leftover worldgen data from the old wisteria tree implementation referenced foliage placers and features that no longer exist.
- Fixed a crash during mod loading when Cloth Config is not installed (`NoClassDefFoundError: me/shedaniel/autoconfig/ConfigData`). Cloth Config is now bundled inside the Forge jar, matching the Fabric build.

## Notes

- No gameplay changes. Fabric players are unaffected by the crash and do not need this update.
