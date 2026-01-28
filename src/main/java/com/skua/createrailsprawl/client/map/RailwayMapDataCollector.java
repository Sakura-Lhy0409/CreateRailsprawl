package com.skua.createrailsprawl.client.map;

import com.skua.createrailsprawl.railway.RailwayMap;
import com.skua.createrailsprawl.railway.RegionPos;
import com.skua.createrailsprawl.railway.planner.StationPlanner;
import com.skua.createrailsprawl.util.CurveRoute;
import com.skua.createrailsprawl.util.ModSaveData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static com.skua.createrailsprawl.CreateRailsprawl.CHUNK_GROUP_SIZE;

/**
 * 铁路网地图数据收集器 - 从服务端收集数据构建快照
 */
public final class RailwayMapDataCollector {
    private RailwayMapDataCollector() {}

    public static RailwayMapSnapshot build(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        int radiusBlocks = 2048;
        return build(level, spawn.getX(), spawn.getZ(), radiusBlocks, true);
    }

    public static RailwayMapSnapshot build(ServerLevel level, int centerX, int centerZ, int radiusBlocks, boolean onlyLoaded) {
        int minBlockX = centerX - radiusBlocks;
        int maxBlockX = centerX + radiusBlocks;
        int minBlockZ = centerZ - radiusBlocks;
        int maxBlockZ = centerZ + radiusBlocks;
        ModSaveData saveData = ModSaveData.get(level);
        
        List<RailwayMapSnapshot.StationInfo> stations = new ArrayList<>();
        List<RailwayMapSnapshot.ConnectionInfo> connections = new ArrayList<>();
        List<List<BlockPos>> railPolylines = new ArrayList<>();
        
        Set<Long> seenStations = new HashSet<>();
        Set<Long> seenConnections = new HashSet<>();

        // 计算需要查询的区域范围
        int minRegionX = Math.floorDiv(minBlockX, CHUNK_GROUP_SIZE * 16);
        int maxRegionX = Math.floorDiv(maxBlockX, CHUNK_GROUP_SIZE * 16);
        int minRegionZ = Math.floorDiv(minBlockZ, CHUNK_GROUP_SIZE * 16);
        int maxRegionZ = Math.floorDiv(maxBlockZ, CHUNK_GROUP_SIZE * 16);

        for (int rx = minRegionX; rx <= maxRegionX; rx++) {
            for (int rz = minRegionZ; rz <= maxRegionZ; rz++) {
                RegionPos regionPos = new RegionPos(rx, rz);
                RailwayMap railwayMap = saveData.getRailwayMap(regionPos);
                if (railwayMap == null) continue;

                // 收集车站
                for (StationPlanner.StationGenInfo station : railwayMap.stations) {
                    BlockPos pos = station.placePos();
                    if (pos.getX() < minBlockX || pos.getX() > maxBlockX ||
                        pos.getZ() < minBlockZ || pos.getZ() > maxBlockZ) continue;
                    if (onlyLoaded && !isChunkLoaded(level, pos.getX(), pos.getZ())) continue;
                    
                    long key = posKey(pos);
                    if (seenStations.add(key)) {
                        String name = station.stationTemplate() != null ? station.stationTemplate().getType().name() : null;
                        stations.add(new RailwayMapSnapshot.StationInfo(pos, name));
                    }
                }

                // 收集路线折线
                Set<CurveRoute> processedRoutes = new HashSet<>();
                railwayMap.routeMap.forEach((chunkPos, routes) -> {
                    if (onlyLoaded && !level.hasChunk(chunkPos.x, chunkPos.z)) return;
                    for (CurveRoute route : routes) {
                        if (processedRoutes.contains(route)) continue;
                        processedRoutes.add(route);
                        
                        List<BlockPos> polyline = new ArrayList<>();
                        int rasterScale = 32;
                        for (CurveRoute.CurveSegment segment : route.getSegments()) {
                            for (Vec3 p : segment.rasterize(rasterScale)) {
                                int x = (int) Math.round(p.x * rasterScale);
                                int z = (int) Math.round(p.z * rasterScale);
                                if (x >= minBlockX && x <= maxBlockX && z >= minBlockZ && z <= maxBlockZ) {
                                    if (onlyLoaded && !isChunkLoaded(level, x, z)) continue;
                                    polyline.add(new BlockPos(x, (int) p.y, z));
                                }
                            }
                        }
                        if (polyline.size() >= 2) {
                            railPolylines.add(polyline);
                        }
                    }
                });
            }
        }

        List<RailwayMapSnapshot.StationInfo> stationList = new ArrayList<>(stations);
        for (List<BlockPos> polyline : railPolylines) {
            BlockPos start = polyline.get(0);
            BlockPos end = polyline.get(polyline.size() - 1);
            BlockPos from = findNearestStation(start, stationList, 96);
            BlockPos to = findNearestStation(end, stationList, 96);
            if (from == null || to == null || from.equals(to)) continue;
            long connKey = connectionKey(from, to);
            if (seenConnections.add(connKey)) {
                connections.add(new RailwayMapSnapshot.ConnectionInfo(from, to, RailwayMapSnapshot.ConnectionStatus.COMPLETED));
            }
        }

        return new RailwayMapSnapshot(stations, connections, railPolylines);
    }

    private static long posKey(BlockPos pos) {
        return ((long) pos.getX() << 32) | (pos.getZ() & 0xFFFFFFFFL);
    }

    private static long connectionKey(BlockPos a, BlockPos b) {
        long ka = posKey(a);
        long kb = posKey(b);
        return ka < kb ? (ka * 31 + kb) : (kb * 31 + ka);
    }

    private static boolean isChunkLoaded(ServerLevel level, int blockX, int blockZ) {
        int chunkX = Math.floorDiv(blockX, 16);
        int chunkZ = Math.floorDiv(blockZ, 16);
        return level.hasChunk(chunkX, chunkZ);
    }

    private static BlockPos findNearestStation(BlockPos pos, List<RailwayMapSnapshot.StationInfo> stations, int maxDistance) {
        if (stations.isEmpty()) return null;
        double best = (double) maxDistance * maxDistance;
        BlockPos bestPos = null;
        for (RailwayMapSnapshot.StationInfo s : stations) {
            double d = s.pos().distSqr(pos);
            if (d <= best) {
                best = d;
                bestPos = s.pos();
            }
        }
        return bestPos;
    }
}
