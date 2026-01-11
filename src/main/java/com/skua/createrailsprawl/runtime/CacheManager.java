package com.skua.createrailsprawl.runtime;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.client.map.RailwayMapSnapshot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理器 - 借鉴 RoadWeaver 的优化
 */
public final class CacheManager {
    private CacheManager() {}

    // 地图快照缓存
    private static final ConcurrentHashMap<ResourceLocation, RailwayMapSnapshot> MAP_SNAPSHOT_CACHE = new ConcurrentHashMap<>();
    private static ScheduledFuture<?> clearTask;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "CRS-CacheCleanup");
        t.setDaemon(true);
        return t;
    });

    public static void onServerStarted() {
        MAP_SNAPSHOT_CACHE.clear();
        CreateRailsprawl.LOGGER.debug("CacheManager: 缓存已初始化");
    }

    public static void onServerStopping() {
        MAP_SNAPSHOT_CACHE.clear();
        if (clearTask != null) {
            clearTask.cancel(false);
            clearTask = null;
        }
        CreateRailsprawl.LOGGER.debug("CacheManager: 所有缓存已清理");
    }

    public static void onDimensionUnload(ServerLevel level) {
        if (level == null) return;
        MAP_SNAPSHOT_CACHE.remove(level.dimension().location());
        CreateRailsprawl.LOGGER.debug("CacheManager: 维度 {} 的缓存已清理", level.dimension().location());
    }

    // 地图快照缓存操作
    public static void putMapSnapshot(ResourceLocation dimensionId, RailwayMapSnapshot snapshot) {
        if (dimensionId != null && snapshot != null) {
            MAP_SNAPSHOT_CACHE.put(dimensionId, snapshot);
        }
    }

    public static RailwayMapSnapshot getMapSnapshot(ResourceLocation dimensionId) {
        return dimensionId != null ? MAP_SNAPSHOT_CACHE.get(dimensionId) : null;
    }

    public static void scheduleClearMapSnapshot(long delayMs) {
        if (clearTask != null) {
            clearTask.cancel(false);
        }
        clearTask = scheduler.schedule(() -> MAP_SNAPSHOT_CACHE.clear(), delayMs, TimeUnit.MILLISECONDS);
    }

    public static void cancelClearMapSnapshot() {
        if (clearTask != null) {
            clearTask.cancel(false);
            clearTask = null;
        }
    }
}
