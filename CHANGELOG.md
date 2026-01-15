# 更新日志

## v1.2.4 (2026-01-15)

### 🐛 Bug 修复
- 修复车站与铁轨位置不匹配的问题
  - 修正 StationTemplate 中车站出口位置偏移计算
  - 移除多余的 +1 偏移量，解决 xy 轴错误和高度差 ±1 的问题

### ✨ 兼容性改进
- 扩展 Create Mod 版本兼容性
  - 支持 Create 5.1.0 及以上所有版本
  - 向下兼容 Minecraft 1.20.1 的所有 Create 版本

---

## v1.2.3 (2026-01-12)

### 🐛 Bug 修复
- 修复项目运行时的问题，确保模组可以正常运行

### 📝 文档
- 更新 README 文档，添加更详细的模组介绍和使用教程

---

## v1.2.2 (2026-01-11)

### ✨ 新特性
- 添加 SQLite 分片存储 (RailwayShardStorage)
  - 支持大规模铁路网络的高效存储
  - 按区域分片存储数据
- 添加按维度覆盖配置 (DimensionRailwaySettings)
  - 支持为不同维度设置独立的铁路生成参数
- 添加配置校验 sanitize 方法
  - 自动修复超出范围的配置值

### 🔧 性能优化
- 添加空间索引优化 (RailwaySpatialIndex)
  - 网格划分实现 O(1)~O(k) 查询复杂度
  - LRU 缓存限制内存使用
- 添加异步规划服务 (RailwayPlanningService)
  - 增量规划避免重复计算
  - Epoch 机制防止过期任务
- 添加区块生成追踪 (ChunkGenTracker)
  - 区分世界生成阶段和玩家操作阶段
- 添加 ComputeService 工具类
  - 封装异步任务提交
  - 支持 Epoch 检查

---

## v1.2.1 (2026-01-11)

### 🔧 性能优化
- 重构 RailwayBuilder 线程池管理
  - 移除自建线程池，改用 ThreadPoolManager.computeExecutor()
  - 添加 clearAll() 方法清理缓存
- 添加 Epoch 机制防止过期任务执行
  - 服务器重启后旧任务自动失效
- ThreadPoolManager 增强
  - 添加独立的生成线程池 GENERATION_EXEC
  - 支持 resizeComputePool() 和 resizeGenerationPool() 运行时调整
- CacheManager 增强
  - 添加 MAX_REGION_CACHE_SIZE 缓存大小限制
  - 添加 invalidateRailwayCache() 方法
  - 服务器停止时自动清理 RailwayBuilder 缓存

### 🙏 致谢
- 感谢 [RoadWeaver](https://github.com/shiroha-233/RoadWeaver) 项目的线程池和缓存管理方案参考

---

## v1.2.0 (2026-01-11)

### ✨ 新特性
- 新增铁路地图界面（按 N 键打开）
  - 实时显示铁路网络布局
  - 显示车站位置和名称
  - 支持缩放和拖拽
- 新增线程池管理器，优化异步任务调度
- 新增缓存管理器，提升性能

### 🔧 技术改进
- 借鉴 RoadWeaver 项目优化路径规划算法
- 改进 A* 寻路算法性能
- 优化曲线路径生成

### 🙏 致谢
- 感谢 [RoadWeaver](https://github.com/shiroha-233/RoadWeaver) 项目的参考

---

## v1.1.1 (2026-01-11)

### 🧹 代码清理
- 移除未实现的 railway_blueprint 物品及相关资源
- 移除空的 ModItems 注册类
- 精简语言文件，移除未实现功能的翻译
- 清理多余的配方和纹理文件

---

## v1.1.0 (2026-01-11)

### 🎉 正式版发布

这是 Create Railsprawl 的第一个正式版本！

### ✨ 新特性
- 完整移植 TongDaRailway 项目到 Forge 1.20.1
- 支持 Create Mod 6.0.8 的铁轨系统
- 世界生成时自动生成铁路网络
- 智能地形适应（桥梁、隧道、路基）
- 多种车站类型（地面站、地下站）

### 🔧 技术改进
- 从 NeoForge 1.21 移植到 Forge 1.20.1
- 优化 Mixin 注入点兼容性
- 异步铁路生成，提升性能

### 📝 文档
- 完善 README 文档
- 添加详细的安装和使用说明

### 🙏 致谢
- 感谢 [Hello-LuckyHuang](https://github.com/Hello-LuckyHuang/TongDaRailway) 的原项目开源贡献

---

## v1.0.4

### 协议变更
- 协议从 MIT 变更为 GPL-3.0
- 添加对 TongDaRailway 项目的致谢

### 代码优化
- 重构 RoutePlanner 和 RailwayBuilder 缓存管理
- 修复静态缓存字段引用问题

## v1.0.3

### Bug修复
- 修复世界加载卡住的问题
  - 服务器启动后前100tick内跳过被动生成
  - 确保世界完全加载后再触发铁轨生成

## v1.0.2

### Bug修复
- 修复世界加载卡在0%的问题
  - 将区块加载事件中的耗时操作改为异步处理
  - 避免玩家建筑检测阻塞主线程

## v1.0.1

### 兼容性更新
- 适配 Tectonic 地形mod
  - 自动检测 Tectonic 是否加载
  - 针对 Tectonic 陡峭地形调整采样步长
  - 提高坡度阈值以适应更大的地形起伏

## v1.0.0

### 初始版本
- 生成机械动力风格的铁路网络
- 支持被动区块加载
- 支持主动命令生成
