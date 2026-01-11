# Create Railsprawl (机械动力·铁轨蔓延)

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.2.0+-orange.svg)](https://files.minecraftforge.net/)
[![Create](https://img.shields.io/badge/Create-6.0.8-purple.svg)](https://www.curseforge.com/minecraft/mc-mods/create)

一个基于 Minecraft Forge 1.20.1 的模组，依赖 Create Mod 6.0.8。在世界生成时自动生成机械动力风格的铁轨网络，包括桥梁、隧道和车站。

## 📖 模组介绍

Create Railsprawl 是一个世界生成增强模组，它会在你探索世界时自动生成一套完整的铁路网络系统。这些铁路完全兼容 Create Mod 的列车系统，让你的世界从一开始就拥有便捷的交通网络。

### ✨ 主要特性

- **自动铁路生成**：在世界生成时自动规划并建造铁路网络
- **智能地形适应**：
  - 遇到山脉自动挖掘隧道
  - 跨越河谷自动建造桥梁
  - 平地铺设标准路基
- **车站系统**：自动在合适位置生成车站，包括地面站和地下站
- **铁路地图界面**：按 N 键打开地图，实时查看铁路网络布局
- **Create Mod 完全兼容**：生成的铁轨可直接使用 Create Mod 的列车
- **多种建筑风格**：包含多种隧道、桥梁和车站的建筑样式
- **性能优化**：异步生成，线程池管理，不影响游戏流畅度

### 🏗️ 生成结构

| 类型 | 结构名称 | 说明 |
|------|----------|------|
| 桥梁 | stone_bridge_a | 石质桥梁 |
| 桥梁 | narrow_stone_bridge_a | 窄石桥 |
| 隧道 | stone_tunnel_a/b | 石质隧道 |
| 隧道 | large_brick_tunnel | 大型砖石隧道 |
| 路基 | roadbed_a | 标准路基 |
| 车站 | casing_station | 机壳风格车站 |
| 车站 | super_brick_station | 超级砖石车站 |
| 车站 | diorite_station | 闪长岩地下车站 |

## 📦 安装方法

### 前置要求

- Minecraft 1.20.1
- Forge 47.2.0 或更高版本
- Create Mod 6.0.8

### 安装步骤

1. 确保已安装 Minecraft Forge 1.20.1
2. 下载并安装 [Create Mod 6.0.8](https://www.curseforge.com/minecraft/mc-mods/create)
3. 下载本模组的 jar 文件
4. 将 jar 文件放入游戏的 `mods` 文件夹
5. 启动游戏即可

## 🎮 使用方法

### 自动生成

模组会在世界生成时自动工作，无需任何操作。当你探索新区块时，铁路网络会自动生成。

### 配置文件

配置文件位于：`config/createrailsprawl-common.toml`

主要配置项：

```toml
# 铁路生成概率 (0.0-1.0)
railwayChance = 0.3

# 车站生成概率 (0.0-1.0)
stationChance = 0.1

# 最大铁路长度（区块）
maxRailwayLength = 64

# 是否生成桥梁
enableBridges = true

# 是否生成隧道
enableTunnels = true
```

### 命令

模组提供了 `/railsprawl` 命令用于调试和查看铁路信息（需要 OP 权限）：

| 命令 | 说明 |
|------|------|
| `/railsprawl info` | 显示模组信息，包括已加载的车站和铁路模板数量 |
| `/railsprawl region` | 显示当前所在区域的铁路状态（车站数、路线区块数等） |
| `/railsprawl goto` | 传送到最近的车站（搜索周围 3x3 区域） |

## 📋 更新日志

详见 [CHANGELOG.md](CHANGELOG.md)

## 🙏 致谢

本项目基于 [TongDaRailway](https://github.com/Hello-LuckyHuang/TongDaRailway) 项目移植而来。

特别感谢原作者 **Hello-LuckyHuang** 的开源贡献！原项目为 NeoForge 1.21 版本，本项目将其移植到 Forge 1.20.1 并进行了适配优化。

根据原项目的 GPL-3.0 开源协议要求，本项目同样采用 GPL-3.0 协议发布。

## 📜 开源协议

本项目采用 [GNU General Public License v3.0](LICENSE) 开源协议。

```
Copyright (C) 2024-2026 Sakura-Lhy0409

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

## 🔗 相关链接

- [GitHub 仓库](https://github.com/Sakura-Lhy0409/CreateRailsprawl)
- [原项目 TongDaRailway](https://github.com/Hello-LuckyHuang/TongDaRailway)
- [Create Mod](https://www.curseforge.com/minecraft/mc-mods/create)

## 🐛 问题反馈

如果你发现任何 Bug 或有功能建议，请在 [GitHub Issues](https://github.com/Sakura-Lhy0409/CreateRailsprawl/issues) 中提交。
