/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.planner;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.planner.RoutePlanner.RegionPos;
import com.skua.createrailsprawl.structure.ModStructureManager;
import com.skua.createrailsprawl.structure.StationTemplate;
import com.skua.createrailsprawl.util.MyMth;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static com.skua.createrailsprawl.planner.RoutePlanner.CHUNK_GROUP_SIZE;

/**
 * 车站规划器
 * 负责车站位置生成、出口方向分配、区域间连接规划
 */
public class StationPlanner {
    private final RegionPos regionPos;

    public StationPlanner(RegionPos regionPos) {
        this.regionPos = regionPos;
    }

    /**
     * 生成车站信息
     */
    public static List<StationGenInfo> generateStation(RegionPos regionPos, ServerLevel level, long seed) {
        return generateStationFromSeed(regionPos, seed);
    }

    /**
     * 生成车站信息（不依赖ServerLevel，可在异步线程调用）
     * 使用纯种子计算，高度使用默认值
     */
    public static List<StationGenInfo> generateStationFromSeed(RegionPos regionPos, long seed) {
        long regionSeed = seed + regionPos.hashCode();
        List<StationGenInfo> result = new ArrayList<>();

        // 使用确定性随机数生成车站位置
        int[] pos = generateStationPos(regionSeed, CHUNK_GROUP_SIZE);
        ChunkPos chunkPos = new ChunkPos(
                MyMth.chunkPosXFromRegionPos(regionPos, pos[0]),
                MyMth.chunkPosZFromRegionPos(regionPos, pos[1])
        );

        int x = chunkPos.getBlockX(0);
        int z = chunkPos.getBlockZ(0);

        // 使用种子计算伪随机高度（不依赖世界数据）
        Random heightRandom = new Random(regionSeed * 31 + x * 17 + z);
        int y = 64 + heightRandom.nextInt(30); // 64-94 范围内的高度

        // 根据种子决定生成地上还是地下车站
        boolean isUnderground = heightRandom.nextFloat() < 0.3f; // 30%概率地下
        StationTemplate station;
        if (isUnderground) {
            station = ModStructureManager.getRandomUndergroundStation(regionSeed);
        } else {
            station = ModStructureManager.getRandomNormalStation(regionSeed);
        }

        result.add(new StationGenInfo(station, new BlockPos(x, y, z), isUnderground));

        return result;
    }

    /**
     * 生成区域间连接
     */
    public List<ConnectionGenInfo> generateConnections(ServerLevel level, long seed) {
        return generateConnectionsFromSeed(seed, level.getSeaLevel());
    }

    /**
     * 生成区域间连接（不依赖ServerLevel，可在异步线程调用）
     */
    public List<ConnectionGenInfo> generateConnectionsFromSeed(long seed, int seaLevel) {
        List<ConnectionGenInfo> result = new ArrayList<>();

        List<StationGenInfo> thisStations = generateStationFromSeed(regionPos, seed);
        if (thisStations.isEmpty()) return result;

        // 获取相邻区域的车站
        List<StationGenInfo> north = generateStationFromSeed(new RegionPos(regionPos.x(), regionPos.z() - 1), seed);
        List<StationGenInfo> south = generateStationFromSeed(new RegionPos(regionPos.x(), regionPos.z() + 1), seed);
        List<StationGenInfo> east = generateStationFromSeed(new RegionPos(regionPos.x() + 1, regionPos.z()), seed);
        List<StationGenInfo> west = generateStationFromSeed(new RegionPos(regionPos.x() - 1, regionPos.z()), seed);

        StationGenInfo thisStation = thisStations.get(0);
        BlockPos thisPos = thisStation.placePos();

        // 生成四个方向的连接
        // 东向连接
        if (!east.isEmpty()) {
            BlockPos eastPos = east.get(0).placePos();
            result.add(createConnection(thisPos, eastPos, new Vec3(1, 0, 0)));
        }

        // 西向连接
        if (!west.isEmpty()) {
            BlockPos westPos = west.get(0).placePos();
            result.add(createConnection(westPos, thisPos, new Vec3(1, 0, 0)));
        }

        // 南向连接
        if (!south.isEmpty()) {
            BlockPos southPos = south.get(0).placePos();
            result.add(createConnection(thisPos, southPos, new Vec3(0, 0, 1)));
        }

        // 北向连接
        if (!north.isEmpty()) {
            BlockPos northPos = north.get(0).placePos();
            result.add(createConnection(northPos, thisPos, new Vec3(0, 0, 1)));
        }

        CreateRailsprawl.LOGGER.debug("区域 {} 生成 {} 个连接", regionPos, result.size());

        return result;
    }

