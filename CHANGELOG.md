# Changelog

## 1.3.0 - 2026-02-16

### Added
- Spawn tab now shows structure spawns
- Spawn tab now distinguishes between `Natural Biome Spawns` and `Structure Spawns`.
- Spawn tab source filters:
  - `B` (Biomes)
  - `S` (Structures)
- New common config option `spyglassDiscoveryChargeTicks` (default `28`, range `1..200`) to control how long a mob must be aimed with a spyglass before discovery triggers.
- Added localization coverage for Wildex UI/system text via language keys.
- Added `de_de` language file for German translations.
- Stats tab now shows `Hitbox Width`.
- Player UI state persistence for Wildex screen:
  - last selected tab
  - last selected mob
  - stored server-side per player/per world via world save data
- Automatic one-time config migration for `excludedModIds`:
  - on first start with `1.3.0`, Wildex adds `minecraft:giant` and `minecraft:illusioner` to the common config defaults
  - migration state is persisted in `wildex-migrations.properties`
  - migration runs only once and does not re-add entries if users remove them later by choice

### Changed
- Spawn tab headings/subheadings were refined for clearer readability.
- Migrated many hardcoded UI text literals to translatable components (`en_us`/`de_de`).
- Left mob list sorting is now locale-aware (sorted by translated mob display names).
- Stats tab now uses a clearer two-column layout with marquee labels for long localized strings.
- Info tab now uses a two-column layout (trait text + mark column) with marquee labels for long strings.
- If no persisted UI state exists for the current player/world, Wildex now falls back to defaults:
  - `Stats` tab
  - first mob entry in the list.
- Wildex config files now use a dedicated subfolder:
  - `config/wildex/wildex-common.toml`
  - `config/wildex/wildex-client.toml`
  - `config/wildex/wildex-migrations.properties`

### Fixed
- Fixed spawn tab scrollbar behavior:
  - scrollbar can be dragged with mouse
  - scrolling no longer gets visually stuck while content still overflows.
- Fixed various layout/interaction issues around filter and scroll area
- Fixed player login crash with mods registering non-instantiable entities:
  - Wildex completion total counting now guards entity instantiation with exception handling
  - problematic entities are skipped instead of crashing login/join flow.
- Fixed startup crash introduced by the new `excludedModIds` migration:
  - resolved a config reload recursion (`save -> reload -> migration`) that could end in `StackOverflowError`
  - migration execution is now reentrancy-safe and keeps one-time behavior intact.
- Hardened entity preview/data/loot paths against faulty third-party entity registrations:
  - Wildex now uses safe guarded entity creation in critical client/server paths
  - prevents runtime crashes when modded entities cannot be instantiated safely.
- Reduced server load risk from repeated client info requests:
  - added short cooldown throttling for mob loot/spawn request packets
  - added short-lived server-side runtime caching for loot/spawn responses.
- Fixed spawn info regression caused by request throttling:
  - loot and spawn requests now use separate throttle keys
  - spawn data no longer gets accidentally dropped after loot requests.
- Fixed stale UI-state carryover across session transitions:
  - player UI-state cache is now reset on login/logout before fresh server state is applied.
- Improved diagnostics for config migration marker IO:
  - read/write failures for `wildex-migrations.properties` are now logged as warnings.
- Fixed stats tab divider rendering:
  - the vertical divider now ends at the last rendered stats row instead of extending to panel bottom.

### Compatibility
- Existing worlds are supported.
- No world/save data migration required.
- Safe upgrade path from `1.2.1a`.
- Existing user config choices remain respected after migration (no forced re-adding on later starts).
- Existing Wildex config files in `config/` are migrated automatically to `config/wildex/` on first start.

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
