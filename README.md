# Create Railsprawl (机械动力·铁轨蔓延)

[English](README_EN.md) | 简体中文

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-47.2.0+-orange.svg)](https://files.minecraftforge.net/)
[![Create](https://img.shields.io/badge/Create-0.5.1+-purple.svg)](https://www.curseforge.com/minecraft/mc-mods/create)
[![Version](https://img.shields.io/badge/Version-1.2.5-brightgreen.svg)](https://github.com/Sakura-Lhy0409/CreateRailsprawl/releases)

一个基于 Minecraft Forge 1.20.1 的模组，依赖 Create Mod 0.5.1+（支持 0.5.1j 和 6.0.x 版本）。在世界生成时自动生成机械动力风格的铁轨网络，包括桥梁、隧道和车站。

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
2. 下载并安装 [Create Mod 0.5.1+](https://www.curseforge.com/minecraft/mc-mods/create)（支持 0.5.1j 和 6.0.x 版本）
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

## 🔗 兼容性

### ✅ 已测试兼容

| 模组名称 | 兼容状态 | 备注 |
|----------|----------|------|
| [Terralith](https://www.curseforge.com/minecraft/mc-mods/terralith) | ✅ 兼容 | 已测试，可正常使用 |

### ⚠️ 其他地形模组

其他地形生成模组（如 Tectonic、Biomes O' Plenty、Oh The Biomes You'll Go 等）作者尚未测试，请自行测试兼容性：

- 如果测试后**可以兼容**，欢迎在 [Issues](https://github.com/Sakura-Lhy0409/CreateRailsprawl/issues) 中反馈，我会更新兼容列表，谢谢！
- 如果测试后**不兼容**，也请提交反馈，我会尽量进行适配

## 🤝 贡献指南

欢迎参与本项目的开发！以下是贡献指南：

### 本地开发环境搭建

1. **克隆仓库**
   ```bash
   git clone https://github.com/Sakura-Lhy0409/CreateRailsprawl.git
   cd CreateRailsprawl
   ```

2. **环境要求**
   - JDK 17 或更高版本
   - IntelliJ IDEA（推荐）或 Eclipse
   - Minecraft Forge MDK 1.20.1

3. **导入项目**
   - IntelliJ IDEA：直接打开项目文件夹，Gradle 会自动导入
   - 等待 Gradle 同步完成

4. **运行游戏**
   ```bash
   ./gradlew runClient
   ```

5. **构建 JAR**
   ```bash
   ./gradlew build
   ```
   构建产物位于 `build/libs/` 目录

### PR 提交规范

1. **分支命名**
   - 功能：`feature/功能描述`
   - 修复：`fix/问题描述`
   - 文档：`docs/文档描述`

2. **Commit 信息格式**
   ```
   <类型>: <简短描述>
   
   [可选的详细描述]
   ```
   类型包括：`feat`（新功能）、`fix`（修复）、`docs`（文档）、`refactor`（重构）、`perf`（性能优化）

3. **PR 要求**
   - 确保代码可以正常编译
   - 简要描述更改内容和原因
   - 如果是新功能，请附上测试说明

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

## 🚀 开发计划

### 计划中的功能

| 功能 | 状态 | 说明 |
|------|------|------|
| 更多路边装饰 | 🔮 计划中 | 路灯、信号灯、里程碑等 |
| 链接多种结构 | 🔮 计划中 | 铁路连接村庄、要塞等原版结构 |
| 链接群系 | 🔮 计划中 | 根据群系类型调整铁路风格 |
| 更多精美建筑 | 🔮 计划中 | 更多车站、桥梁、隧道样式 |
| 路途事件 | 🔮 计划中 | 沿途生成特殊事件或建筑 |
| 自定义链接 | 🔮 计划中 | 支持自定义起点终点 |
| 主路系统 | 🔮 计划中 | 主干线与支线的层级系统 |
| 半砖过渡 | 🔮 计划中 | 使用半砖实现更平滑的坡度过渡 |
| 贝塞尔曲线平滑 | 🔮 计划中 | 使用贝塞尔曲线优化弯道 |
| 地下铁路系统 | 🔮 计划中 | 完整的地下铁路网络支持 |
| 高架铁路 | 🔮 计划中 | 城市风格的高架轨道系统 |
| 铁路信号系统 | 🔮 计划中 | 与 Create 信号系统集成 |
| 多轨道并行 | 🔮 计划中 | 支持双轨或多轨并行铺设 |
| 自定义模板 | 🔮 计划中 | 支持玩家自定义车站/桥梁模板 |
| 铁路网络可视化 | 🔮 计划中 | 更详细的地图显示和统计 |
| 跨维度铁路 | 🔮 计划中 | 下界/末地铁路网络支持 |
| 真实高铁路网算法 | 🔮 计划中 | 优化算法使轨道路网生成更像现实的高铁路网系统 |

## ⚡ 性能说明

### 性能影响因素

- **世界地形复杂度**：复杂地形（如山脉、峡谷）会增加寻路计算量
- **寻路步长与权重配置**：较小的步长和复杂的权重配置会增加计算时间
- **道路同时生成数量**：同时规划的铁路数量越多，CPU 占用越高
- **线程池数量**：可在配置中调整，建议根据 CPU 核心数设置

### 异步架构

本模组采用双线程池架构，所有耗时操作均在后台执行，不会阻塞游戏主线程：

- **计算线程池（CRS-Compute）**：负责 A* 寻路、路网规划等计算密集型任务，默认线程数为 CPU 核心数 - 1
- **生成线程池（CRS-Gen）**：负责铁路结构的实际生成，默认 4 个线程
- **节流机制**：内置占空比控制，在耗时任务中周期性休眠，避免 CPU 占用过高导致卡顿
- **主线程**：只负责任务调度与结果应用

这意味着即使在生成大量铁路时，游戏也能保持流畅运行。

## 💖 支持作者

如果你喜欢这个模组，可以通过爱发电支持我，随缘赞助~

[![爱发电](https://img.shields.io/badge/爱发电-支持作者-ff69b4.svg)](https://afdian.com/a/LHY0409)

## 🔗 相关链接

- [GitHub 仓库](https://github.com/Sakura-Lhy0409/CreateRailsprawl)
- [原项目 TongDaRailway](https://github.com/Hello-LuckyHuang/TongDaRailway)
- [RoadWeaver](https://github.com/shiroha-233/RoadWeaver)
- [Create Mod](https://www.curseforge.com/minecraft/mc-mods/create)
- [爱发电](https://afdian.com/a/LHY0409)

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
