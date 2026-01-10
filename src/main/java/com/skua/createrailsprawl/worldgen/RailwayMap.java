/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.worldgen;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.planner.RoutePlanner;
import com.skua.createrailsprawl.planner.RoutePlanner.RegionPos;
import com.skua.createrailsprawl.planner.StationPlanner;
import com.skua.createrailsprawl.planner.StationPlanner.ConnectionGenInfo;
import com.skua.createrailsprawl.planner.StationPlanner.StationGenInfo;
import com.skua.createrailsprawl.structure.TrackPutInfo;
import com.skua.createrailsprawl.util.AStarPathfinder;
import com.skua.createrailsprawl.util.CurveRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.skua.createrailsprawl.planner.RoutePlanner.CHUNK_GROUP_SIZE;

/**
 * 区域铁路数据
 * 存储每个区域的路线、车站和轨道信息
 */
public class RailwayMap {
    public static final int SAMPLING_NUM = 2;

    public final RegionPos regionPos;
    public final long seed;

    // 每个区块的路线（用于路基生成）
    public final Map<ChunkPos, Set<CurveRoute>> routeMap = new ConcurrentHashMap<>();
    // 每个区块的轨道放置信息
    public final Map<ChunkPos, List<TrackPutInfo>> trackMap = new ConcurrentHashMap<>();
    // 车站信息
    public final List<StationInfo> stations = Collections.synchronizedList(new ArrayList<>());

    private volatile boolean planned = false;

    public RailwayMap(RegionPos regionPos, long seed) {
        this.regionPos = regionPos;
        this.seed = seed;
    }

    /**
     * 规划铁路路线（参考 TongDaRailway）
     * 使用 WorldGenRegion 获取高度图和结构图
     */
    public void startPlanningRoutes(WorldGenRegion level) {
        if (planned) return;

        try {
            ServerLevel serverLevel = level.getLevel();

            // 创建路线规划器
            RoutePlanner routePlanner = new RoutePlanner(serverLevel, regionPos);
            int[][] costMap = routePlanner.getCostMap();
            int[][] structureCostMap = routePlanner.getStructureCostMap();

            // 生成车站位置和连接规划
            List<StationGenInfo> stationInfos = StationPlanner.generateStation(regionPos, serverLevel, seed);
            for (StationGenInfo info : stationInfos) {
                stations.add(new StationInfo(info.placePos(), "Station_" + regionPos.x() + "_" + regionPos.z()));
            }

            StationPlanner stationPlanner = new StationPlanner(regionPos);
            List<ConnectionGenInfo> connections = stationPlanner.generateConnections(serverLevel, seed);

            // 对每个连接进行A*寻路和路线规划
            for (ConnectionGenInfo connection : connections) {
                try {
                    // 转换为图片坐标
                    int centerX = regionPos.x() * CHUNK_GROUP_SIZE * 16;
                    int centerZ = regionPos.z() * CHUNK_GROUP_SIZE * 16;
                    int[] picStart = AStarPathfinder.worldToImagePos(
                            connection.connectStart()[0], connection.connectStart()[1],
                            centerX, centerZ, RoutePlanner.SAMPLING_NUM);
                    int[] picEnd = AStarPathfinder.worldToImagePos(
                            connection.connectEnd()[0], connection.connectEnd()[1],
                            centerX, centerZ, RoutePlanner.SAMPLING_NUM);

                    // A*寻路
                    List<int[]> path = AStarPathfinder.findPath(costMap, picStart, picEnd,
                            (x, y) -> {
                                int heightLimit = costMap[x][y] < level.getSeaLevel() + 2 ? 100 : 0;
                                int structLimit = structureCostMap[x][y];
                                return heightLimit + structLimit;
                            });

                    if (!path.isEmpty()) {
                        // 规划路线
                        BlockPos startPos = new BlockPos(connection.connectStart()[0], connection.connectStart()[2], connection.connectStart()[1]);
                        BlockPos endPos = new BlockPos(connection.connectEnd()[0], connection.connectEnd()[2], connection.connectEnd()[1]);

                        RoutePlanner.PlanResult result = routePlanner.planRoute(startPos, endPos);
                        if (result != null && !result.isEmpty()) {
                            putChunk(result);
                        }
                    }
                } catch (Exception e) {
                    CreateRailsprawl.LOGGER.debug("连接规划失败: {}", e.getMessage());
                }
            }

            planned = true;
            CreateRailsprawl.LOGGER.debug("区域 {} 规划完成，生成 {} 条路线", regionPos, routeMap.size());

        } catch (Exception e) {
            CreateRailsprawl.LOGGER.error("区域 {} 规划失败", regionPos, e);
        }
    }

