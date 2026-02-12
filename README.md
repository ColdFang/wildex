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
- Clean, readable layout designed for large modpacks

### Progressive Information Tabs
Each mob entry can display:
- **Stats** (health, attributes, combat data)
- **Loot** (sampled drops with min/max ranges)
- **Spawns** (biomes and dimensions)
- **Additional info**

### Completion System
- Tracks whether a player has fully completed the bestiary
- Triggers a completion effect and unlocks "Spyglass Pulse" as a reward

### Spyglass Pulse
- **Is a Completion Reward**:
    - Unlocks once all trackable mobs are discovered
    - Highlights nearby mobs through walls
    - Has a cooldown and visual/sound feedback



---

## Multiplayer & Persistence

- All discovery, kill counts, cooldowns, and completion data are:
    - Stored server-side
    - Synced selectively to clients
- Fully multiplayer-safe
- No global progress – everything is **player-specific**

---

## Configuration

Wildex is highly configurable via standard NeoForge config files.

Available options include:
- Enable / disable Hidden Mode
- Require the Wildex book for keybind access
- Give the Wildex book on first join
- Debug discovery mode (for testing)
- Exclude specific mobs from tracking by id

---

## Integrations (FTB Quests / KubeJS)

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

Thanks to Reddit user `u/NeuroPalooza` for the integration feature suggestion.

---

## Performance & Compatibility

- No constant world scanning
- No background entity ticking
- Network traffic is request-based and minimal
- Safe for large modpacks and long-running worlds

Designed with **mod compatibility first**:
- Works with modded dimensions
- Works with modded mobs and loot tables
- Does not modify entity behavior or spawning

---

## Requirements

- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.219+
- **KubeJS bridge tested with**:
  - KubeJS `2101.7.2-build.348`
  - Rhino `2101.2.7-build.81`

---

## Status

Wildex is under active development.  
Features and UI may evolve, but save data and core systems are designed to remain stable.

---

## License

See `LICENSE.txt` for details.
