# Changelog

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