    /**
     * 将路线分配到各区块
     */
    private void putChunk(RoutePlanner.PlanResult result) {
        CurveRoute curve = result.curve();

        // 将曲线分配到经过的区块
        for (CurveRoute.CurveSegment segment : curve.getSegments()) {
            for (var p : segment.rasterize(16)) {
                for (int i = -1; i < 2; i++) {
                    for (int j = -1; j < 2; j++) {
                        int cx = (int) Math.floor(p.x / 16) + i;
                        int cz = (int) Math.floor(p.z / 16) + j;
                        routeMap.computeIfAbsent(new ChunkPos(cx, cz), k -> ConcurrentHashMap.newKeySet())
                                .add(curve);
                    }
                }
            }
        }

        // 将轨道放置信息分配到各区块
        for (var placement : result.placements()) {
            BlockPos pos = placement.pos();
            ChunkPos chunkPos = new ChunkPos(pos);

            TrackPutInfo trackInfo = new TrackPutInfo(
                    pos,
                    com.simibubi.create.content.trains.track.TrackShape.ZO,
                    placement.bezierInfo() != null ?
                            new TrackPutInfo.BezierInfo(
                                    placement.bezierInfo().start(),
                                    placement.bezierInfo().startDir(),
                                    placement.bezierInfo().endOffset(),
                                    placement.bezierInfo().endDir()
                            ) : null,
                    com.simibubi.create.content.trains.track.TrackShape.ZO
            );
            trackMap.computeIfAbsent(chunkPos, k -> Collections.synchronizedList(new ArrayList<>())).add(trackInfo);
        }
    }

    public boolean isPlanned() {
        return planned;
    }

    // --- NBT序列化 ---

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("regionX", regionPos.x());
        tag.putInt("regionZ", regionPos.z());
        tag.putLong("seed", seed);
        tag.putBoolean("planned", planned);

        // 保存轨道信息
        CompoundTag tracksTag = new CompoundTag();
        trackMap.forEach((chunkPos, tracks) -> {
            String key = chunkPos.x + "," + chunkPos.z;
            tracksTag.put(key, TrackPutInfo.listToNBT(tracks));
        });
        tag.put("tracks", tracksTag);

        // 保存车站信息
        ListTag stationsTag = new ListTag();
        for (StationInfo station : stations) {
            stationsTag.add(station.toNBT());
        }
        tag.put("stations", stationsTag);

        return tag;
    }

    public static RailwayMap fromNBT(CompoundTag tag) {
        RegionPos regionPos = new RegionPos(tag.getInt("regionX"), tag.getInt("regionZ"));
        long seed = tag.getLong("seed");
        RailwayMap map = new RailwayMap(regionPos, seed);
        map.planned = tag.getBoolean("planned");

        // 加载轨道信息
        CompoundTag tracksTag = tag.getCompound("tracks");
        for (String key : tracksTag.getAllKeys()) {
            String[] parts = key.split(",");
            ChunkPos chunkPos = new ChunkPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            List<TrackPutInfo> tracks = TrackPutInfo.listFromNBT(tracksTag.getList(key, 10));
            map.trackMap.put(chunkPos, Collections.synchronizedList(new ArrayList<>(tracks)));
        }

        // 加载车站信息
        ListTag stationsTag = tag.getList("stations", 10);
        for (int i = 0; i < stationsTag.size(); i++) {
            map.stations.add(StationInfo.fromNBT(stationsTag.getCompound(i)));
        }

        return map;
    }

    /**
     * 车站信息
     */
    public record StationInfo(BlockPos pos, String name) {
        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("x", pos.getX());
            tag.putInt("y", pos.getY());
            tag.putInt("z", pos.getZ());
            tag.putString("name", name);
            return tag;
        }

        public static StationInfo fromNBT(CompoundTag tag) {
            return new StationInfo(
                    new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z")),
                    tag.getString("name")
            );
        }
    }
}
