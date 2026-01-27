package com.skua.createrailsprawl.railway;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.runtime.ThreadPoolManager;
import com.skua.createrailsprawl.util.ModSaveData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

public class RailwayBuilder {
    private static RailwayBuilder instance;
    private static long seed;

    private final Map<RegionPos, Future<?>> regionFutures = new ConcurrentHashMap<>();
    public final Map<RegionPos, RailwayMap> regionRailways = new ConcurrentHashMap<>();
    public final Map<RegionPos, int[][]> regionHeightMap = new ConcurrentHashMap<>();
    public final Map<RegionPos, int[][]> regionStructureMap = new ConcurrentHashMap<>();

    private final ServerLevel serverLevel;

    private RailwayBuilder(WorldGenRegion level) {
        this.serverLevel = level.getLevel();
    }

    public static synchronized RailwayBuilder getInstance(long seed, WorldGenRegion level) {
        if (instance == null || RailwayBuilder.seed != seed) {
            instance = new RailwayBuilder(level);
            RailwayBuilder.seed = seed;
        }
        return instance;
    }

    public static synchronized RailwayBuilder getInstance(long seed) {
        if (instance == null || RailwayBuilder.seed != seed) {
            return null;
        }
        return instance;
    }

    public void generateRailway(RegionPos regionPos, WorldGenRegion worldGenRegion) {
        if (worldGenRegion == null) {
            return;
        }
        if (regionRailways.containsKey(regionPos)) {
            return;
        }

        ModSaveData data = ModSaveData.get(serverLevel);
        RailwayMap savedData = data.getRailwayMap(regionPos);
        if (savedData != null) {
            regionRailways.put(regionPos, savedData);
            CreateRailsprawl.LOGGER.info("Region {} Done! Read From Local Data", regionPos);
            return;
        }

        try {
            if (!regionFutures.containsKey(regionPos)) {
                final long epoch = ThreadPoolManager.currentEpoch();
                var f = ThreadPoolManager.computeExecutor().submit(() -> {
                    if (!ThreadPoolManager.isEpoch(epoch)) {
                        CreateRailsprawl.LOGGER.debug("Region {} 任务已过期，跳过", regionPos);
                        return;
                    }
                    RailwayMap railwayMap = new RailwayMap(regionPos);
                    railwayMap.startPlanningRoutes(worldGenRegion);
                    if (!ThreadPoolManager.isEpoch(epoch)) {
                        CreateRailsprawl.LOGGER.debug("Region {} 任务执行中过期，丢弃结果", regionPos);
                        return;
                    }
                    regionRailways.put(regionPos, railwayMap);
                    data.putRailwayMap(regionPos, railwayMap);
                });
                regionFutures.put(regionPos, f);
            }
            regionFutures.get(regionPos).get();
        } catch (InterruptedException | ExecutionException e) {
            CreateRailsprawl.LOGGER.error(e.getMessage());
        } finally {
            regionFutures.remove(regionPos);
        }
    }

    /**
     * 清理所有缓存数据（服务器停止时调用）
     */
    public static synchronized void clearAll() {
        if (instance != null) {
            instance.regionFutures.clear();
            instance.regionRailways.clear();
            instance.regionHeightMap.clear();
            instance.regionStructureMap.clear();
            instance = null;
        }
        seed = 0;
    }
}
