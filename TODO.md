# Create Railsprawl 优化任务

## 🐛 当前 Bug - RailwayBuilder 未初始化

### 问题描述
游戏中显示 "RailwayBuilder not initialized!" 错误，铁路网络不生成。

### 问题分析
**文件**：`NoiseBasedChunkGeneratorMixin.java`

可能原因：
1. Mixin 注入失败 - `@Inject` 使用了 `remap = false`，可能导致方法签名不匹配
2. 维度检查不可靠 - `effectsLocation().toString().equals("minecraft:overworld")` 可能在某些情况下失败
3. Mixin 执行时机问题 - `buildSurface` 可能不是最佳注入点

### 修复方案

#### 方案 A：修复 Mixin 注入
```java
// NoiseBasedChunkGeneratorMixin.java
// 移除 remap = false，让 Mixin 自动处理映射
@Inject(method = "buildSurface", at = @At("HEAD"))
public void surfaceStart(WorldGenRegion level, StructureManager structureManager, 
                         RandomState random, ChunkAccess chunk, CallbackInfo ci) {
    // 使用更可靠的维度检查
    if (level.dimensionType().natural()) {
        RegionPos regionPos = MyMth.regionPosFromChunkPos(chunk.getPos());
        RailwayBuilder railwayBuilder = RailwayBuilder.getInstance(level.getSeed(), level);
        railwayBuilder.generateRailway(regionPos);
    }
}
```

#### 方案 B：添加调试日志
在 `NoiseBasedChunkGeneratorMixin` 中添加日志，确认 Mixin 是否被调用：
```java
CreateRailsprawl.LOGGER.info("Mixin triggered for chunk: {}", chunk.getPos());
```

#### 方案 C：检查 Mixin 配置
确认 `createrailsprawl.mixins.json` 中的配置正确，特别是 `refmap` 是否生成。

### 已修复
修改内容（`NoiseBasedChunkGeneratorMixin.java`）：
1. ✅ 移除 `remap = false`，使用简单方法名让Mixin自动处理映射
2. ✅ 使用 `level.dimensionType().natural()` 替代字符串比较
3. ✅ 添加 `railwayBuilder != null` 空值检查
4. ✅ 添加INFO级别日志 `[Railsprawl] Mixin triggered for chunk: {}`
5. ✅ 方法改为 `private` 访问修饰符

### 待验证
- [ ] 重新构建模组：`./gradlew build`
- [ ] 在游戏中测试，查看日志是否有 `[Railsprawl] Mixin triggered` 输出
- [ ] 如果Mixin触发但铁路仍不生成，检查RailwayMap的规划逻辑

---

## ✅ 代码审查结果

所有优化任务已完成并集成到核心流程中。

### ✅ 已完成的修改

#### 1. 双向A*寻路算法
**文件**：`RailwayMap.java` 第45行
```java
// 已从 findPath 改为 findPathBidirectional
List<int[]> way = AStarPathfinder.findPathBidirectional(costMap, picStart, picEnd, ...);
```

#### 2. TerrainSamplingCache 集成
**文件**：`RoutePlanner.java`
```java
// 已添加成员变量
private final TerrainSamplingCache cache;

// 已在构造函数中初始化
public RoutePlanner(RegionPos regionPos) {
    this.regionPos = regionPos;
    this.cache = new TerrainSamplingCache();
}
```

#### 3. PathPostProcessor 集成
**文件**：`RoutePlanner.java` `getWay()` 方法
```java
// 已添加路径后处理调用
if (way != null && way.size() >= 3) {
    way = PathPostProcessor.process(way, level, cache);
}
```

### ✅ 新增工具类

| 文件 | 状态 | 说明 |
|------|------|------|
| `TerrainSamplingCache.java` | ✅ 已集成 | 地形采样缓存 |
| `PathPostProcessor.java` | ✅ 已集成 | 路径后处理（简化、松弛、桥梁拉直） |
| `BridgeDetector.java` | ✅ 已集成 | 桥梁检测（通过 PathPostProcessor 调用） |
| `AStarPathfinder.findPathBidirectional()` | ✅ 已集成 | 双向A*寻路 |

### ✅ 配置参数

| 参数 | 状态 | 说明 |
|------|------|------|
| `heuristicWeight` | ✅ 已使用 | 启发式权重 |
| `orthoStepCost` | ✅ 已使用 | 正交步进成本 |
| `diagStepCost` | ✅ 已使用 | 对角步进成本 |
| `deviationWeight` | ✅ 已使用 | 偏离权重 |
| `bridgeMinWaterDepth` | ✅ 已使用 | 桥梁最小水深 |
| `elevationWeight` | ⚠️ 待完善 | 高度权重（可进一步优化） |
| `biomeWeight` | ⚠️ 待完善 | 生物群系权重（可进一步优化） |
| `stabilityWeight` | ⚠️ 待完善 | 稳定性权重（可进一步优化） |
| `floatingWeight` | ⚠️ 待完善 | 浮空成本权重（可进一步优化） |
| `penetrationWeight` | ⚠️ 待完善 | 穿透成本权重（可进一步优化） |

---

## 🔮 后续优化建议（可选）

### 1. 完善 AStarPathfinder 成本计算
在 `findPath` 和 `findPathBidirectional` 中添加更多成本因素：
- 生物群系成本（河流、海洋区域）
- 地形稳定性成本（周围高度差）
- 浮空/穿透成本（桥梁/隧道权衡）

### 2. 添加缓存生命周期管理
在区域处理完成后清理 `TerrainSamplingCache`，避免内存泄漏。

### 3. 添加调试日志
在关键步骤添加日志输出，便于观察优化效果。

---

## 参考项目

- [RoadWeaver](https://github.com/shiroha-233/RoadWeaver) - 路径后处理、双向A*、地形缓存的参考实现
- [TongDaRailway](https://github.com/Hello-LuckyHuang/TongDaRailway) - 本项目的原始版本
