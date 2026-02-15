# Changelog

## 1.2.1a - 2026-02-15

### Fixed
- Fixed client join disconnect (`missing channel`) on dedicated servers:
  - Wildex clientbound payload channels are now advertised correctly during handshake on dedicated server runtime.
  - Prevents kicks like "channel is missing on the server side, but required on the client".

### Compatibility
- Existing worlds are supported.
- No data migration required.
- Safe update from `1.2.1`.

## 1.2.1 - 2026-02-15

### Fixed
- Fixed dedicated server startup crash caused by client-only classloading during mod initialization.
- Fixed additional dedicated server crash path during item registration
- Split client payload handlers from common network registration so client-only classes are no longer loaded on server.
- Hardened client bootstrap registration

### Compatibility
- Existing worlds are supported.
- No data migration required.
- Safe update from `1.2.0` for singleplayer and dedicated servers.

## 1.2.0 - 2026-02-13

### Added
- Spyglass known-mob overlay in world-space: when aiming at a mob with the spyglass, Wildex can show a floating name label.
- New client config toggle `showDiscoveredSpyglassOverlay` (default `true`).

### Changed
- Improved Ender Dragon model rendering in Wildex mob preview.
- Improved Ender Dragon model rendering in discovery toast
- Spyglass overlay behavior works in both modes:
  - Hidden Mode `true`: only for already discovered mobs.
  - Hidden Mode `false`: for targeted mobs in general.

### Fixed
- Fixed inconsistent progress values after uninstalling mods that added mobs:
  - removed/invalid mob IDs are no longer counted as discovered
  - prevents states like `discovered > total` (for example `100/90`).
- Fixed dimension-separated Wildex progress:
  - discovery, kills and cooldown data are now stored world-wide instead of per dimension
  - Wildex progress no longer appears reset when switching between Overworld/Nether/End.

### Compatibility
- Existing worlds are supported.
- No manual data migration required.
- Existing Wildex progress remains intact; only invalid discovery entries from removed mob mods are ignored/cleaned automatically.
- Automatic one-time migration merges existing per-dimension Wildex data into the new world-wide storage format.

## 1.1.1 - 2026-02-12

### Fixed
- Added `wildex:wildex_book` to the vanilla `Tools & Utilities` creative tab so JEI can list the item and show its crafting recipe reliably.


## 1.1.0 - 2026-02-12

### Added
- New integration surface for quest/script/modpack workflows (API, events, scoreboard sync, KubeJS bridge).
- Full technical details and usage examples are documented in `README.md` (see "Integrations").
- Thanks to Reddit user `u/NeuroPalooza` for the integration feature suggestion.


### Changed
- Unifid progress calculation via a central progress service used by API and scoreboard sync.
- Scoreboard sync now runs on relevant progress hooks:
  - player login
  - mob discovery
  - completion unlock
- Mob index filtering now uses unified Wildex trackable-filter logic, matching completion/API behavior.
- Completion total cache now refreshes correctly when `excludedModIds` config changes.
- Wildex GUI now shows the running mod version (small label near the Theme button) 
- Theme button visuals were restyyled to match the Wildex UI look
- General cleanup pass across UI/network/world classes
- Added config toggle `kubejsBridgeEnabled` (default `true`) for bridge emits.

### Compatibility
- Existing worlds are supported.
- No data migration required.
- Existing Wildex discovery/kill/coldown/completion progress is preserved.
- KubeJS bridge tested with:
  - KubeJS `2101.7.2-build.348`
  - Rhino `2101.2.7-build.81`
