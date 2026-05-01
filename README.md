# OreToggle Fabric

Server-side Fabric mod for dynamically controlling ore availability.

Originally based on my Paper/Spigot plugin, now fully rewritten for Fabric.

---

## Features

- Toggle ores on/off:
  `/toggleore <ore> on/off`
- Fast distance-based ore scanning (closest blocks first)
- Automatic ore replacement near players
- Restore replaced ores:
  `/restoreore <ore>`
- JSON persistence (survives restart)
- Autosave system
- Status command:
  `/oretoggle status`
- Configurable performance and behavior

---

## How it works

When an ore is disabled, nearby matching blocks are replaced with:

- Stone (overworld)
- Deepslate (deepslate ores)
- Netherrack (nether ores / ancient debris)

Replaced blocks are tracked and can be restored later.

The scanner prioritizes blocks closest to the player and updates dynamically as the player moves.

---

## Commands

### Toggle ore

`/toggleore <ore> on/off`


### Restore ore

`/restoreore <ore>`


### Status

`/oretoggle status`


### Manual save

`/oretoggle save`

---

## Config

Config file:

`config/oretoggle-config.json`


Default values:
```json
{
  "blocksPerTick": 2048,
  "scanRadiusChunks": 2,
  "autosaveSeconds": 60,
  "maxTrackedBlocks": 0
}
```
- blocksPerTick – how many blocks are scanned per tick
- scanRadiusChunks – scan radius around player
- autosaveSeconds – autosave interval
- maxTrackedBlocks – limit for stored blocks (0 = unlimited)

---

## Requirements
- Minecraft 1.21.11
- Fabric Loader
- Fabric API
---
## Notes
- Server-side only (clients do NOT need the mod)
- No mixins used
- Designed for SMP / server use
- Behavior may differ from older versions (scanner rewritten)

---

## License
MIT