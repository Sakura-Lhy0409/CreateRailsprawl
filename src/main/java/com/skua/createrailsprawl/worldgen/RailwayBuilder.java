/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.worldgen;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.data.ModSaveData;
import com.skua.createrailsprawl.planner.RoutePlanner.RegionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

/**
 * 铁路建造器单例
 * 管理所有区域的铁路生成
 * 参考 TongDaRailway 的实现
 */
public class RailwayBuilder {
    private static RailwayBuilder instance;
    private static long currentSeed;

    private final Map<RegionPos, Future<?>> regionFutures = new ConcurrentHashMap<>();
    public final Map<RegionPos, RailwayMap> regionRailways = new ConcurrentHashMap<>();
    // 参考 TongDaRailway: 缓存高度图和结构图以提高性能
    public final Map<RegionPos, int[][]> regionHeightMap = new ConcurrentHashMap<>();
    public final Map<RegionPos, int[][]> regionStructureMap = new ConcurrentHashMap<>();

    private final LinkedBlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            64, 1024, 1, TimeUnit.DAYS, taskQueue
    );

    private WorldGenRegion level;

    private RailwayBuilder(WorldGenRegion level) {
        this.level = level;
    }

    public static synchronized RailwayBuilder getInstance(long seed, WorldGenRegion level) {
        if (instance == null || currentSeed != seed) {
            instance = new RailwayBuilder(level);
            currentSeed = seed;
        }
        return instance;
    }

    public static synchronized RailwayBuilder getInstance(long seed) {
        if (instance == null || currentSeed != seed) {
            return null;
        }
        return instance;
    }

    /**
     * 生成区域铁路（参考 TongDaRailway）
     * 如果已生成则直接返回，否则启动异步线程生成并等待完成
     */
    public void generateRailway(RegionPos regionPos) {
        // 如果已生成，直接返回
        if (regionRailways.containsKey(regionPos)) {
            return;
        }

        // 尝试从本地存档读取
        try {
            ServerLevel serverLevel = Objects.requireNonNull(level.getServer()).getLevel(ServerLevel.OVERWORLD);
            if (serverLevel != null) {
                ModSaveData data = ModSaveData.get(serverLevel);
                RailwayMap savedData = data.getRailwayMap(regionPos);
                if (savedData != null) {
                    regionRailways.put(regionPos, savedData);
                    CreateRailsprawl.LOGGER.info("区域 {} 从存档读取完成", regionPos);
                    return;
                }
            }
        } catch (Exception e) {
            // 忽略存档读取错误，继续生成
        }

        try {
            // 如果没有线程在生成，启动新线程
            if (!regionFutures.containsKey(regionPos)) {
                var future = executor.submit(() -> {
                    try {
                        long startTime = System.currentTimeMillis();

                        RailwayMap railwayMap = new RailwayMap(regionPos, level.getSeed());
                        railwayMap.startPlanningRoutes(level);

                        regionRailways.put(regionPos, railwayMap);

                        // 保存到存档
                        try {
                            ServerLevel serverLevel = Objects.requireNonNull(level.getServer()).getLevel(ServerLevel.OVERWORLD);
                            if (serverLevel != null) {
                                ModSaveData data = ModSaveData.get(serverLevel);
                                data.setRailwayMap(regionPos, railwayMap);
                            }
                        } catch (Exception e) {
                            // 忽略保存错误
                        }

                        long endTime = System.currentTimeMillis();
                        CreateRailsprawl.LOGGER.info("区域 {} 铁路生成完成，耗时 {}ms", regionPos, endTime - startTime);
                    } catch (Exception e) {
                        CreateRailsprawl.LOGGER.error("区域 {} 铁路生成失败", regionPos, e);
                    }
                });
                regionFutures.put(regionPos, future);
            }

            // 等待线程完成
            regionFutures.get(regionPos).get();
        } catch (InterruptedException | ExecutionException e) {
            CreateRailsprawl.LOGGER.error("等待区域 {} 生成时出错: {}", regionPos, e.getMessage());
        } finally {
            regionFutures.remove(regionPos);
        }
    }

    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
        regionRailways.clear();
        regionFutures.clear();
    }

    public static void clearAll() {
        if (instance != null) {
            instance.shutdown();
            instance = null;
        }
    }
}
