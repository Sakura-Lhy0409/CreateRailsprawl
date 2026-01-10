package com.skua.createrailsprawl.railway;

import com.skua.createrailsprawl.CreateRailsprawl;
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

    private final LinkedBlockingQueue<Runnable> regionRailwayLoadQueue = new LinkedBlockingQueue<>();
    private final ThreadPoolExecutor regionRailwayLoadPoolExecutor = new ThreadPoolExecutor(64, 1024, 1, TimeUnit.DAYS, regionRailwayLoadQueue);
    private final WorldGenRegion level;

    private RailwayBuilder(WorldGenRegion level) {
        this.level = level;
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

    public void generateRailway(RegionPos regionPos) {
        if (regionRailways.containsKey(regionPos)) {
            return;
        }

        ModSaveData data = ModSaveData.get(Objects.requireNonNull(level.getServer()).getLevel(ServerLevel.OVERWORLD));
        RailwayMap savedData = data.getRailwayMap(regionPos);
        if (savedData != null) {
            regionRailways.put(regionPos, savedData);
            CreateRailsprawl.LOGGER.info("Region {} Done! Read From Local Data", regionPos);
            return;
        }

        try {
            if (!regionFutures.containsKey(regionPos)) {
                var f = regionRailwayLoadPoolExecutor.submit(() -> {
                    RailwayMap railwayMap = new RailwayMap(regionPos);
                    railwayMap.startPlanningRoutes(level);
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
}
