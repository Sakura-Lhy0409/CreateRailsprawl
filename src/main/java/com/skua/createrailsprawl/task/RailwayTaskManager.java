/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.task;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.config.RailwayConfig;
import com.skua.createrailsprawl.generator.AestheticRailwayGenerator;
import com.skua.createrailsprawl.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 异步任务队列管理器
 * 实现分片批次处理，避免服务器卡顿
 */
@Mod.EventBusSubscriber(modid = CreateRailsprawl.MOD_ID)
public class RailwayTaskManager {

    private static final Queue<GenerationTask> taskQueue = new ConcurrentLinkedQueue<>();
    private static final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> playerGenerationCounts = new ConcurrentHashMap<>();
    private static long lastBatchTime = 0;
    private static boolean emergencyStop = false;
    private static boolean pausedDueToTps = false;

    /**
     * 生成任务
     */
    public static class GenerationTask {
        public final UUID playerId;
        public final ServerLevel level;
        public final List<ChunkPos> chunks;
        public final AestheticRailwayGenerator.GenerationResult result;
        public int currentIndex = 0;
        public final long startTime;
        public final String taskId;

        public GenerationTask(UUID playerId, ServerLevel level, List<ChunkPos> chunks) {
            this.playerId = playerId;
            this.level = level;
            this.chunks = chunks;
            this.result = new AestheticRailwayGenerator.GenerationResult();
            this.startTime = System.currentTimeMillis();
            this.taskId = UUID.randomUUID().toString().substring(0, 8);
        }

        public boolean isComplete() {
            return currentIndex >= chunks.size();
        }

        public float getProgress() {
            return chunks.isEmpty() ? 1f : (float) currentIndex / chunks.size();
        }
    }

