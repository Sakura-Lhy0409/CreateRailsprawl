/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

/**
 * 铁轨生成配置系统
 * 支持 railwaymod-common.toml 配置文件
 */
public class RailwayConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final CommonConfig COMMON;

    static {
        Pair<CommonConfig, ForgeConfigSpec> commonPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        COMMON = commonPair.getLeft();
        COMMON_SPEC = commonPair.getRight();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, "railwaymod-common.toml");
    }

    public static void reload() {
        // ForgeConfigSpec 会自动重载，此方法用于触发相关逻辑
    }

    public static class CommonConfig {
        // 生成参数
        public final ForgeConfigSpec.IntValue maxGenerationRadius;
        public final ForgeConfigSpec.IntValue lampInterval;
        public final ForgeConfigSpec.IntValue bridgePillarInterval;
        public final ForgeConfigSpec.IntValue structureAvoidDistance;
        public final ForgeConfigSpec.IntValue stationWidth;
        public final ForgeConfigSpec.IntValue stationLength;

        // 性能参数
        public final ForgeConfigSpec.IntValue chunksPerBatch;
        public final ForgeConfigSpec.IntValue batchIntervalMs;
        public final ForgeConfigSpec.IntValue maxGenerationsPerPlayer;
        public final ForgeConfigSpec.IntValue generationCooldownMinutes;
        public final ForgeConfigSpec.IntValue minTpsForPassiveGeneration;

        // 蓝图参数
        public final ForgeConfigSpec.IntValue blueprintRadius;
        public final ForgeConfigSpec.IntValue blueprintCooldownSeconds;
        public final ForgeConfigSpec.BooleanValue blueprintConsumeOnUse;

        // 权限参数
        public final ForgeConfigSpec.IntValue maxRadiusForNonOp;
        public final ForgeConfigSpec.BooleanValue requireOpForRemove;
        public final ForgeConfigSpec.BooleanValue requireOpForReload;

        // 保护参数
        public final ForgeConfigSpec.BooleanValue protectPlayerBuildings;
        public final ForgeConfigSpec.IntValue playerBuildingDensityThreshold;
        public final ForgeConfigSpec.BooleanValue respectClaimPlugins;

        // 维度参数
        public final ForgeConfigSpec.BooleanValue enableOverworld;
        public final ForgeConfigSpec.BooleanValue enableNether;
        public final ForgeConfigSpec.BooleanValue enableEnd;
        public final ForgeConfigSpec.BooleanValue netherPassiveGeneration;
        public final ForgeConfigSpec.BooleanValue endPassiveGeneration;

        // TrackSpawner参数
        public final ForgeConfigSpec.BooleanValue enableTrackSpawner;
        public final ForgeConfigSpec.IntValue trackSpawnerRange;

        // 调试参数
        public final ForgeConfigSpec.BooleanValue enableDebugMode;
        public final ForgeConfigSpec.BooleanValue enableDetailedLogging;

        // 备份参数
        public final ForgeConfigSpec.BooleanValue enableAutoBackup;
        public final ForgeConfigSpec.IntValue maxBackupAge;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.push("generation");
            maxGenerationRadius = builder
                    .comment("主动生成最大半径（区块数）")
                    .defineInRange("maxGenerationRadius", 20, 1, 50);
            lampInterval = builder
                    .comment("路灯生成间隔（方块数）")
                    .defineInRange("lampInterval", 20, 5, 100);
            bridgePillarInterval = builder
                    .comment("桥墩生成间隔（方块数）")
                    .defineInRange("bridgePillarInterval", 6, 3, 20);
            structureAvoidDistance = builder
                    .comment("建筑避让偏移距离（方块数）")
                    .defineInRange("structureAvoidDistance", 4, 1, 10);
            stationWidth = builder
                    .comment("站台宽度")
                    .defineInRange("stationWidth", 3, 2, 10);
            stationLength = builder
                    .comment("站台长度")
                    .defineInRange("stationLength", 5, 3, 20);
            builder.pop();

            builder.push("performance");
            chunksPerBatch = builder
                    .comment("每批次处理区块数")
                    .defineInRange("chunksPerBatch", 2, 1, 10);
            batchIntervalMs = builder
                    .comment("批次间隔（毫秒）")
                    .defineInRange("batchIntervalMs", 200, 50, 2000);
            maxGenerationsPerPlayer = builder
                    .comment("每玩家10分钟内最大生成次数")
                    .defineInRange("maxGenerationsPerPlayer", 3, 1, 20);
            generationCooldownMinutes = builder
                    .comment("生成冷却时间（分钟）")
                    .defineInRange("generationCooldownMinutes", 10, 1, 60);
            minTpsForPassiveGeneration = builder
                    .comment("被动生成最低TPS阈值")
                    .defineInRange("minTpsForPassiveGeneration", 15, 5, 20);
            builder.pop();

            builder.push("blueprint");
            blueprintRadius = builder
                    .comment("蓝图生成半径（区块数）")
                    .defineInRange("blueprintRadius", 3, 1, 10);
            blueprintCooldownSeconds = builder
                    .comment("蓝图使用冷却（秒）")
                    .defineInRange("blueprintCooldownSeconds", 30, 0, 300);
            blueprintConsumeOnUse = builder
                    .comment("使用后消耗蓝图")
                    .define("blueprintConsumeOnUse", true);
            builder.pop();

            builder.push("permissions");
            maxRadiusForNonOp = builder
                    .comment("非OP玩家最大生成半径")
                    .defineInRange("maxRadiusForNonOp", 3, 1, 10);
            requireOpForRemove = builder
                    .comment("删除命令需要OP权限")
                    .define("requireOpForRemove", true);
            requireOpForReload = builder
                    .comment("重载命令需要OP权限")
                    .define("requireOpForReload", true);
            builder.pop();

            builder.push("protection");
            protectPlayerBuildings = builder
                    .comment("保护玩家建筑")
                    .define("protectPlayerBuildings", true);
            playerBuildingDensityThreshold = builder
                    .comment("玩家建筑密度阈值（触发保护）")
                    .defineInRange("playerBuildingDensityThreshold", 10, 1, 100);
            respectClaimPlugins = builder
                    .comment("尊重领地插件")
                    .define("respectClaimPlugins", true);
            builder.pop();

            builder.push("dimensions");
            enableOverworld = builder
                    .comment("主世界启用生成")
                    .define("enableOverworld", true);
            enableNether = builder
                    .comment("下界启用生成")
                    .define("enableNether", true);
            enableEnd = builder
                    .comment("末地启用生成")
                    .define("enableEnd", true);
            netherPassiveGeneration = builder
                    .comment("下界启用被动生成")
                    .define("netherPassiveGeneration", false);
            endPassiveGeneration = builder
                    .comment("末地启用被动生成")
                    .define("endPassiveGeneration", false);
            builder.pop();

            builder.push("trackSpawner");
            enableTrackSpawner = builder
                    .comment("启用TrackSpawner延迟生成轨道")
                    .define("enableTrackSpawner", true);
            trackSpawnerRange = builder
                    .comment("TrackSpawner触发范围（方块数）")
                    .defineInRange("trackSpawnerRange", 100, 16, 256);
            builder.pop();

            builder.push("debug");
            enableDebugMode = builder
                    .comment("启用调试模式（显示粒子效果）")
                    .define("enableDebugMode", false);
            enableDetailedLogging = builder
                    .comment("启用详细日志")
                    .define("enableDetailedLogging", true);
            builder.pop();

            builder.push("backup");
            enableAutoBackup = builder
                    .comment("启用自动备份")
                    .define("enableAutoBackup", true);
            maxBackupAge = builder
                    .comment("备份最大保留时间（小时）")
                    .defineInRange("maxBackupAge", 24, 1, 168);
            builder.pop();
        }
    }
}
