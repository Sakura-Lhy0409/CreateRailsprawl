# Create Railsprawl

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.2.0+-orange.svg)](https://files.minecraftforge.net/)
[![Create](https://img.shields.io/badge/Create-6.0.8-purple.svg)](https://www.curseforge.com/minecraft/mc-mods/create)

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

### 🏗️ Generated Structures

| Type | Structure Name | Description |
|------|----------------|-------------|
| Bridge | stone_bridge_a | Stone bridge |
| Bridge | narrow_stone_bridge_a | Narrow stone bridge |
| Tunnel | stone_tunnel_a/b | Stone tunnel |
| Tunnel | large_brick_tunnel | Large brick tunnel |
| Roadbed | roadbed_a | Standard roadbed |
| Station | casing_station | Casing-style station |
| Station | super_brick_station | Super brick station |
| Station | diorite_station | Diorite underground station |

## 📦 Installation

### Requirements

- Minecraft 1.20.1
- Forge 47.2.0 or higher
- Create Mod 6.0.8

### Installation Steps

1. Make sure Minecraft Forge 1.20.1 is installed
2. Download and install [Create Mod 6.0.8](https://www.curseforge.com/minecraft/mc-mods/create)
3. Download this mod's jar file
4. Place the jar file in the game's `mods` folder
5. Launch the game

## 🎮 Usage

### Automatic Generation

The mod works automatically during world generation, no action required. Railway networks will automatically generate as you explore new chunks.

### Configuration

Configuration file location: `config/createrailsprawl-common.toml`

Main configuration options:

```toml
# Railway generation probability (0.0-1.0)
railwayChance = 0.3

# Station generation probability (0.0-1.0)
stationChance = 0.1

# Maximum railway length (chunks)
maxRailwayLength = 64

# Enable bridge generation
enableBridges = true

# Enable tunnel generation
enableTunnels = true
```

### Commands

The mod provides `/railsprawl` command for debugging and viewing railway information (requires OP permission):

| Command | Description |
|---------|-------------|
| `/railsprawl info` | Display mod info, including loaded station and railway template counts |
| `/railsprawl region` | Display railway status in current region (station count, route chunks, etc.) |
| `/railsprawl goto` | Teleport to nearest station (searches surrounding 3x3 regions) |

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

## 🐛 Bug Reports

If you find any bugs or have feature suggestions, please submit them on [GitHub Issues](https://github.com/Sakura-Lhy0409/CreateRailsprawl/issues).
