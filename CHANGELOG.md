# Changelog

## 2.3.0 - 2026-02-26

### Added
- Added a `New` marker in the mob list for newly discovered entries that have not been opened yet.
- Added persistent per-player viewed-entry state (saved server-side).
- Added client/server sync for viewed-entry state so behavior is multiplayer-safe and server-authoritative.
- Added optional XP rewards for first-time viewed new entries:
  - `newEntryRewardsXp` (default: `true`)
  - `newEntryXpAmount` (default: `5`)
- Added taming data support in Info/Misc:
  - new `Taming Items` section
  - synchronized client/server payload + cache model for taming item IDs
  - new localized hint when an ownable mob has no detected taming item
- Added collapsible `+ / -` toggles for `Breeding Items` and `Taming Items` sections in Info/Misc.
- Added runtime cache lifecycle hooks (server start/stop + datapack reload) and per-tick breeding queue processing.
- Added an undiscovered preview rune-overlay effect (theme-tinted mystery glyph particles).
- Added a new mob preview variant toggle button (`B`/`A`) next to the preview reset button:
  - switches between adult and baby preview variants
  - visible only for discovered entries with supported baby-state switching
  - uses robust fallback checks for modded entities (Ageable + safe method probing)
- Added localized tooltips for preview variant toggle actions (show baby/show adult).
- Added a new common config option `keepCompletionAfterNewMobs` (default: `false`) to control completion behavior when new mobs are added by modpack changes.

### Changed
- Reworked runtime request caches (loot/spawns/breeding) to bounded LRU maps and centralized clear handling.
- Breeding requests are now queued and deduplicated per mob, then processed incrementally to reduce repeated extractor work.
- Breeding extraction now also computes taming items for tamable entities using interaction probes with retry confirmation.
- Loot extraction now uses state-aware sampling profiles (player-kill, on-fire, size variants, captain, sheep color, frog kill) and exposes condition profile metadata to the client.
- Loot extraction now samples entity-specific loot table keys and vanilla-like equipment drops for better parity with in-game behavior (including cases like sheep wool tables and pillager equipment/banner drops).
- Info/Misc visual styling was refined with theme-aware divider colors and improved empty-state text handling/wrapping.
- Discovery list sync is now requested independently of hidden mode so discovered-gated UI features remain correct in both modes.
- Mob preview variant selection now resets to adult when changing selected mobs.
- Progress/completion state now derives from filtered discovered counts (trackable mobs), preventing completion flag drift after config/filter changes.
- Completion status and Spyglass Pulse unlock now follow a unified server-side completion check that respects `keepCompletionAfterNewMobs`.

### Fixed
- Fixed viewed-entry clearing on selection changes triggered outside direct row clicks (new marker now clears reliably).
- Fixed stale runtime cache data across server lifecycle/datapack sync boundaries.
- Fixed potential overlap between undiscovered preview effects and the preview controls hint area.
- Fixed possible kill-counter integer overflow by clamping per-mob kill values to `Integer.MAX_VALUE`.
- Fixed hidden-mode information leak paths by validating discovery server-side for kills/loot/spawn info requests.
- Fixed potential spyglass charge residue on logout by clearing charge state immediately when a player leaves.
- Fixed KubeJS bridge callback accumulation across server/datapack reload cycles by clearing listeners on lifecycle hooks.
- Fixed loot condition tooltip clarity:
  - condition lines now start directly (no leading dash) and are capitalized
  - AND/OR logic is rendered explicitly between condition expressions
  - removed redundant explanatory hint text when only logical operators are sufficient
- Fixed loot tooltip layering so condition tooltips always render above list item icons.
- Fixed misleading conditional tagging by tightening condition-profile inference/verification against observed sampling results.
- Fixed scissor-stack underflow crash on very small window sizes by guaranteeing valid scissor pushes in the shared scissor helper.
- Fixed stale completion unlock states after mob pool growth by re-evaluating completion against current totals when `keepCompletionAfterNewMobs = false`.

### Compatibility
- Existing worlds are supported.
- No world/save data migration required.
- Safe update path from `2.2.0`.

## 2.2.0 - 2026-02-24

This update focuses on a major UI polish pass, new visual themes, and introduces breeding information in the Info/Misc tab.

### Added
- Added three new Wildex themes: `Jungle`, `Runes`, and `Steampunk`.
- Added theme-specific Wildex book item visuals (the held/inventory book now matches the selected theme).
- Added a new expandable top-right menu (`Menu`) with:
  - `Theme` button
  - `GUI Scale` toggle button
- Added a collapsible trophy panel with `+ / -` toggle control.
- Added breeding data to the Info/Misc tab, including:
  - `Ownable Entity`
  - `Breeding Items`
