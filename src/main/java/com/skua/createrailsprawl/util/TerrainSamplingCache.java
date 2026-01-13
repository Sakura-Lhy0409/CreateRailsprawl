package com.skua.createrailsprawl.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 地形采样缓存，减少重复采样提升性能
 */
public class TerrainSamplingCache {
    private final ConcurrentHashMap<Long, Integer> heightCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> oceanFloorCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Boolean> waterCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Holder<Biome>> biomeCache = new ConcurrentHashMap<>();

    private static long packCoord(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public int height(ServerLevel level, int x, int z) {
        long key = packCoord(x, z);
        return heightCache.computeIfAbsent(key, k ->
                level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z));
    }

    public int oceanFloor(ServerLevel level, int x, int z) {
        long key = packCoord(x, z);
        return oceanFloorCache.computeIfAbsent(key, k ->
                level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z));
    }

    public boolean isColumnWater(ServerLevel level, int x, int z) {
        long key = packCoord(x, z);
        return waterCache.computeIfAbsent(key, k -> {
            int surfaceY = height(level, x, z);
            BlockState state = level.getBlockState(new BlockPos(x, surfaceY, z));
            return state.is(Blocks.WATER) || state.getFluidState().isSource();
        });
    }

    public Holder<Biome> biome(ServerLevel level, int x, int z) {
        long key = packCoord(x, z);
        return biomeCache.computeIfAbsent(key, k ->
                level.getBiome(new BlockPos(x, 64, z)));
    }

    public boolean isNearWater(ServerLevel level, int x, int z, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (isColumnWater(level, x + dx, z + dz)) {
                    return true;
                }
            }
        }
        return false;
    }

    public int waterDepth(ServerLevel level, int x, int z) {
        int surface = height(level, x, z);
        int floor = oceanFloor(level, x, z);
        int seaLevel = level.getSeaLevel();
        if (surface <= seaLevel && isColumnWater(level, x, z)) {
            return Math.max(0, seaLevel - floor);
        }
        return 0;
    }

    public void clear() {
        heightCache.clear();
        oceanFloorCache.clear();
        waterCache.clear();
        biomeCache.clear();
    }

    public int size() {
        return heightCache.size() + oceanFloorCache.size() + waterCache.size() + biomeCache.size();
    }
}
