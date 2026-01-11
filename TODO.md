# CreateRailsprawl 优化清单

基于 RoadWeaver 和 TongDaRailway 项目的对比分析

## 高优先级

- [x] **RailwayBuilder 线程池生命周期管理**
  - ~~当前问题：`ThreadPoolExecutor(64, 1024, 1, TimeUnit.DAYS, ...)` 参数过大~~
  - ~~添加 `shutdown()` 方法清理资源~~
  - ~~使用 `ThreadPoolManager.computeExecutor()` 替代自建线程池~~
  - ~~在 `CacheManager.onServerStopping()` 中调用清理~~

- [x] **添加 Epoch 机制防止过期任务**
  - ~~在异步任务开始时记录 `ThreadPoolManager.currentEpoch()`~~
  - ~~在任务执行中检查 `ThreadPoolManager.isEpoch(epoch)`~~
  - ~~服务器重启后旧任务自动失效~~

## 中优先级

- [x] **ThreadPoolManager 增强**
  - ~~添加独立的生成线程池 `GENERATION_EXEC`~~
  - ~~添加 `resizeGenerationPool()` 方法支持运行时调整~~
  - ~~添加 `resizeComputePool()` 方法支持运行时调整~~
  - ~~从 `RailwayConfig` 读取线程数配置~~

- [x] **CacheManager 增强**
  - ~~管理 `regionRailways` 和 `regionHeightMap` 缓存~~
  - ~~添加缓存大小限制（参考 RoadWeaver 的 `MAX_PLANNED_KEYS = 200_000`）~~
  - ~~维度卸载时清理对应区域数据~~
  - ~~添加 `invalidateRailwayCache()` 方法~~

## 低优先级

- [x] **ComputeService 工具类**
  - ~~封装 `CompletableFuture.supplyAsync()` 和 `runAsync()`~~
  - ~~统一使用 `ThreadPoolManager.computeExecutor()`~~
  - ~~简化异步任务提交代码~~

- [x] **ChunkGenTracker 区块生成追踪**
  - ~~区分世界生成阶段（WorldGenRegion）和玩家操作阶段~~
  - ~~只在区块首次生成时执行某些操作~~
  - ~~参考 RoadWeaver 的 `ChunkGenTracker` 实现~~

- [x] **异步规划服务**
  - ~~参考 RoadWeaver 的 `planRectAsync()` 实现增量规划~~
  - ~~避免重复计算已规划的区域~~
  - ~~使用 `CompletableFuture` 改进异步处理~~

- [x] **数据持久化优化**
  - ~~当前 NBT 存储对小规模数据足够~~
  - ~~如果铁路网络规模增大，考虑迁移到 SQLite 分片存储~~
  - ~~参考 RoadWeaver 的 `RoadShardStorage` 实现~~

- [x] **配置系统增强**（RoadWeaver 主要优势之一：100+ 配置项）
  - [x] ~~添加更多可配置项：~~
    - [x] ~~A* 寻路权重（orthoStepCost、diagStepCost、elevationWeight、heuristicWeight 等）~~
    - [x] ~~线程占空比 threadDutyCycle（1-100%）~~
    - [x] ~~计算线程数 computeThreads（0=自动）~~
    - [x] ~~生成线程数 generationThreads~~
    - [x] ~~最大并发生成数 maxConcurrentGenerations~~
  - [x] ~~支持按维度覆盖配置（DimensionRoadSettings）~~
  - [x] ~~添加配置校验和默认值修复（sanitize 方法）~~
  - ~~参考 RoadWeaver 的 `ModConfig` 和 `DimensionRoadSettings`~~

- [x] **空间索引优化**（RoadWeaver 主要优势之一：网格划分 + LRU 缓存）
  - ~~使用网格划分实现高效空间查询（GRID_SIZE = 8）~~
  - ~~查询复杂度从 O(n) 降低到 O(1)~O(k)~~
  - ~~添加 LRU 缓存限制内存使用（MAX_CACHED_CHUNKS_PER_DIM = 512）~~
  - ~~支持缓存失效机制（invalidateChunk）~~
  - ~~参考 RoadWeaver 的 `RoadSpatialIndex` 实现~~

## Bug 修复记录

### 2026-01-11: 模板命名空间错误
- **问题**: 所有 `railway_structure/*.json` 配置文件中的 `template` 字段使用了错误的命名空间 `tongdarailway:`
- **原因**: 从 TongDaRailway 项目移植时未更新命名空间
- **影响**: 模板加载失败，导致 Station Templates: 0, Railway Templates: 0
- **修复**: 将所有 `tongdarailway:` 替换为 `createrailsprawl:`
- **涉及文件**:
  - station/casing_station_0.json
  - station/casing_station_1.json
  - station/casing_station_cross.json
  - station/diorite_station_0.json
  - station/diorite_station_1.json
  - station/super_brick_station_0.json
  - station/super_brick_station_1.json
  - railway/large_brick_tunnel.json
  - railway/narrow_stone_bridge_a.json
  - railway/roadbed_a.json
  - railway/stone_bridge_a.json
  - railway/stone_tunnel_a.json
  - railway/stone_tunnel_b.json

## 参考文件

- RoadWeaver: `D:\编程项目\我的世界模组\RoadWeaver\common\src\main\java\net\shiroha233\roadweaver\runtime\`
- TongDaRailway: `D:\编程项目\我的世界模组\TongDaRailway-for-forge-main\src\main\java\com\hxzhitang\tongdarailway_for_forge\`