- Added server-authoritative breeding data requests and runtime caching for multiplayer-safe results.

### Changed
- Implemented breeding extraction using server-side interaction checks for better real gameplay accuracy.
- Updated the Misc info section layout
- Updated share/claim side buttons with smoother dock behavior and hover-aware peek/expand motion.
- Updated menu/button visual styling with stronger frame rendering for better readability.
- Improved theme support across screen textures, trophy backgrounds, and item model variants.

### Fixed
- Fixed Misc tab scrollbar visibility when content overflows
- Fixed Misc tab scrollbar mouse dragging support
- Fixed trophy tooltip behavior while collapsed (no tooltip when folded in)
- Reduced jittery feel of share/claim collapse by adding a short delayed retract

### Compatibility
- Existing worlds are supported.
- No world/save data migration required.
- Safe upgrade path from `2.1.0`.

## 2.1.0 - 2026-02-20

You probably won't notice much from this update during normal gameplay besides the new GUI scale slider, but it lays the groundwork for better GUI tweaking and much easier addition of new themes in future updates.

### Added
- Introduced a dedicated Wildex UI scale control.
- Added a reusable, centralized UI scaling/rendering path for Wildex screen elements.
- Refactored trophy rendering into a dedicated component with theme-aware asset selection.

### Changed
- Reworked Wildex screen scaling so Wildex UI sizing is driven by the internal Wildex scale flow instead of Minecraft GUI scale behavior.
- Continued screen package cleanup and renderer separation to improve maintainability and make future theme expansion easier.
- Updated claim button rendering to include the configured payment item icon.

### Fixed
- Fixed multiple Wildex UI elements that were still indirectly reacting to Minecraft GUI scale.
- Fixed inconsistent scaling across key UI content (texts, icons, list/overlay elements, buttons, and panel visuals).
- Fixed mob list scrollbar drag behavior (reduced jump/stick behavior and improved drag consistency).
- Fixed several share overlay sizing/alignment issues and kept rendering consistent with Wildex UI scaling.

### Compatibility
- Existing worlds are supported.
- No world/save data migration required.
- Safe update path from `2.0.0`.

## 2.0.0 - 2026-02-19

### Added
- New multiplayer Sharing system in Wildex:
  - share discovered entries with other online players
  - optional price per offer (configurable payment item)
  - offer accept/decline flow with clickable chat actions (`[Accept]` / `[Decline]`)
  - offer timeout (1 minute) with expiration chat notifications for both players
  - sender-side active-offer cap (max 3) plus anti-spam send cooldown.
  - Persistent per-player Accept Offers preference (saved server-side, survives relog/restart).
  - Share payouts are now queued and claimable later
  - payments are stored persistently server-side
  - new Wildex button to claim pending payouts
  - items are added to inventory or dropped if inventory is full.
- Exposure integration for discovery progression:
  - mob discovery can be triggered from Exposure photo workflow (when enabled)
  - integration remains optional and safe when Exposure is not installed.
- Server-authoritative config sync for multiplayer clients:
  - client now respects server Common config for gameplay-relevant behavior
  - prevents client-side local Common config edits from overriding server rules.

### Changed
- Replaced Wildex background textures (Vintage/Modern art refresh).
- Share Overlay and Wildex GUI received a full layout/interaction pass
- Extended GUI layout refinements for both themes, including tighter panel fitting and high GUI-scale behavior improvements.
- refined tooltips and localization.

### Fixed
- Fixed exploit path where client-local Common config changes could affect multiplayer behavior
- Fixed share dropdown regressions
- Fixed Mob Preview rotation bug after drag-release (no more sudden catch-up/reset behavior).

### Credits
- Thanks to user `HockeyZman2000` for the sharing feature suggestion.
- Thanks to user `vinylwitch` for the Exposure integration suggestion.
- Thanks to `amon` for the new Wildex textures/artwork.

### Compatibility
- Existing worlds are supported.
- No manual world/save migration required.
- Existing Wildex config migration remains one-time and reentrancy-safe.
- one time migration for common config (layout migration marker `v200`)
- Safe upgrade path from `1.3.1`.

## 1.3.1 - 2026-02-16

### Added
- Added English and German localization keys for in-game config entries (`wildex.configuration.*`).
- Mob Preview controls:
  - mouse wheel zoom
  - left-drag rotation (yaw + pitch)
  - `Controls` hint label inside the preview with tooltip help.

### Fixed
- Improved Mob Preview renderer stability for modded entities (safe preview behavior now active by default).
- Fixed preview pose issues while dragging (no unintended head/neck deformation from pitch input).
- Improved tooltip behavior during preview interaction

### Credits
- Thanks to user `VaporeonScripts` for the report and testing feedback.

### Compatibility
- Existing worlds are supported.
- No world/save data migration required.

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
