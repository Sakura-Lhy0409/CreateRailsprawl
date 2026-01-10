/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.data;

import com.skua.createrailsprawl.CreateRailsprawl;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 铁轨数据持久化管理器
 * 支持数据存储到 world/railway_data 目录
 * 实现弱引用缓存和定时清理
 */
public class RailwayDataManager extends SavedData {

    private static final String DATA_NAME = "railway_network";
    private static final Map<ChunkPos, WeakReference<ChunkRailwayData>> chunkCache = new ConcurrentHashMap<>();
    private static RailwayDataManager instance;

    // 区块铁轨数据
    private final Map<ChunkPos, ChunkRailwayData> chunkData = new ConcurrentHashMap<>();
    // 全局统计
    private long totalTrackLength = 0;
    private int totalGeneratedChunks = 0;
    private long lastCleanupTime = 0;

    public RailwayDataManager() {
    }

    public static RailwayDataManager get(ServerLevel level) {
        if (instance == null) {
            instance = level.getDataStorage().computeIfAbsent(
                    RailwayDataManager::load,
                    RailwayDataManager::new,
                    DATA_NAME
            );
        }
        return instance;
    }

    public static RailwayDataManager load(CompoundTag tag) {
        RailwayDataManager manager = new RailwayDataManager();

        manager.totalTrackLength = tag.getLong("totalTrackLength");
        manager.totalGeneratedChunks = tag.getInt("totalGeneratedChunks");

        ListTag chunkList = tag.getList("chunks", Tag.TAG_COMPOUND);
        for (int i = 0; i < chunkList.size(); i++) {
            CompoundTag chunkTag = chunkList.getCompound(i);
            ChunkRailwayData data = ChunkRailwayData.load(chunkTag);
            manager.chunkData.put(data.chunkPos, data);
        }

        CreateRailsprawl.LOGGER.info("[RailwayMod] 加载铁轨数据：{} 个区块，总长度 {} 格",
                manager.totalGeneratedChunks, manager.totalTrackLength);

        return manager;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("totalTrackLength", totalTrackLength);
        tag.putInt("totalGeneratedChunks", totalGeneratedChunks);

        ListTag chunkList = new ListTag();
        for (ChunkRailwayData data : chunkData.values()) {
            chunkList.add(data.save());
        }
        tag.put("chunks", chunkList);

        return tag;
    }

    /**
     * 获取区块数据（带缓存）
     */
    public ChunkRailwayData getChunkData(ChunkPos pos) {
        // 先检查弱引用缓存
        WeakReference<ChunkRailwayData> ref = chunkCache.get(pos);
        if (ref != null) {
            ChunkRailwayData cached = ref.get();
            if (cached != null) {
                return cached;
            }
        }

        // 从持久化数据获取
        ChunkRailwayData data = chunkData.get(pos);
        if (data != null) {
            chunkCache.put(pos, new WeakReference<>(data));
        }
        return data;
    }

    /**
     * 设置区块数据
     */
    public void setChunkData(ChunkPos pos, ChunkRailwayData data) {
        ChunkRailwayData old = chunkData.put(pos, data);
        chunkCache.put(pos, new WeakReference<>(data));

        if (old == null) {
            totalGeneratedChunks++;
        }
        totalTrackLength += data.trackLength - (old != null ? old.trackLength : 0);

        setDirty();
    }

    /**
     * 移除区块数据
     * @return 是否成功移除
     */
    public boolean removeChunkData(ChunkPos pos) {
        ChunkRailwayData removed = chunkData.remove(pos);
        chunkCache.remove(pos);

        if (removed != null) {
            totalGeneratedChunks--;
            totalTrackLength -= removed.trackLength;
            setDirty();
            return true;
        }
        return false;
    }

    /**
     * 检查区块是否已生成
     */
    public boolean isChunkGenerated(ChunkPos pos) {
        return chunkData.containsKey(pos);
    }

