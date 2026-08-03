# Sky Island Generator

English | [中文](./README.md)

A Minecraft 1.20.1 Forge mod that adds a "Sky Island" world type. When selected during world creation, all three dimensions generate small starting platforms surrounded by void, while preserving vanilla special structures (End Portals, Ancient City portals, spawners, etc.) and normal biome distribution.

## Features

### Overworld
- Generates a 5×5×1 grass block platform at (8, 64, 8)
- An oak tree planted at the platform center
- Spawn point fixed directly above the platform center
- All vanilla structures preserved (Strongholds, End Portals, Ancient City portals, spawners, etc.)
- Ruined Portals filtered out
- Multiple biomes distributed normally

### Nether
- Generates a 5×5×3 netherrack platform at (8, 64, 8)
- Nether Fortresses only generate Blaze spawner rooms (MonsterThrone)
- Bastion Remnants generate unchanged
- Multiple biomes distributed normally

### The End
- Vanilla obsidian platform preserved (auto-generated when entering the End)
- Dragon pool (exit portal) position adjusted to "equivalent terrain" height to avoid generating in void
- Obsidian pillars (end crystals) generate normally
- End Cities generate normally
- Exit gateway generates correctly after defeating the Ender Dragon

## Technical Principles

### Core Generator Architecture
The mod core is `AbstractSkyIslandChunkGenerator`, extending `NoiseBasedChunkGenerator`:

- **Why extend**: `ChunkMap.createRandomState` checks `instanceof NoiseBasedChunkGenerator`. Only by extending this class can `RandomState` be created correctly, enabling `MultiNoiseBiomeSource` to work properly for multi-biome distribution.

- **Override strategy**:
  - `fillFromNoise`: Does not call `super`, only places platform blocks to avoid noise terrain generation
  - `buildSurface` / `applyCarvers`: Empty implementations, no surface layers or caves
  - `applyBiomeDecoration`: Overridden to only generate structure blocks, filter Ruined Portals, and specially allow obsidian pillars
  - `getBaseHeight`: Returns `platformY` (minimum 63) to prevent structures from generating at Y=-64
  - `createState`: Not overridden, uses vanilla method to ensure End Cities and other structures calculate correctly

### Dragon Pool Position Fix
Through Mixin injection into `EndDragonFight.spawnExitPortal`, sets `portalLocation` before vanilla heightmap lookup:

- Calls `getBaseHeightInEquivalentNoiseWorld` to calculate the height "if terrain existed"
- Generates the dragon pool at equivalent terrain height, not at Y=0 in void
- Referenced from CarpetSkyAdditions implementation

### Nether Fortress Modification
Through Mixin intercepting `NetherFortressStructure.findGenerationPoint`, replaces vanilla multi-component generation logic to only generate the `MonsterThrone` room containing Blaze spawners.

### World Type Registration
Through data-driven `world_preset` JSON files defining the sky island world preset, appended to the vanilla `normal` world type tag, without modifying the world creation UI code.

### Spawn Point Setting
Listens to `LevelEvent.CreateSpawnPosition` event. When the overworld uses the sky island generator, forcibly sets the spawn point to the platform center and cancels default search.

## Mixin Notes

Since Forge 1.20.1 projects do not auto-generate refmap, Mixin requires manual mapping handling between production environment (SRG mappings) and development environment (Mojang mappings):

- `@Inject`'s `method` specifies both Mojang name and SRG name
- `@Shadow` uses `aliases` attribute for SRG name
- Set `remap = false` to disable auto-mapping

## Configuration

The mod config file is located at `config/skyisland-common.toml`:

| Config | Description | Default |
|--------|-------------|---------|
| `overworld.platformX/Y/Z` | Overworld platform coordinates | 0 / 64 / 0 |
| `overworld.platformSize` | Overworld platform size | 5 |
| `nether.platformX/Y/Z` | Nether platform coordinates | 0 / 64 / 0 |
| `nether.platformSize` | Nether platform size | 5 |
| `end.preserveObsidianPlatform` | Whether to preserve End obsidian platform | true |

> **Note**: Actual platform coordinates are controlled by the `world_preset` JSON file (chunk center 8,64,8). The config file only serves as a registry declaration.

## Installation

1. Install Minecraft 1.20.1
2. Install Forge 47.x.x
3. Place `skyisland-1.0.0.jar` into the `.minecraft/mods` folder
4. Launch the game and select "Sky Island" world type when creating a new world

## Development

```bash
# Clone the repository
git clone https://github.com/19136644525lxy/Sky-Island-Generator.git
cd Sky-Island-Generator

# Build the project
./gradlew build

# Launch test client
./gradlew runClient
```

**Requirements**:
- JDK 17
- Gradle 8.8+

## Project Structure

```
src/main/java/com/skyisland/
├── SkyIslandMod.java                 # Mod main class
├── config/
│   └── SkyIslandConfig.java          # Configuration
├── core/
│   ├── registry/
│   │   └── SkyRegistries.java        # Registries
│   └── worldgen/
│       ├── AbstractSkyIslandChunkGenerator.java  # Generator base class
│       ├── OverworldSkyIslandGenerator.java      # Overworld generator
│       ├── NetherSkyIslandGenerator.java         # Nether generator
│       └── EndSkyIslandGenerator.java            # End generator
└── mixin/
    ├── MixinEndDragonFight.java      # Dragon pool position fix
    └── MixinNetherFortressStructure.java  # Nether fortress modification
```

## Acknowledgments

- [CarpetSkyAdditions](https://github.com/jsorrell/CarpetSkyAdditions) — Referenced for sky island generator implementation approach

## License

This project is licensed under the [MIT License](./LICENSE).

## Authors

- Yifei
- ZhangXaohan
