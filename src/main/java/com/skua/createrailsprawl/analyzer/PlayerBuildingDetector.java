/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.analyzer;

import com.skua.createrailsprawl.config.RailwayConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 玩家建筑保护检测器
 * 检测区块内的玩家放置方块，避免破坏玩家建筑
 */
public class PlayerBuildingDetector {

    /**
     * 检测结果
     */
    public static class DetectionResult {
        public final boolean hasPlayerBuilding;
        public final int playerBlockCount;
        public final BlockPos densestArea;

        public DetectionResult(boolean hasPlayerBuilding, int playerBlockCount, BlockPos densestArea) {
            this.hasPlayerBuilding = hasPlayerBuilding;
            this.playerBlockCount = playerBlockCount;
            this.densestArea = densestArea;
        }
    }

    /**
     * 检测区块内是否有玩家建筑
     * @param level 世界
     * @param chunkPos 区块位置
     * @return 检测结果
     */
    public static DetectionResult detectPlayerBuilding(ServerLevel level, ChunkPos chunkPos) {
        if (!RailwayConfig.COMMON.protectPlayerBuildings.get()) {
            return new DetectionResult(false, 0, null);
        }

        int threshold = RailwayConfig.COMMON.playerBuildingDensityThreshold.get();
        int playerBlockCount = 0;
        BlockPos densestArea = null;
        int maxDensity = 0;

        int minX = chunkPos.getMinBlockX();
        int minZ = chunkPos.getMinBlockZ();

        // 扫描区块内的方块
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = minX + x;
                int worldZ = minZ + z;

                // 扫描地表附近
                int surfaceY = TerrainStructureBiomeAnalyzer.findSurfaceY(level, worldX, worldZ);

                for (int y = surfaceY - 5; y < surfaceY + 20; y++) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    BlockState state = level.getBlockState(pos);

                    if (isPlayerPlacedBlock(state)) {
                        playerBlockCount++;

                        // 计算局部密度
                        int localDensity = countNearbyPlayerBlocks(level, pos);
                        if (localDensity > maxDensity) {
                            maxDensity = localDensity;
                            densestArea = pos;
                        }
                    }
                }
            }
        }

        boolean hasBuilding = playerBlockCount >= threshold;
        return new DetectionResult(hasBuilding, playerBlockCount, densestArea);
    }

    /**
     * 检测指定位置周围是否有玩家建筑
     * @param level 世界
     * @param pos 位置
     * @param radius 检测半径
     * @return 是否有玩家建筑
     */
    public static boolean hasNearbyPlayerBuilding(ServerLevel level, BlockPos pos, int radius) {
        if (!RailwayConfig.COMMON.protectPlayerBuildings.get()) {
            return false;
        }

        int threshold = RailwayConfig.COMMON.playerBuildingDensityThreshold.get();
        int count = 0;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    if (isPlayerPlacedBlock(level.getBlockState(checkPos))) {
                        count++;
                        if (count >= threshold) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 判断方块是否可能是玩家放置的
     * 通过方块类型判断（非自然生成的方块）
     */
    private static boolean isPlayerPlacedBlock(BlockState state) {
        // 检测常见的玩家建筑方块
        String blockName = state.getBlock().getDescriptionId();

        // 人工方块特征
        if (blockName.contains("planks") ||
            blockName.contains("brick") ||
            blockName.contains("glass") ||
            blockName.contains("door") ||
            blockName.contains("fence") ||
            blockName.contains("stairs") ||
            blockName.contains("slab") ||
            blockName.contains("wall") ||
            blockName.contains("bed") ||
            blockName.contains("chest") ||
            blockName.contains("furnace") ||
            blockName.contains("crafting") ||
            blockName.contains("torch") ||
            blockName.contains("lantern") ||
            blockName.contains("carpet") ||
            blockName.contains("wool") ||
            blockName.contains("concrete") ||
            blockName.contains("terracotta") ||
            blockName.contains("glazed")) {
            return true;
        }

        return false;
    }

    /**
     * 计算附近的玩家方块数量
     */
    private static int countNearbyPlayerBlocks(ServerLevel level, BlockPos center) {
        int count = 0;
        int radius = 3;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (isPlayerPlacedBlock(level.getBlockState(pos))) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    /**
     * 查找避开玩家建筑的安全位置
     * @param level 世界
     * @param originalPos 原始位置
     * @param searchRadius 搜索半径
     * @return 安全位置，如果找不到返回null
     */
    public static BlockPos findSafePosition(ServerLevel level, BlockPos originalPos, int searchRadius) {
        // 螺旋搜索安全位置
        for (int r = 1; r <= searchRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) continue;

                    BlockPos testPos = originalPos.offset(dx, 0, dz);
                    int y = TerrainStructureBiomeAnalyzer.findSurfaceY(level, testPos.getX(), testPos.getZ());
                    testPos = new BlockPos(testPos.getX(), y, testPos.getZ());

                    if (!hasNearbyPlayerBuilding(level, testPos, 5)) {
                        return testPos;
                    }
                }
            }
        }

        return null;
    }
}
