# OreToggle Fabric

Fabric mod version of my original Paper/Spigot plugin.

## Features
- Toggle ores on/off:  
  `/toggleore <ore> on/off`
- Automatic ore replacement near player
- Restore ores:  
  `/restoreore <ore>`
- JSON persistence (survives restart)
- Autosave
- Status command:  
  `/oretoggle status`

## How it works
When an ore is disabled, nearby matching blocks are replaced with:
- Stone (overworld)
- Deepslate (deepslate ores)
- Netherrack (nether ores / debris)

Replaced blocks are stored and can be restored later.

## Commands

### Toggle ore
`/toggleore <ore> on/off`
`/restoreore <ore>`

### Status
`/oretoggle status`

### Manual save
`/oretoggle save`

## Notes
- Works on Fabric 1.21.x
- No mixins used
- Designed for SMP / servers
- Rewritten from paper plugin to Fabric

## License
MIT