    /**
     * 获取区块哈希值（用于冲突检测）
     */
    public String getChunkHash(ChunkPos pos) {
        ChunkRailwayData data = getChunkData(pos);
        return data != null ? data.getHash() : null;
    }

    /**
     * 定时清理无效节点
     */
    public void cleanupInvalidNodes() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < 30 * 60 * 1000) { // 30分钟
            return;
        }
        lastCleanupTime = now;

        int removed = 0;
        Iterator<Map.Entry<ChunkPos, ChunkRailwayData>> iterator = chunkData.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ChunkPos, ChunkRailwayData> entry = iterator.next();
            if (entry.getValue().isInvalid()) {
                iterator.remove();
                chunkCache.remove(entry.getKey());
                removed++;
            }
        }

        if (removed > 0) {
            CreateRailsprawl.LOGGER.info("[RailwayMod] 清理了 {} 个无效节点", removed);
            setDirty();
        }
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTrackLength", totalTrackLength);
        stats.put("totalGeneratedChunks", totalGeneratedChunks);
        stats.put("cachedChunks", chunkCache.size());
        stats.put("memoryUsage", estimateMemoryUsage());
        return stats;
    }

    private long estimateMemoryUsage() {
        return chunkData.size() * 256L; // 估算每个区块数据约256字节
    }

    /**
     * 重置实例（用于世界切换）
     */
    public static void reset() {
        instance = null;
        chunkCache.clear();
    }

    /**
     * 区块铁轨数据
     */
    public static class ChunkRailwayData {
        public final ChunkPos chunkPos;
        public final Set<BlockPos> nodes = new HashSet<>();
        public int trackLength = 0;
        public String terrainType = "";
        public long generationTime;
        public boolean hasStation = false;
        public boolean hasBridge = false;
        public boolean hasTunnel = false;

        public ChunkRailwayData(ChunkPos chunkPos) {
            this.chunkPos = chunkPos;
            this.generationTime = System.currentTimeMillis();
        }

        public static ChunkRailwayData load(CompoundTag tag) {
            ChunkPos pos = new ChunkPos(tag.getInt("chunkX"), tag.getInt("chunkZ"));
            ChunkRailwayData data = new ChunkRailwayData(pos);

            data.trackLength = tag.getInt("trackLength");
            data.terrainType = tag.getString("terrainType");
            data.generationTime = tag.getLong("generationTime");
            data.hasStation = tag.getBoolean("hasStation");
            data.hasBridge = tag.getBoolean("hasBridge");
            data.hasTunnel = tag.getBoolean("hasTunnel");

            ListTag nodeList = tag.getList("nodes", Tag.TAG_COMPOUND);
            for (int i = 0; i < nodeList.size(); i++) {
                CompoundTag nodeTag = nodeList.getCompound(i);
                data.nodes.add(new BlockPos(
                        nodeTag.getInt("x"),
                        nodeTag.getInt("y"),
                        nodeTag.getInt("z")
                ));
            }

            return data;
        }

        public CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("chunkX", chunkPos.x);
            tag.putInt("chunkZ", chunkPos.z);
            tag.putInt("trackLength", trackLength);
            tag.putString("terrainType", terrainType);
            tag.putLong("generationTime", generationTime);
            tag.putBoolean("hasStation", hasStation);
            tag.putBoolean("hasBridge", hasBridge);
            tag.putBoolean("hasTunnel", hasTunnel);

            ListTag nodeList = new ListTag();
            for (BlockPos node : nodes) {
                CompoundTag nodeTag = new CompoundTag();
                nodeTag.putInt("x", node.getX());
                nodeTag.putInt("y", node.getY());
                nodeTag.putInt("z", node.getZ());
                nodeList.add(nodeTag);
            }
            tag.put("nodes", nodeList);

            return tag;
        }

        public String getHash() {
            return String.format("%d_%d_%d_%d",
                    chunkPos.x, chunkPos.z, trackLength, nodes.size());
        }

        public boolean isInvalid() {
            return nodes.isEmpty() && trackLength == 0;
        }
    }
}