    /**
     * 创建连接信息
     */
    private ConnectionGenInfo createConnection(BlockPos startPos, BlockPos endPos, Vec3 exitDir) {
        Vec3 start = Vec3.atCenterOf(startPos);
        Vec3 end = Vec3.atCenterOf(endPos);

        Vec3 startDir = exitDir;
        Vec3 endDir = exitDir.reverse();

        // 计算寻路起点和终点（偏移30格）
        int[] connectStart = new int[]{
                (int) (start.x + exitDir.x * 40),
                (int) (start.z + exitDir.z * 40),
                startPos.getY()
        };
        int[] connectEnd = new int[]{
                (int) (end.x - exitDir.x * 40),
                (int) (end.z - exitDir.z * 40),
                endPos.getY()
        };

        return new ConnectionGenInfo(start, startDir, end, endDir, connectStart, connectEnd, exitDir);
    }

    /**
     * 确定性随机生成车站位置
     */
    private static int[] generateStationPos(long seed, int range) {
        Random random = new Random(seed);
        // 在区域中心附近生成（中心1/3区域内）
        int margin = range / 3;
        int x = margin + random.nextInt(range - 2 * margin);
        int z = margin + random.nextInt(range - 2 * margin);
        return new int[]{x, z};
    }

    // --- 内部记录类 ---

    /**
     * 车站生成信息
     */
    public record StationGenInfo(
            StationTemplate stationTemplate,
            BlockPos placePos,
            boolean isUnderground
    ) {
        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            if (stationTemplate != null) {
                tag.putInt("id", stationTemplate.getId());
                tag.putString("type", stationTemplate.getType().name());
            }
            tag.putInt("x", placePos.getX());
            tag.putInt("y", placePos.getY());
            tag.putInt("z", placePos.getZ());
            tag.putBoolean("underground", isUnderground);
            return tag;
        }

        public static StationGenInfo fromNBT(CompoundTag tag) {
            int x = tag.getInt("x");
            int y = tag.getInt("y");
            int z = tag.getInt("z");
            boolean underground = tag.getBoolean("underground");

            StationTemplate template = null;
            if (tag.contains("id")) {
                int id = tag.getInt("id");
                if (underground) {
                    template = ModStructureManager.undergroundStation.get(id);
                } else {
                    template = ModStructureManager.normalStation.get(id);
                }
            }

            return new StationGenInfo(template, new BlockPos(x, y, z), underground);
        }

        public static ListTag listToNBT(List<StationGenInfo> list) {
            ListTag listTag = new ListTag();
            for (StationGenInfo info : list) {
                listTag.add(info.toNBT());
            }
            return listTag;
        }

        public static List<StationGenInfo> listFromNBT(ListTag listTag) {
            List<StationGenInfo> list = new ArrayList<>();
            for (int i = 0; i < listTag.size(); i++) {
                list.add(fromNBT(listTag.getCompound(i)));
            }
            return list;
        }
    }

    /**
     * 路线连接信息
     */
    public record ConnectionGenInfo(
            Vec3 start,           // 起点坐标
            Vec3 startDir,        // 起点方向
            Vec3 end,             // 终点坐标
            Vec3 endDir,          // 终点方向
            int[] connectStart,   // 寻路起点 [x, z, y]
            int[] connectEnd,     // 寻路终点 [x, z, y]
            Vec3 exitDir          // 出口方向
    ) {
        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("sx", start.x);
            tag.putDouble("sy", start.y);
            tag.putDouble("sz", start.z);
            tag.putDouble("sdx", startDir.x);
            tag.putDouble("sdy", startDir.y);
            tag.putDouble("sdz", startDir.z);
            tag.putDouble("ex", end.x);
            tag.putDouble("ey", end.y);
            tag.putDouble("ez", end.z);
            tag.putDouble("edx", endDir.x);
            tag.putDouble("edy", endDir.y);
            tag.putDouble("edz", endDir.z);
            tag.putIntArray("cs", connectStart);
            tag.putIntArray("ce", connectEnd);
            tag.putDouble("exitX", exitDir.x);
            tag.putDouble("exitY", exitDir.y);
            tag.putDouble("exitZ", exitDir.z);
            return tag;
        }

        public static ConnectionGenInfo fromNBT(CompoundTag tag) {
            return new ConnectionGenInfo(
                    new Vec3(tag.getDouble("sx"), tag.getDouble("sy"), tag.getDouble("sz")),
                    new Vec3(tag.getDouble("sdx"), tag.getDouble("sdy"), tag.getDouble("sdz")),
                    new Vec3(tag.getDouble("ex"), tag.getDouble("ey"), tag.getDouble("ez")),
                    new Vec3(tag.getDouble("edx"), tag.getDouble("edy"), tag.getDouble("edz")),
                    tag.getIntArray("cs"),
                    tag.getIntArray("ce"),
                    new Vec3(tag.getDouble("exitX"), tag.getDouble("exitY"), tag.getDouble("exitZ"))
            );
        }
    }
}