    /**
     * 提交生成任务
     */
    public static String submitTask(ServerPlayer player, ServerLevel level, BlockPos center, int radiusChunks) {
        UUID playerId = player.getUUID();

        // 检查冷却
        if (!canPlayerGenerate(playerId)) {
            return null;
        }

        // 构建区块列表
        ChunkPos centerChunk = new ChunkPos(center);
        List<ChunkPos> chunks = new ArrayList<>();

        for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
                ChunkPos chunkPos = new ChunkPos(centerChunk.x + dx, centerChunk.z + dz);
                // 仅添加已加载的区块
                if (level.hasChunk(chunkPos.x, chunkPos.z)) {
                    chunks.add(chunkPos);
                }
            }
        }

        GenerationTask task = new GenerationTask(playerId, level, chunks);
        taskQueue.add(task);

        // 更新玩家生成计数
        playerGenerationCounts.merge(playerId, 1, Integer::sum);

        CreateRailsprawl.LOGGER.info("[RailwayMod] 玩家 {} 提交生成任务 {}，共 {} 个区块",
                player.getName().getString(), task.taskId, chunks.size());

        return task.taskId;
    }

    /**
     * 检查玩家是否可以生成
     */
    public static boolean canPlayerGenerate(UUID playerId) {
        long now = System.currentTimeMillis();
        int cooldownMinutes = RailwayConfig.COMMON.generationCooldownMinutes.get();
        int maxGenerations = RailwayConfig.COMMON.maxGenerationsPerPlayer.get();

        // 检查冷却
        Long lastGeneration = playerCooldowns.get(playerId);
        if (lastGeneration != null && now - lastGeneration < cooldownMinutes * 60 * 1000L) {
            // 检查生成次数
            int count = playerGenerationCounts.getOrDefault(playerId, 0);
            if (count >= maxGenerations) {
                return false;
            }
        } else {
            // 冷却已过，重置计数
            playerGenerationCounts.put(playerId, 0);
        }

        playerCooldowns.put(playerId, now);
        return true;
    }

    /**
     * 服务器Tick处理
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (emergencyStop) return;

        MinecraftServer server = event.getServer();

        // TPS检测
        double tps = calculateTps(server);
        int minTps = RailwayConfig.COMMON.minTpsForPassiveGeneration.get();

        if (tps < minTps) {
            if (!pausedDueToTps) {
                pausedDueToTps = true;
                CreateRailsprawl.LOGGER.warn("[RailwayMod] TPS过低 ({})，暂停生成任务", String.format("%.1f", tps));
            }
            return;
        } else if (pausedDueToTps) {
            pausedDueToTps = false;
            CreateRailsprawl.LOGGER.info("[RailwayMod] TPS恢复，继续生成任务");
        }

        // 批次间隔检查
        long now = System.currentTimeMillis();
        int batchInterval = RailwayConfig.COMMON.batchIntervalMs.get();
        if (now - lastBatchTime < batchInterval) return;
        lastBatchTime = now;

        // 处理任务队列
        processTaskBatch(server);
    }

    /**
     * 处理一批任务
     */
    private static void processTaskBatch(MinecraftServer server) {
        if (taskQueue.isEmpty()) return;

        int chunksPerBatch = RailwayConfig.COMMON.chunksPerBatch.get();
        int processedChunks = 0;

        Iterator<GenerationTask> iterator = taskQueue.iterator();
        while (iterator.hasNext() && processedChunks < chunksPerBatch) {
            GenerationTask task = iterator.next();

            // 检查世界是否有效
            if (task.level == null || !task.level.getServer().isRunning()) {
                iterator.remove();
                continue;
            }

            // 处理区块
            while (!task.isComplete() && processedChunks < chunksPerBatch) {
                ChunkPos chunkPos = task.chunks.get(task.currentIndex);

                // 检查区块是否已加载
                if (task.level.hasChunk(chunkPos.x, chunkPos.z)) {
                    try {
                        AestheticRailwayGenerator.generateInChunkAsync(task.level, chunkPos, task.result);
                        CreateRailsprawl.LOGGER.debug("[RailwayMod] 区块 [{},{}] 生成完成",
                                chunkPos.x, chunkPos.z);
                    } catch (Exception e) {
                        CreateRailsprawl.LOGGER.error("[RailwayMod] 区块 [{},{}] 生成失败: {}",
                                chunkPos.x, chunkPos.z, e.getMessage());
                    }
                }

                task.currentIndex++;
                processedChunks++;

                // 发送进度更新
                ServerPlayer player = server.getPlayerList().getPlayer(task.playerId);
                if (player != null) {
                    NetworkHandler.sendProgressUpdate(player, task);
                }
            }

            // 任务完成
            if (task.isComplete()) {
                iterator.remove();
                onTaskComplete(server, task);
            }
        }
    }

    /**
     * 任务完成回调
     */
    private static void onTaskComplete(MinecraftServer server, GenerationTask task) {
        ServerPlayer player = server.getPlayerList().getPlayer(task.playerId);
        if (player != null) {
            NetworkHandler.sendTaskComplete(player, task);
        }

        long duration = System.currentTimeMillis() - task.startTime;
        CreateRailsprawl.LOGGER.info("[RailwayMod] 任务 {} 完成，耗时 {}ms，{}",
                task.taskId, duration, task.result.getSummary());
    }

    /**
     * 紧急停止所有任务
     */
    public static void emergencyStop() {
        emergencyStop = true;
        int count = taskQueue.size();
        taskQueue.clear();
        CreateRailsprawl.LOGGER.warn("[RailwayMod] 紧急停止！已取消 {} 个任务", count);
    }

    /**
     * 恢复任务处理
     */
    public static void resume() {
        emergencyStop = false;
        CreateRailsprawl.LOGGER.info("[RailwayMod] 任务处理已恢复");
    }

    /**
     * 获取当前任务数量
     */
    public static int getQueueSize() {
        return taskQueue.size();
    }

    /**
     * 是否处于紧急停止状态
     */
    public static boolean isEmergencyStopped() {
        return emergencyStop;
    }

    /**
     * 计算服务器TPS
     */
    private static double calculateTps(MinecraftServer server) {
        double mspt = server.getAverageTickTime();
        return Math.min(20.0, 1000.0 / Math.max(mspt, 50));
    }

    /**
     * 获取统计信息
     */
    public static Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("queueSize", taskQueue.size());
        stats.put("emergencyStop", emergencyStop);
        stats.put("pausedDueToTps", pausedDueToTps);
        stats.put("activePlayers", playerGenerationCounts.size());
        return stats;
    }
}
