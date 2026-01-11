package com.skua.createrailsprawl.runtime;

import com.skua.createrailsprawl.railway.RegionPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.WorldGenLevel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 铁路空间索引 - 使用网格划分实现高效空间查询
 * 查询复杂度从 O(n) 降低到 O(1)~O(k)
 */
public final class RailwaySpatialIndex {
    private RailwaySpatialIndex() {}

    private static final int GRID_SIZE = 8;
    private static final int GRID_SHIFT = 3;
    private static final int MAX_CACHED_CHUNKS_PER_DIM = 512;

    // 维度 -> 区块坐标 -> 网格索引
    private static final Map<String, Map<Long, ChunkGridIndex>> CHUNK_INDEX = new ConcurrentHashMap<>();

    /**
     * 判断指定位置是否在铁路附近
     */
    public static boolean isNearRailway(WorldGenLevel level, BlockPos pos, int margin) {
        ServerLevel serverLevel = extractServerLevel(level);
        return serverLevel != null && isNearRailway(serverLevel, pos, margin);
    }

    public static boolean isNearRailway(ServerLevel level, BlockPos pos, int margin) {
        if (level == null || pos == null) return false;

        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        String dimKey = dimKey(level);

        Map<Long, ChunkGridIndex> dimIndex = CHUNK_INDEX.computeIfAbsent(dimKey, k -> createLRUCache());
        long chunkKey = chunkKey(cx, cz);

        ChunkGridIndex gridIndex = dimIndex.get(chunkKey);
        if (gridIndex == null || gridIndex.isEmpty()) return false;

        int px = pos.getX(), py = pos.getY(), pz = pos.getZ();
        int gridX = px >> GRID_SHIFT;
        int gridZ = pz >> GRID_SHIFT;
        int gridRadius = (margin >> GRID_SHIFT) + 1;

        for (int dx = -gridRadius; dx <= gridRadius; dx++) {
            for (int dz = -gridRadius; dz <= gridRadius; dz++) {
                long gridKey = gridKey(gridX + dx, gridZ + dz);
                Set<Long> points = gridIndex.getPoints(gridKey);
                if (points == null || points.isEmpty()) continue;

                for (long packed : points) {
                    BlockPos rail = BlockPos.of(packed);
                    int rdx = Math.abs(px - rail.getX());
                    int rdz = Math.abs(pz - rail.getZ());
                    if (rdx <= margin && rdz <= margin) {
                        int yDiff = py - rail.getY();
                        if (yDiff >= -2 && yDiff <= 12) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * 添加铁路点到索引
     */
    public static void addRailwayPoint(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null) return;

        String dimKey = dimKey(level);
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;

        Map<Long, ChunkGridIndex> dimIndex = CHUNK_INDEX.computeIfAbsent(dimKey, k -> createLRUCache());
        long chunkKey = chunkKey(cx, cz);

        ChunkGridIndex gridIndex = dimIndex.computeIfAbsent(chunkKey, k -> new ChunkGridIndex());

        int gridX = pos.getX() >> GRID_SHIFT;
        int gridZ = pos.getZ() >> GRID_SHIFT;
        gridIndex.addPoint(gridKey(gridX, gridZ), pos.asLong());
    }

    private static Map<Long, ChunkGridIndex> createLRUCache() {
        return Collections.synchronizedMap(
            new LinkedHashMap<Long, ChunkGridIndex>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, ChunkGridIndex> eldest) {
                    return size() > MAX_CACHED_CHUNKS_PER_DIM;
                }
            }
        );
    }

    @SuppressWarnings("deprecation")
    private static ServerLevel extractServerLevel(WorldGenLevel level) {
        if (level instanceof ServerLevel sl) return sl;
        if (level instanceof WorldGenRegion region) return region.getLevel();
        return null;
    }

    private static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private static long gridKey(int gx, int gz) {
        return ((long) gx << 32) | (gz & 0xFFFFFFFFL);
    }

    private static String dimKey(ServerLevel level) {
        return level.dimension().location().toString();
    }

    // 缓存管理
    public static void clearCache(ServerLevel level) {
        if (level != null) CHUNK_INDEX.remove(dimKey(level));
    }

    public static void clearAllCache() {
        CHUNK_INDEX.clear();
    }

    public static void invalidateChunk(ServerLevel level, int cx, int cz) {
        if (level == null) return;
        Map<Long, ChunkGridIndex> dimIndex = CHUNK_INDEX.get(dimKey(level));
        if (dimIndex != null) dimIndex.remove(chunkKey(cx, cz));
    }

    private static final class ChunkGridIndex {
        private final Map<Long, Set<Long>> grids = new ConcurrentHashMap<>();

        void addPoint(long gridKey, long packedPos) {
            grids.computeIfAbsent(gridKey, k -> ConcurrentHashMap.newKeySet()).add(packedPos);
        }

        Set<Long> getPoints(long gridKey) {
            return grids.get(gridKey);
        }

        boolean isEmpty() {
            return grids.isEmpty();
        }
    }
}
