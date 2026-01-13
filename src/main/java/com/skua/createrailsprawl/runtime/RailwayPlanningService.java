package com.skua.createrailsprawl.runtime;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.railway.RegionPos;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 铁路规划服务 - 异步增量规划
 */
public final class RailwayPlanningService {
    private RailwayPlanningService() {}

    // 已规划的区域
    private static final Set<Long> PLANNED_REGIONS = ConcurrentHashMap.newKeySet();
    private static final int MAX_PLANNED_REGIONS = 200_000;

    /**
     * 异步规划指定区域
     */
    public static CompletableFuture<Void> planRegionAsync(ServerLevel level, RegionPos regionPos) {
        long key = regionKey(regionPos);
        if (PLANNED_REGIONS.contains(key)) {
            return CompletableFuture.completedFuture(null);
        }

        final long epoch = ThreadPoolManager.currentEpoch();
        return ComputeService.runAsyncWithEpoch(() -> {
            if (!ThreadPoolManager.isEpoch(epoch)) return;

            // 标记为已规划
            PLANNED_REGIONS.add(key);
            pruneIfTooLarge();

            CreateRailsprawl.LOGGER.debug("Region {} 规划完成", regionPos);
        });
    }

    /**
     * 检查区域是否已规划
     */
    public static boolean isPlanned(RegionPos regionPos) {
        return PLANNED_REGIONS.contains(regionKey(regionPos));
    }

    /**
     * 清理过大的缓存
     */
    private static void pruneIfTooLarge() {
        if (PLANNED_REGIONS.size() > MAX_PLANNED_REGIONS) {
            int toRemove = PLANNED_REGIONS.size() - MAX_PLANNED_REGIONS;
            var it = PLANNED_REGIONS.iterator();
            while (toRemove > 0 && it.hasNext()) {
                it.next();
                it.remove();
                toRemove--;
            }
        }
    }

    private static long regionKey(RegionPos pos) {
        return ((long) pos.x() << 32) | (pos.z() & 0xFFFFFFFFL);
    }

    public static void resetAll() {
        PLANNED_REGIONS.clear();
    }
}
