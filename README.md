# Create Railsprawl (机械动力·铁轨蔓延)

Minecraft Forge 1.20.1 模组，依赖 Create Mod 6.0.8。自动生成机械动力风格的铁轨网络。

- 版本：1.0.0
- 作者：Sakura-Lhy0409
- 协议：MIT License

## 功能

- 自动生成 Create Mod 风格铁轨网络
- 智能地形适应（桥梁、隧道）
- 自动生成站台、路灯等附属结构
- 保护玩家建筑
- 支持主世界/下界/末地
- 异步任务队列，不卡服
- 自动备份与回滚
- 支持领地插件

## 安装

1. 安装 Minecraft Forge 1.20.1
2. 安装 Create Mod 6.0.8
3. 将 `createrailsprawl-1.20.1-1.0.0.jar` 放入 `mods` 文件夹

## 命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/railway generate <半径>` | 玩家 | 生成铁轨网络 |
| `/railway remove <半径>` | OP | 删除铁轨数据 |
| `/railway reload` | OP | 重载配置 |
| `/railway stats` | 玩家 | 显示统计信息 |
| `/railway rollback <半径>` | OP | 回滚区块 |
| `/railway emergency stop` | OP | 紧急停止 |
| `/railway emergency resume` | OP | 恢复任务 |
| `/railway debug` | OP | 切换调试模式 |

## 配置

配置文件：`config/railwaymod-common.toml`

```toml
[generation]
maxGenerationRadius = 20      # 最大生成半径（区块）
lampInterval = 20             # 路灯间隔
bridgePillarInterval = 6      # 桥墩间隔
structureAvoidDistance = 4    # 建筑避让距离
stationWidth = 3              # 站台宽度
stationLength = 5             # 站台长度

[performance]
chunksPerBatch = 2            # 每批处理区块数
batchIntervalMs = 200         # 批次间隔(ms)
maxGenerationsPerPlayer = 3   # 每玩家10分钟内最大生成次数
minTpsForPassiveGeneration = 15 # 被动生成最低TPS

[blueprint]
blueprintRadius = 3           # 蓝图生成半径
blueprintCooldownSeconds = 30 # 蓝图冷却时间
blueprintConsumeOnUse = true  # 使用后消耗

[permissions]
maxRadiusForNonOp = 3         # 非OP最大生成半径
requireOpForRemove = true     # 删除需要OP
requireOpForReload = true     # 重载需要OP

[protection]
protectPlayerBuildings = true # 保护玩家建筑
playerBuildingDensityThreshold = 10 # 建筑密度阈值
respectClaimPlugins = true    # 尊重领地插件

[dimensions]
enableOverworld = true        # 主世界
enableNether = true           # 下界
enableEnd = true              # 末地
netherPassiveGeneration = false
endPassiveGeneration = false

[backup]
enableAutoBackup = true       # 自动备份
maxBackupAge = 24             # 备份保留时间(小时)
```

## 物品

- **铁轨蓝图**：右键使用可在周围生成铁轨网络

## 注意事项

1. 首次生成建议使用小半径（3-5）测试
2. 大范围生成会分批异步执行
3. 遇问题可用 `/railway emergency stop` 紧急停止
4. 使用 `/railway rollback` 可回滚
5. 建议生成前备份存档
