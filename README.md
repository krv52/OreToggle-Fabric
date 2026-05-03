# OreToggle Fabric

Server-side Fabric mod for dynamically controlling ore availability.
Disable ores while a server is running, replace matching ore blocks near players, and restore tracked replacements later.

---

## Features

- Server-side ore toggles with `/toggleore <ore> on/off`
- Distance-sorted scanning around online players
- Automatic replacement for disabled ores
- Restore tracked replacements with `/restoreore <ore>`
- Persistent replaced-block storage across restarts
- Autosave and manual save support
- Configurable scan radius, scan rate, autosave interval, and tracked block limit

---

## How it works

When an ore is disabled, the server scans around each online player. Matching ore blocks are replaced with:

- Stone (overworld)
- Deepslate (deepslate ores)
- Netherrack (nether ores and ancient debris)

Replaced blocks are tracked in `config/oretoggle-replaced-blocks.json` and can be restored later.

The scanner checks closer blocks first, skips unloaded chunks, and resets its scan position when a player moves, changes world, or ore toggle state changes.

---

## Commands

### Toggle ore availability

`/toggleore <ore> on/off`

Supported ore keys:

`coal`, `iron`, `gold`, `redstone`, `lapis`, `diamond`, `emerald`, `copper`, `quartz`, `nether_gold`, `debris`

### Restore tracked replacements

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
  "blocksPerTick": 512,
  "horizontalScanRadius": 64,
  "verticalScanRadius": 32,
  "autosaveSeconds": 60,
  "maxTrackedBlocks": 0
}
```

- `blocksPerTick`: number of block positions checked per player per server tick
- `horizontalScanRadius`: horizontal block radius around each player
- `verticalScanRadius`: vertical block radius above and below each player
- `autosaveSeconds`: autosave interval for replaced-block storage
- `maxTrackedBlocks`: maximum number of stored replacements (`0` = unlimited)

The mod creates the config file on first run. Older configs using `scanRadiusChunks` are migrated by converting chunks to blocks when `horizontalScanRadius` is missing or invalid.

---

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.19.2 or newer
- Fabric API

---

## Notes

- Install on the dedicated server. Clients do not need the mod to join.
- The metadata is also valid in a development singleplayer client so integrated-server testing can load the mod.
- No client entrypoints or client mixins are registered.
- Main entrypoint: `me.krv.oretoggle.OreToggleFabric`
- Build with `./gradlew build` on Unix-like shells or `.\gradlew.bat build` on Windows PowerShell.

---

## License

MIT
