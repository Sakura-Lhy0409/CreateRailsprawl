/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.manager;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 铁轨路径管理器 - 单例类
 * 管理所有区块的主动/被动铁轨节点，提供冲突检测和内存管理
 */
public class RailwayPathManager {
    private static final RailwayPathManager INSTANCE = new RailwayPathManager();

    // 存储每个区块的铁轨节点位置
    private final Map<ChunkPos, Set<BlockPos>> chunkNodes = new ConcurrentHashMap<>();
    // 存储已生成铁轨的区块，防止重复生成
    private final Set<ChunkPos> generatedChunks = ConcurrentHashMap.newKeySet();

    private RailwayPathManager() {}

    public static RailwayPathManager getInstance() {
        return INSTANCE;
    }

    /**
     * 检查区块是否已生成铁轨
     * @param chunkPos 区块位置
     * @return 是否已生成
     */
    public boolean isChunkGenerated(ChunkPos chunkPos) {
        return generatedChunks.contains(chunkPos);
    }

    /**
     * 标记区块已生成铁轨
     * @param chunkPos 区块位置
     */
    public void markChunkGenerated(ChunkPos chunkPos) {
        generatedChunks.add(chunkPos);
    }

    /**
     * 添加铁轨节点
     * @param chunkPos 区块位置
     * @param nodePos 节点位置
     * @return 是否添加成功���false表示节点已存在）
     */
    public boolean addNode(ChunkPos chunkPos, BlockPos nodePos) {
        Set<BlockPos> nodes = chunkNodes.computeIfAbsent(chunkPos, k -> ConcurrentHashMap.newKeySet());
        return nodes.add(nodePos);
    }

    /**
     * 获取区块内所有节点
     * @param chunkPos 区块位置
     * @return 节点集合
     */
    public Set<BlockPos> getNodes(ChunkPos chunkPos) {
        return chunkNodes.getOrDefault(chunkPos, Set.of());
    }

    /**
     * 获取相邻区块的节点用于连接
     * @param chunkPos 当前区块位置
     * @return 相邻区块的所有节点
     */
    public Set<BlockPos> getAdjacentNodes(ChunkPos chunkPos) {
        Set<BlockPos> adjacentNodes = ConcurrentHashMap.newKeySet();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                ChunkPos adjacent = new ChunkPos(chunkPos.x + dx, chunkPos.z + dz);
                adjacentNodes.addAll(getNodes(adjacent));
            }
        }
        return adjacentNodes;
    }

    /**
     * 检查位置是否存在节点冲突
     * @param pos 检查位置
     * @param radius 检查半径
     * @return 是否存在冲突
     */
    public boolean hasConflict(BlockPos pos, int radius) {
        ChunkPos chunkPos = new ChunkPos(pos);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos check = new ChunkPos(chunkPos.x + dx, chunkPos.z + dz);
                for (BlockPos node : getNodes(check)) {
                    if (node.distManhattan(pos) < radius) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 区块卸载时清除对应节点数据，防止内存泄漏
     * @param chunkPos 区块位置
     */
    public void onChunkUnload(ChunkPos chunkPos) {
        chunkNodes.remove(chunkPos);
        generatedChunks.remove(chunkPos);
    }

    /**
     * 清除所有数据（用于世界切换）
     */
    public void clear() {
        chunkNodes.clear();
        generatedChunks.clear();
    }
}
