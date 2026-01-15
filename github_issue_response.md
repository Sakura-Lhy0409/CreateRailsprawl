## 问题已修复 ✅

感谢您的反馈！我已经在 v1.2.4 版本中修复了车站与铁轨位置不匹配的问题。

### 修复内容

**问题根源：**
在 `StationTemplate.java` 的 `searchExit()` 方法中，计算车站出口位置的偏移量存在多余的 `+1` 操作，导致车站出口位置与铁轨不匹配。

**具体修改：**
移除了 BlockPos 偏移量计算中的三个多余 `+1`：
- X轴：`-(int) Math.floor(getWidth() / 2.0) + 1` → `-(int) Math.floor(getWidth() / 2.0)`
- Y轴：`-heightOffset + 1` → `-heightOffset`
- Z轴：`-(int) Math.floor(getDepth() / 2.0) + 1` → `-(int) Math.floor(getDepth() / 2.0)`

这个修复应该能解决您报告的车站与铁轨 xy 轴错误和高度差 ±1 的问题。

### 额外改进

同时在此版本中，我还扩展了 Create Mod 的版本兼容性：
- 现在支持 Create 5.1.0 及以上所有版本
- 向下兼容 Minecraft 1.20.1 的所有 Create 版本

### 下载

请下载最新的 v1.2.4 版本进行测试。如果问题仍然存在，请随时反馈！
