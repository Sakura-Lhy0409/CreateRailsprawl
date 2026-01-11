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

- [ ] **异步规划服务**
  - 参考 RoadWeaver 的 `planRectAsync()` 实现增量规划
  - 避免重复计算已规划的区域
  - 使用 `CompletableFuture` 改进异步处理

- [ ] **数据持久化优化**
  - 当前 NBT 存储对小规模数据足够
  - 如果铁路网络规模增大，考虑迁移到 SQLite 分片存储
  - 参考 RoadWeaver 的 `RoadShardStorage` 实现

## 参考文件

- RoadWeaver: `D:\编程项目\我的世界模组\RoadWeaver\common\src\main\java\net\shiroha233\roadweaver\runtime\`
- TongDaRailway: `D:\编程项目\我的世界模组\TongDaRailway-for-forge-main\src\main\java\com\hxzhitang\tongdarailway_for_forge\`
