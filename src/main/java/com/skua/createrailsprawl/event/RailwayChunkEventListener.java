/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.event;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.analyzer.DimensionTerrainAnalyzer;
import com.skua.createrailsprawl.analyzer.PlayerBuildingDetector;
import com.skua.createrailsprawl.config.RailwayConfig;
import com.skua.createrailsprawl.data.RailwayDataManager;
import com.skua.createrailsprawl.generator.AestheticRailwayGenerator;
import com.skua.createrailsprawl.integration.ClaimIntegration;
import com.skua.createrailsprawl.manager.RailwayPathManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 区块事件监听器
 * 监听区块加载/卸载事件，触发被动生成和节点清理
 * 支持维度检测、玩家建筑保护、领地兼容
 */
@Mod.EventBusSubscriber(modid = CreateRailsprawl.MOD_ID)
public class RailwayChunkEventListener {

    /**
     * 区块加载时触发被动铁轨生成
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        // 仅服务端处理
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // 世界加载期间跳过，避免阻塞
        if (level.getServer().getTickCount() < 100) {
            return;
        }

        ChunkPos chunkPos = event.getChunk().getPos();

        // 检查维度是否启用被动生成
        if (!DimensionTerrainAnalyzer.isPassiveGenerationEnabled(level)) {
            return;
        }

        // 检查是否已生成
        if (RailwayDataManager.get(level).isChunkGenerated(chunkPos)) {
            return;
        }

        // 检查领地保护
        if (!ClaimIntegration.canGenerateAt(level, chunkPos.getMiddleBlockPosition(64), null)) {
            return;
        }

        // 检查玩家建筑（简化检测，避免卡顿）
        PlayerBuildingDetector.DetectionResult detection =
                PlayerBuildingDetector.detectPlayerBuilding(level, chunkPos);
        if (detection.hasPlayerBuilding) {
            return;
        }

        // 被动生成铁轨
        AestheticRailwayGenerator.generatePassive(level, chunkPos);
    }

    /**
     * 区块卸载时清理节点数据，防止内存泄漏
     */
    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        // 仅服务端处理
        if (event.getLevel().isClientSide()) {
            return;
        }

        ChunkPos chunkPos = event.getChunk().getPos();
        RailwayPathManager.getInstance().onChunkUnload(chunkPos);
    }
}
