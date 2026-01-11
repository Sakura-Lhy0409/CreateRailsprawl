# Create Railsprawl

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.2.0+-orange.svg)](https://files.minecraftforge.net/)
[![Create](https://img.shields.io/badge/Create-6.0.8-purple.svg)](https://www.curseforge.com/minecraft/mc-mods/create)
[![Version](https://img.shields.io/badge/Version-1.2.3-brightgreen.svg)](https://github.com/Sakura-Lhy0409/CreateRailsprawl/releases)

English | [简体中文](README.md)

A Minecraft Forge 1.20.1 mod that depends on Create Mod 6.0.8. Automatically generates Create-style railway networks during world generation, including bridges, tunnels, and stations.

## 📖 Introduction

Create Railsprawl is a world generation enhancement mod that automatically generates a complete railway network system as you explore the world. These railways are fully compatible with Create Mod's train system, giving your world a convenient transportation network from the start.

### ✨ Key Features

- **Automatic Railway Generation**: Automatically plans and builds railway networks during world generation
- **Smart Terrain Adaptation**:
  - Automatically digs tunnels through mountains
  - Automatically builds bridges across valleys
  - Lays standard roadbed on flat terrain
- **Station System**: Automatically generates stations at suitable locations, including surface and underground stations
- **Railway Map Interface**: Press N key to open the map and view the railway network layout in real-time
- **Full Create Mod Compatibility**: Generated tracks can be used directly with Create Mod trains
- **Multiple Building Styles**: Includes various tunnel, bridge, and station architectural styles
- **Performance Optimized**: Async generation with thread pool management, no impact on game smoothness

## 🎮 Quick Start

### Installation Steps

1. Make sure **Minecraft Forge 1.20.1** (47.2.0 or higher) is installed
2. Download and install [Create Mod 6.0.8](https://www.curseforge.com/minecraft/mc-mods/create)
3. Download this mod's jar file
4. Place the jar file in the game's `mods` folder
5. Launch the game

### Basic Usage

1. **Create a new world**: The mod works automatically during world generation
2. **Explore the world**: Railway networks will automatically generate as you explore new chunks
3. **View the map**: Press **N key** to open the railway map interface
4. **Use trains**: After finding a station, you can use Create Mod's train system

## 🗺️ Railway Map Interface

Press **N key** to open the railway map interface, where you can:

- View the generated railway network layout
- View station positions and names
- Use mouse wheel to zoom the map
- Drag to move the map view
- Click on a station to teleport to that location (requires OP permission)

## 🏗️ Generated Structures

### Bridge Types

| Structure Name | Description | Use Case |
|----------------|-------------|----------|
| stone_bridge_a | Stone bridge | Crossing rivers, canyons |
| narrow_stone_bridge_a | Narrow stone bridge | Crossing small water bodies |

### Tunnel Types

| Structure Name | Description | Use Case |
|----------------|-------------|----------|
| stone_tunnel_a | Stone tunnel A | Through mountains |
| stone_tunnel_b | Stone tunnel B | Through mountains (variant) |
| large_brick_tunnel | Large brick tunnel | Through large mountains |

### Roadbed Types

| Structure Name | Description | Use Case |
|----------------|-------------|----------|
| roadbed_a | Standard roadbed | Flat terrain |

### Station Types

| Structure Name | Description | Features |
|----------------|-------------|----------|
| casing_station_0/1 | Casing-style station | Surface station, Create style |
| casing_station_cross | Casing cross station | Intersection station |
| super_brick_station_0/1 | Super brick station | Surface station, large |
| diorite_station_0/1 | Diorite underground station | Underground station |

## ⚙️ Configuration

Configuration file location: `config/createrailsprawl-common.toml`

### Main Configuration Options

```toml
# Enable track spawner
enableTrackSpawner = true

# Generate track spawner blocks
generateTrackSpawner = true

# Use track spawner (true) or place tracks during world generation (false)
placeTracksUsingTrackSpawner = true

# Station spacing (in chunks)
stationSpacing = 32

# Railway generation height range
minRailwayHeight = 64
maxRailwayHeight = 128

# Maximum slope (degrees)
maxSlope = 15.0

# Generate bridges
generateBridges = true

# Generate tunnels
generateTunnels = true
```

### Dimension Configuration

You can set independent railway generation parameters for different dimensions:

```toml
[dimensions.minecraft:overworld]
enabled = true
stationSpacing = 32

[dimensions.minecraft:the_nether]
enabled = false
```

## 🔧 Command Reference

The mod provides `/railsprawl` command for debugging and viewing railway information (requires OP permission):

| Command | Description |
|---------|-------------|
| `/railsprawl info` | Display mod info, including loaded station and railway template counts |
| `/railsprawl region` | Display railway status in current region (station count, route chunks, etc.) |
| `/railsprawl goto` | Teleport to nearest station (searches surrounding 3x3 regions) |

## 🔄 How It Works

### Railway Generation Process

1. **Region Planning**: When a player enters a new region, the mod plans the railway network for that region
2. **Station Placement**: Plans stations at suitable locations based on terrain and configuration
3. **Route Calculation**: Uses A* algorithm to calculate optimal routes between stations
4. **Terrain Adaptation**: Automatically selects bridges, tunnels, or roadbed based on terrain
5. **Async Generation**: Generates railways in background threads without affecting game performance

### Performance Optimization

- **Async Processing**: All time-consuming operations execute in background threads
- **Spatial Index**: Uses grid partitioning for fast queries
- **Cache Management**: LRU cache limits memory usage
- **Incremental Planning**: Avoids recalculating already planned regions

## ❓ FAQ

### Q: Railways not generating?

A: Please check the following:
1. Make sure Create Mod is properly installed
2. Check if `enableTrackSpawner` is `true` in the config file
3. Try exploring more chunks, railways need a certain range to generate

### Q: Game lag?

A: The mod has been performance optimized, but if you still experience lag:
1. Try increasing the `stationSpacing` config value
2. Reduce the number of simultaneously loaded chunks
3. Make sure enough memory is allocated to the game

### Q: How to disable railway generation in a specific dimension?

A: Add dimension configuration in the config file and set `enabled = false`

### Q: Railway conflicts with other mods?

A: This mod is compatible with most mods. If you encounter issues, please report on GitHub Issues.

## 📋 Changelog

See [CHANGELOG.md](CHANGELOG.md)

## 🙏 Credits

This project is ported from [TongDaRailway](https://github.com/Hello-LuckyHuang/TongDaRailway).

Special thanks to the original author **Hello-LuckyHuang** for the open source contribution! The original project was for NeoForge 1.21, this project ports it to Forge 1.20.1 with adaptations and optimizations.

The railway network map interface and performance optimization solutions are inspired by [RoadWeaver](https://github.com/shiroha-233/RoadWeaver).

Special thanks to RoadWeaver project contributors:
- **shiroha-233** - Project author
- And all contributors to the RoadWeaver project

Following the GPL-3.0 license requirements of the original projects, this project is also released under GPL-3.0.

## 📜 License

This project is licensed under [GNU General Public License v3.0](LICENSE).

```
Copyright (C) 2024-2026 Sakura-Lhy0409

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

## 🔗 Links

- [GitHub Repository](https://github.com/Sakura-Lhy0409/CreateRailsprawl)
- [Original TongDaRailway](https://github.com/Hello-LuckyHuang/TongDaRailway)
- [RoadWeaver](https://github.com/shiroha-233/RoadWeaver)
- [Create Mod](https://www.curseforge.com/minecraft/mc-mods/create)

## 🐛 Bug Reports & 💡 Feature Requests

If you encounter issues, have optimization suggestions, or feature requests while using this MOD, please follow these steps to submit feedback:

1. Go to the project's [Issues](https://github.com/Sakura-Lhy0409/CreateRailsprawl/issues) page
2. Click the **New issue** button in the top right corner
3. Select the appropriate template (Bug Report/Feature Request) and fill in the detailed information
4. After confirming the information is correct, click **Submit new issue**

⚠️ **Important Notes**
- Please search existing Issues before submitting to avoid duplicate reports
- When reporting bugs, please provide: Minecraft version, Forge version, this MOD version, error logs (complete crash-report or latest.log), and reproduction steps
- Please do not post irrelevant content in Issues, such content will be closed directly
