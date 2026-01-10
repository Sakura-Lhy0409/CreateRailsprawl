/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.async;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.config.RailwayConfig;
import com.skua.createrailsprawl.generator.AestheticRailwayGenerator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 异步任务队列
 * 分批处理铁轨生成任务，避免服务器卡顿
 */
public class RailwayTaskQueue {

    private static final RailwayTaskQueue INSTANCE = new RailwayTaskQueue();
    private final ConcurrentLinkedQueue<GenerationTask> taskQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean paused = new AtomicBoolean(false);

    public static RailwayTaskQueue getInstance() {
        return INSTANCE;
    }

    public void start() {
        running.set(true);
        CreateRailsprawl.LOGGER.info("[RailwayMod] 任务队列已启动");
    }

    public void shutdown() {
        running.set(false);
        taskQueue.clear();
        CreateRailsprawl.LOGGER.info("[RailwayMod] 任务队列已关闭");
    }

    public void pause() {
        paused.set(true);
    }

    public void resume() {
        paused.set(false);
    }

    public boolean isPaused() {
        return paused.get();
    }

    public int getQueueSize() {
        return taskQueue.size();
    }

    public String submitTask(ServerLevel level, ServerPlayer player, List<ChunkPos> chunks) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        GenerationTask task = new GenerationTask(taskId, level, player, chunks);
        taskQueue.offer(task);
        return taskId;
    }

    public void processTick() {
        if (!running.get() || paused.get() || taskQueue.isEmpty()) {
            return;
        }

        GenerationTask task = taskQueue.peek();
        if (task == null) return;

        int batchSize = RailwayConfig.COMMON.chunksPerBatch.get();
        for (int i = 0; i < batchSize && task.hasNext(); i++) {
            ChunkPos chunk = task.nextChunk();
            AestheticRailwayGenerator.generateInChunkAsync(task.level, chunk, task.result);
        }

        if (!task.hasNext()) {
            taskQueue.poll();
            task.onComplete();
        }
    }

    public void clearAll() {
        taskQueue.clear();
    }

    /**
     * 生成任务
     */
    public static class GenerationTask {
        public final String taskId;
        public final ServerLevel level;
        public final ServerPlayer player;
        public final List<ChunkPos> chunks;
        public final AestheticRailwayGenerator.GenerationResult result;
        public int currentIndex = 0;

        public GenerationTask(String taskId, ServerLevel level, ServerPlayer player, List<ChunkPos> chunks) {
            this.taskId = taskId;
            this.level = level;
            this.player = player;
            this.chunks = chunks;
            this.result = new AestheticRailwayGenerator.GenerationResult();
        }

        public boolean hasNext() {
            return currentIndex < chunks.size();
        }

        public ChunkPos nextChunk() {
            return chunks.get(currentIndex++);
        }

        public void onComplete() {
            if (player != null) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§a生成完成！" + result.getSummary()));
            }
        }
    }
}
