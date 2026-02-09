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

- **Minecraft**: 1.21.2
- **NeoForge**: 21.2.1-beta+

---

## Status

Wildex is under active development.  
Features and UI may evolve, but save data and core systems are designed to remain stable.

---

## License

See `LICENSE.txt` for details.
