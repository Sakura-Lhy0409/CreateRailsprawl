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
        return build(level, spawn.getX() - radiusBlocks, spawn.getZ() - radiusBlocks,
                spawn.getX() + radiusBlocks, spawn.getZ() + radiusBlocks);
    }

    public static RailwayMapSnapshot build(ServerLevel level, int minBlockX, int minBlockZ, int maxBlockX, int maxBlockZ) {
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
                    
                    long key = posKey(pos);
                    if (seenStations.add(key)) {
                        String name = station.stationTemplate() != null ? station.stationTemplate().getType().name() : null;
                        stations.add(new RailwayMapSnapshot.StationInfo(pos, name));
                    }
                }

                // 收集路线折线
                Set<CurveRoute> processedRoutes = new HashSet<>();
                railwayMap.routeMap.forEach((chunkPos, routes) -> {
                    for (CurveRoute route : routes) {
                        if (processedRoutes.contains(route)) continue;
                        processedRoutes.add(route);
                        
                        List<BlockPos> polyline = new ArrayList<>();
                        for (CurveRoute.CurveSegment segment : route.getSegments()) {
                            for (Vec3 p : segment.rasterize(32)) {
                                int x = (int) Math.round(p.x * 16);
                                int z = (int) Math.round(p.z * 16);
                                if (x >= minBlockX && x <= maxBlockX && z >= minBlockZ && z <= maxBlockZ) {
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

        // 生成连接信息（从车站列表推导）
        List<RailwayMapSnapshot.StationInfo> stationList = new ArrayList<>(stations);
        for (int i = 0; i < stationList.size(); i++) {
            for (int j = i + 1; j < stationList.size(); j++) {
                BlockPos from = stationList.get(i).pos();
                BlockPos to = stationList.get(j).pos();
                double dist = Math.sqrt(from.distSqr(to));
                if (dist < 3000) { // 只显示3000格内的连接
                    long connKey = connectionKey(from, to);
                    if (seenConnections.add(connKey)) {
                        // 检查是否有对应的路线
                        boolean hasRoute = !railPolylines.isEmpty();
                        RailwayMapSnapshot.ConnectionStatus status = hasRoute ? 
                            RailwayMapSnapshot.ConnectionStatus.COMPLETED : 
                            RailwayMapSnapshot.ConnectionStatus.PLANNED;
                        connections.add(new RailwayMapSnapshot.ConnectionInfo(from, to, status));
                    }
                }
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
}
