# Wildex

**Wildex** is an in-game bestiary for Minecraft that automatically tracks and documents all creatures you encounter - vanilla and modded alike.

Instead of relying on external wikis, Wildex builds a personal bestiary as you play: mobs are discovered through combat or observation, their information is unlocked progressively, and everything is presented inside a clean, book-style UI.

Designed to be lightweight, configurable, and compatible with heavily modded environments.

---

## Core Features

### Automatic Mob Discovery  (when Hidden Mode is true)
- Mobs are discovered by:
    - Killing them
    - Observing them with a **Spyglass**
- Supports **vanilla and modded entities**
- Excludes non-trackable or technical entities automatically
- Discovery is stored **per player, per world**

### In-Game Bestiary Book
- Dedicated Wildex book UI + item
- Searchable mob list
- Clean, readable layout

### Progressive Information Tabs
Each mob entry can display:
- **Stats** (health, attributes, combat data)
- **Loot** (sampled drops with min/max ranges)
- **Spawns** (biomes and structures)
- **Additional info**

### Completion System
- Tracks whether a player has fully completed the bestiary
- Triggers a completion effect and unlocks "Spyglass Pulse" as a reward:

    - Unlocks once all trackable mobs are discovered
    - Highlights nearby mobs through walls
    - Has a cooldown and visual/sound feedback



---

## Multiplayer & Persistence

- All discovery, kill counts, cooldowns, and completion data are:
    - Stored server-side
    - Synced selectively to clients
- Fully multiplayer-safe
- No global progress - everything is **player-specific**

### Multiplayer Sharing

Wildex includes an optional player-to-player sharing system for Hidden Mode progression.

- Players can open the share UI in the Wildex screen and send a discovered mob entry to another player.
- Targets can accept or decline offers.
- Optional payment is supported:
  - Currency item is configurable via `shareOfferCurrencyItem`.
  - Maximum allowed price is configurable via `shareOfferMaxPrice`.
  - Payment behavior can be toggled via `shareOffersPaymentEnabled`.
- Pending payouts can be claimed from the same UI.

Server and pack control:

- Sharing can be enabled/disabled globally via `shareOffersEnabled`.
- Validation and limits are enforced server-side.
- In multiplayer, clients use server-provided share settings.
- In singleplayer, sharing is intentionally limited unless debug mode is enabled.
---

## Configuration

Wildex is highly configurable via standard NeoForge config files.

Common options include:

- `hiddenMode`  
  Enables Hidden Mode progression.
- `requireBookForKeybind`  
  Requires carrying the Wildex book to use the keybind.
- `giveBookOnFirstJoin`  
  Gives the Wildex book to new players once per world.
- `spyglassDiscoveryChargeTicks`  
  Time required to discover a mob via Spyglass observation.
- `excludedModIds`  
  Excludes full namespaces or specific entity ids from tracking.

Multiplayer sharing options:

- `shareOffersEnabled`  
  Enables/disables the player-to-player sharing system.
- `shareOffersPaymentEnabled`  
  Enables optional paid share offers.
- `shareOfferCurrencyItem`  
  Currency item id used for share payments (for example `minecraft:emerald`).
- `shareOfferMaxPrice`  
  Maximum allowed offer price.

Integration/debug options:

- `kubejsBridgeEnabled`  
  Enables KubeJS bridge emits for discovery/completion events.
- `exposureDiscoveryEnabled`  
  Enables discovery support through Exposure integration.
- `debugMode`  
  Enables development/testing helpers (for example manual discovery actions in Hidden Mode).

---

## Integrations (Exposure / FTB Quests / KubeJS)

### Exposure Integration

If the Exposure mod is installed, Wildex can grant discovery progress from photo-frame interactions.

- Integration can be enabled/disabled with `exposureDiscoveryEnabled` in common config.
- Exposure-based discovery follows normal Wildex tracking and persistence rules.
- If Exposure is not installed, this integration path remains inactive.

Wildex exposes progress into the vanilla scoreboard so quest and script mods can use it directly.

### Scoreboard Objectives

- `wildex_discovered` = discovered entries (integer)
- `wildex_total` = total trackable entries (integer)
- `wildex_percent` = completion percent scaled `0..10000` (50.00% = `5000`)
- `wildex_complete` = completion flag (`0` or `1`)

These values are synced on player login and on Wildex progress updates (discovery/completion).

### FTB Quests Examples

- "Reach 50% Wildex completion" -> check `wildex_percent >= 5000`
- "Complete the Wildex" -> check `wildex_complete >= 1`

### KubeJS Notes

KubeJS can read the same objectives from player scoreboard data to gate stages, rewards, or commands.

### KubeJS Bridge Events (for addon/pack integrations)

Wildex now provides a stable bridge contract for direct KubeJS-style progress events.

- `wildex.discovery_changed`
- `wildex.completed`

Payload fields:

- `apiVersion` (`1`)
- `playerUuid` (string UUID)
- `playerName` (string)
- `mobId` (string, discovery event only; `null` for completed event)
- `discovered` (int)
- `total` (int)
- `percentScaled` (int, `0..10000`)
- `isComplete` (boolean)

Important:

- Scoreboard integration works immediately and is the recommended baseline for quests/scripts.
- Direct bridge event handling can be used from KubeJS startup scripts via the built-in Wildex script bridge.
- Bridge emits can be toggled with `kubejsBridgeEnabled` in the common config.

Example (`kubejs/startup_scripts/wildex_bridge.js`):

```js
const Bridge = Java.loadClass('de.coldfang.wildex.integration.kubejs.WildexKubeJsScriptBridge')

Bridge.onDiscoveryChanged(payload => {
  console.log(`[Wildex] Discovery: ${payload.playerName} -> ${payload.mobId} (${payload.percentScaled}/10000)`)
})

Bridge.onCompleted(payload => {
  console.log(`[Wildex] Completed: ${payload.playerName}`)
})
```

### API Surface (for addon integrations)

- `de.coldfang.wildex.api.WildexApi#getDiscoveredCount(ServerPlayer)`
- `de.coldfang.wildex.api.WildexApi#getTotalCount(ServerLevel)`
- `de.coldfang.wildex.api.WildexApi#getCompletionPercentScaled(ServerPlayer)`
- `de.coldfang.wildex.api.WildexApi#isComplete(ServerPlayer)`

### API Events

- `de.coldfang.wildex.api.event.WildexDiscoveryChangedEvent`
- `de.coldfang.wildex.api.event.WildexCompletedEvent`

---

## Requirements

- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.219+
- **KubeJS bridge tested with**:
  - KubeJS `2101.7.2-build.348`
  - Rhino `2101.2.7-build.81`
---

## Credits

- Thanks to Reddit user `u/NeuroPalooza` for the kubejs integration feature suggestion.
- Thanks to user `HockeyZman2000` for the sharing feature suggestion.
- Thanks to user `vinylwitch` for the Exposure integration suggestion.
- Thanks to `amon` for the new Wildex textures/artwork.
- Thanks to user `VaporeonScripts` for the report and testing feedback.

---

## License

See `LICENSE.txt` for details.

