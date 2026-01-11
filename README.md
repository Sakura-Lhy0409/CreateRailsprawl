# Create Railsprawl (机械动力·铁轨蔓延)

[English](README_EN.md) | 简体中文

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.2.0+-orange.svg)](https://files.minecraftforge.net/)
[![Create](https://img.shields.io/badge/Create-6.0.8-purple.svg)](https://www.curseforge.com/minecraft/mc-mods/create)
[![Version](https://img.shields.io/badge/Version-1.2.3-brightgreen.svg)](https://github.com/Sakura-Lhy0409/CreateRailsprawl/releases)

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

## 🎮 快速开始

### 安装步骤

1. 确保已安装 **Minecraft Forge 1.20.1**（47.2.0 或更高版本）
2. 下载并安装 [Create Mod 6.0.8](https://www.curseforge.com/minecraft/mc-mods/create)
3. 下载本模组的 jar 文件
4. 将 jar 文件放入游戏的 `mods` 文件夹
5. 启动游戏即可

### 基本使用

1. **创建新世界**：模组会在世界生成时自动工作
2. **探索世界**：当你探索新区块时，铁路网络会自动生成
3. **查看地图**：按 **N 键** 打开铁路地图界面
4. **使用列车**：找到车站后，可以使用 Create Mod 的列车系统

## 🗺️ 铁路地图界面

按 **N 键** 打开铁路地图界面，你可以：

- 查看已生成的铁路网络布局
- 查看车站位置和名称
- 使用鼠标滚轮缩放地图
- 拖拽移动地图视角
- 点击车站可以传送到该位置（需要 OP 权限）

## 🏗️ 生成结构

### 桥梁类型

| 结构名称 | 说明 | 适用场景 |
|----------|------|----------|
| stone_bridge_a | 石质桥梁 | 跨越河流、峡谷 |
| narrow_stone_bridge_a | 窄石桥 | 跨越小型水域 |

### 隧道类型

| 结构名称 | 说明 | 适用场景 |
|----------|------|----------|
| stone_tunnel_a | 石质隧道 A | 穿越山脉 |
| stone_tunnel_b | 石质隧道 B | 穿越山脉（变体） |
| large_brick_tunnel | 大型砖石隧道 | 穿越大型山脉 |

### 路基类型

| 结构名称 | 说明 | 适用场景 |
|----------|------|----------|
| roadbed_a | 标准路基 | 平地铺设 |

### 车站类型

| 结构名称 | 说明 | 特点 |
|----------|------|------|
| casing_station_0/1 | 机壳风格车站 | 地面站，Create 风格 |
| casing_station_cross | 机壳十字车站 | 交叉路口车站 |
| super_brick_station_0/1 | 超级砖石车站 | 地面站，大型 |
| diorite_station_0/1 | 闪长岩地下车站 | 地下站 |

## ⚙️ 配置说明

配置文件位于：`config/createrailsprawl-common.toml`

### 主要配置项

```toml
# 是否启用轨道生成器
enableTrackSpawner = true

# 是否生成轨道生成器方块
generateTrackSpawner = true

# 使用轨道生成器(true)还是在世界生成时直接放置铁轨(false)
placeTracksUsingTrackSpawner = true

# 车站间距（区块数）
stationSpacing = 32

# 铁路生成高度范围
minRailwayHeight = 64
maxRailwayHeight = 128

# 最大坡度（度）
maxSlope = 15.0

# 是否生成桥梁
generateBridges = true

# 是否生成隧道
generateTunnels = true
```

### 维度配置

你可以为不同维度设置独立的铁路生成参数：

```toml
[dimensions.minecraft:overworld]
enabled = true
stationSpacing = 32

[dimensions.minecraft:the_nether]
enabled = false
```

## 🔧 命令参考

模组提供了 `/railsprawl` 命令用于调试和查看铁路信息（需要 OP 权限）：

| 命令 | 说明 |
|------|------|
| `/railsprawl info` | 显示模组信息，包括已加载的车站和铁路模板数量 |
| `/railsprawl region` | 显示当前所在区域的铁路状态（车站数、路线区块数等） |
| `/railsprawl goto` | 传送到最近的车站（搜索周围 3x3 区域） |

## 🔄 工作原理

### 铁路生成流程

1. **区域规划**：当玩家进入新区域时，模组会规划该区域的铁路网络
2. **车站选址**：根据地形和配置，在合适位置规划车站
3. **路线计算**：使用 A* 算法计算车站之间的最优路线
4. **地形适应**：根据地形自动选择桥梁、隧道或路基
5. **异步生成**：在后台线程生成铁路，不影响游戏性能

### 性能优化

- **异步处理**：所有耗时操作在后台线程执行
- **空间索引**：使用网格划分实现快速查询
- **缓存管理**：LRU 缓存限制内存使用
- **增量规划**：避免重复计算已规划的区域

## ❓ 常见问题

### Q: 铁路没有生成？

A: 请检查以下几点：
1. 确保 Create Mod 已正确安装
2. 检查配置文件中 `enableTrackSpawner` 是否为 `true`
3. 尝试探索更多区块，铁路需要一定范围才会生成

### Q: 游戏卡顿？

A: 模组已经过性能优化，但如果仍有卡顿：
1. 尝试增大 `stationSpacing` 配置值
2. 减少同时加载的区块数量
3. 确保分配足够的内存给游戏

### Q: 如何禁用某个维度的铁路生成？

A: 在配置文件中添加维度配置并设置 `enabled = false`

### Q: 铁路与其他模组冲突？

A: 本模组与大多数模组兼容。如果遇到问题，请在 GitHub Issues 中报告。

## 📋 更新日志

详见 [CHANGELOG.md](CHANGELOG.md)

## 🙏 致谢

本项目基于 [TongDaRailway](https://github.com/Hello-LuckyHuang/TongDaRailway) 项目移植而来。

特别感谢原作者 **Hello-LuckyHuang** 的开源贡献！原项目为 NeoForge 1.21 版本，本项目将其移植到 Forge 1.20.1 并进行了适配优化。

本项目的铁路网络地图界面和性能优化方案借鉴自 [RoadWeaver](https://github.com/shiroha-233/RoadWeaver) 项目。

特别感谢 RoadWeaver 项目的贡献者：
- **shiroha-233** - 项目作者
- 以及所有 RoadWeaver 项目的贡献者们

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
- [RoadWeaver](https://github.com/shiroha-233/RoadWeaver)
- [Create Mod](https://www.curseforge.com/minecraft/mc-mods/create)

## 🐛 Bug 反馈 & 💡 功能建议

如果使用本 MOD 时遇到问题、有优化建议或功能需求，请按照以下步骤提交反馈：

1. 进入项目的 [Issues](https://github.com/Sakura-Lhy0409/CreateRailsprawl/issues) 页面
2. 点击右上角的 **New issue** 按钮
3. 选择对应的模板（Bug 报告/功能请求），按照模板填写详细信息
4. 确认信息无误后，点击 **Submit new issue** 提交

⚠️ **注意事项**
- 提交前请先搜索已有的 Issues，避免重复反馈
- 反馈 Bug 时请务必提供：Minecraft 版本、Forge 版本、本 MOD 版本、报错日志（完整 crash-report 或 latest.log）、复现步骤
- 请勿在 Issues 中发送无关内容，违规内容将被直接关闭
