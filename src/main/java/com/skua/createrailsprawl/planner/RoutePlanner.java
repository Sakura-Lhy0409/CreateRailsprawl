/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.planner;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.util.AdaptiveHeightSampler;
import com.skua.createrailsprawl.util.AStarPathfinder;
import com.skua.createrailsprawl.util.CurveRoute;
import com.skua.createrailsprawl.util.MyMth;
import com.skua.createrailsprawl.worldgen.RailwayBuilder;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.math.AngleHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 区域化路线规划器
 * 以128区块为单位进行大区域规划
 * 支持高度图预计算、A*寻路、贝塞尔曲线生成
 */
public class RoutePlanner {
    // 区块组大小（128区块 = 2048格）
    public static final int CHUNK_GROUP_SIZE = 128;
    // 采样密度（每16格采样一次）
    public static final int SAMPLING_NUM = 8;
    // 最大高度增量
    public static final int HEIGHT_MAX_INCREMENT = 64;

    // 静态缓存（跨实例共享）
    private static final Map<RegionPos, int[][]> regionHeightMapCache = new ConcurrentHashMap<>();
    private static final Map<RegionPos, int[][]> regionStructureMapCache = new ConcurrentHashMap<>();

    private final RegionPos regionPos;
    private final ServerLevel level;


    public RoutePlanner(ServerLevel level, RegionPos regionPos) {
        this.level = level;
        this.regionPos = regionPos;
    }

    /**
     * 获取代价地图（高度图）
     * 包含当前区域及相邻区域
     */
    public int[][] getCostMap() {
        int size = CHUNK_GROUP_SIZE * SAMPLING_NUM * 3;
        int[][] heightMap = new int[size][size];
        for (int[] ints : heightMap) {
            Arrays.fill(ints, 50000);
        }

        // 加载当前区域及相邻区域的高度图
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (Math.abs(i) == 1 && Math.abs(j) == 1) continue; // 跳过对角

                RegionPos rPos = new RegionPos(regionPos.x() + i, regionPos.z() + j);
                int[][] map = regionHeightMapCache.computeIfAbsent(rPos, k -> getHeightMap(rPos));

                for (int x = 0; x < map.length; x++) {
                    for (int z = 0; z < map[0].length; z++) {
                        int picX = (i + 1) * CHUNK_GROUP_SIZE * SAMPLING_NUM + x;
                        int picZ = (j + 1) * CHUNK_GROUP_SIZE * SAMPLING_NUM + z;
                        if (picX < size && picZ < size) {
                            heightMap[picX][picZ] = map[x][z];
                        }
                    }
                }
            }
        }

        return heightMap;
    }

    /**
     * 获取结构代价地图
     * 用于避开村庄、神殿等结构
     */
    public int[][] getStructureCostMap() {
        int size = CHUNK_GROUP_SIZE * SAMPLING_NUM * 3;
        int[][] structureMap = new int[size][size];

        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (Math.abs(i) == 1 && Math.abs(j) == 1) continue;

                RegionPos rPos = new RegionPos(regionPos.x() + i, regionPos.z() + j);
                int[][] map = regionStructureMapCache.computeIfAbsent(rPos, k -> getStructureMap(rPos));

                for (int x = 0; x < map.length; x++) {
                    for (int z = 0; z < map[0].length; z++) {
                        int picX = (i + 1) * CHUNK_GROUP_SIZE * SAMPLING_NUM + x;
                        int picZ = (j + 1) * CHUNK_GROUP_SIZE * SAMPLING_NUM + z;
                        if (picX < size && picZ < size) {
                            structureMap[picX][picZ] = map[x][z];
                        }
                    }
                }
            }
        }

        return structureMap;
    }

    /**
     * 使用自适应采样器生成高度图
     */
    private int[][] getHeightMap(RegionPos regionPos) {
        AdaptiveHeightSampler sampler = new AdaptiveHeightSampler(10, 3, 4, (x, z) -> {
            int wx = (int) (x * (16.0 / SAMPLING_NUM) + regionPos.x() * CHUNK_GROUP_SIZE * 16);
            int wz = (int) (z * (16.0 / SAMPLING_NUM) + regionPos.z() * CHUNK_GROUP_SIZE * 16);
            return level.getChunkSource().getGenerator().getBaseHeight(
                    wx, wz, Heightmap.Types.WORLD_SURFACE_WG, level, level.getChunkSource().randomState());
        });

        try {
            long startTime = System.currentTimeMillis();
            sampler.buildQuadTree(CHUNK_GROUP_SIZE * SAMPLING_NUM);
            long endTime = System.currentTimeMillis();
            CreateRailsprawl.LOGGER.debug("构建高度图耗时: {}ms", endTime - startTime);
        } catch (InterruptedException e) {
            CreateRailsprawl.LOGGER.error("构建高度图被中断", e);
        } finally {
            sampler.shutdown();
        }

        return sampler.generateImage(CHUNK_GROUP_SIZE * SAMPLING_NUM, CHUNK_GROUP_SIZE * SAMPLING_NUM);
    }

    /**
     * 生成结构避让图
     * 检测村庄、神殿等结构，在周围设置高代价区域
     */
    private int[][] getStructureMap(RegionPos regionPos) {
        var registryAccess = level.registryAccess();
        var chunkGeneratorStructureState = level.getChunkSource().getGeneratorState();
        var structureManager = level.structureManager();
        var structureFeatureManager = level.getStructureManager();

        var dimensionType = level.dimensionType();
        LevelHeightAccessor levelHeightAccessor = LevelHeightAccessor.create(dimensionType.minY(), dimensionType.height());
        var biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);

        List<BlockPos> structurePosList = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(16);
        try {
            CountDownLatch latch = new CountDownLatch(CHUNK_GROUP_SIZE * CHUNK_GROUP_SIZE);

            for (int gx = 0; gx < CHUNK_GROUP_SIZE; gx++) {
                for (int gz = 0; gz < CHUNK_GROUP_SIZE; gz++) {
                    int finalGx = gx;
                    int finalGz = gz;
                    executor.execute(() -> {
                        try {
                            ChunkPos chunkPos = new ChunkPos(
                                    regionPos.x() * CHUNK_GROUP_SIZE + finalGx,
                                    regionPos.z() * CHUNK_GROUP_SIZE + finalGz
                            );
                            var protoChunk = new ProtoChunk(chunkPos, UpgradeData.EMPTY, levelHeightAccessor, biomeRegistry, null);

                            // 检测结构
                            level.getChunkSource().getGenerator().createStructures(
                                    registryAccess, chunkGeneratorStructureState, structureManager, protoChunk, structureFeatureManager
                            );

                            var res = protoChunk.getAllStarts();
                            res.forEach((key, value) -> {
                                BlockPos pos = new BlockPos(chunkPos.x * 16, 0, chunkPos.z * 16);
                                structurePosList.add(pos);
                            });
                        } catch (Exception e) {
                            CreateRailsprawl.LOGGER.debug("结构检测异常: {}", e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    });
                }
            }

            latch.await();
            executor.shutdown();
        } catch (InterruptedException e) {
            CreateRailsprawl.LOGGER.error("结构检测被中断", e);
        }

        // 生成代价图
        int[][] costMap = new int[CHUNK_GROUP_SIZE * SAMPLING_NUM][CHUNK_GROUP_SIZE * SAMPLING_NUM];
        for (BlockPos pos : structurePosList) {
            int[] p = new int[]{
                    (pos.getX() - regionPos.x() * CHUNK_GROUP_SIZE * 16) * SAMPLING_NUM / 16,
                    (pos.getZ() - regionPos.z() * CHUNK_GROUP_SIZE * 16) * SAMPLING_NUM / 16
            };
            // 在结构周围5区块范围内设置高代价
            for (int x = -5 * SAMPLING_NUM; x < 5 * SAMPLING_NUM; x++) {
                for (int z = -5 * SAMPLING_NUM; z < 5 * SAMPLING_NUM; z++) {
                    int px = p[0] + x;
                    int pz = p[1] + z;
                    if (px > 0 && px < costMap.length && pz > 0 && pz < costMap[0].length) {
                        costMap[px][pz] = 500;
                    }
                }
            }
        }

        CreateRailsprawl.LOGGER.debug("区域 {} 检测到 {} 个结构", regionPos, structurePosList.size());
        return costMap;
    }

    /**
     * 规划路线
     * @param start 起点（世界坐标）
     * @param end 终点（世界坐标）
     * @return 规划结果
     */
    public PlanResult planRoute(BlockPos start, BlockPos end) {
        int[][] costMap = getCostMap();
        int[][] structureMap = getStructureCostMap();

        // 转换为图片坐标
        int[] startImg = worldToImagePos(start.getX(), start.getZ());
        int[] endImg = worldToImagePos(end.getX(), end.getZ());

        // A*寻路
        List<int[]> path = AStarPathfinder.findPath(costMap, startImg, endImg,
                (x, y) -> structureMap[x][y]);

        if (path.isEmpty()) {
            CreateRailsprawl.LOGGER.warn("无法找到从 {} 到 {} 的路径", start, end);
            return null;
        }

        // 处理高度
        List<int[]> heightAdjustedPath = handleHeight(path, costMap, start.getY(), end.getY());

        // 生成曲线
        return connectTrack(heightAdjustedPath);
    }

    /**
     * 处理路径高度
     * 使用智能算法判断桥梁/隧道
     */
    private List<int[]> handleHeight(List<int[]> path, int[][] heightMap, int startY, int endY) {
        List<double[]> adPath = new LinkedList<>();
        int seaLevel = level.getSeaLevel();

        // 第一步：限制高度范围
        for (int[] p : path) {
            int h = heightMap[p[0]][p[1]];
            h = Math.max(h, seaLevel + 5);
            h = Math.min(h, seaLevel + HEIGHT_MAX_INCREMENT);
            adPath.add(new double[]{p[0], p[1], h});
        }

        // 设置起点和终点高度
        if (!adPath.isEmpty()) {
            adPath.get(0)[2] = startY;
            adPath.get(adPath.size() - 1)[2] = endY;
        }

        // 第二步：智能高度调整（桥梁/隧道判断）
        adPath = adjustmentHeight(adPath);

        // 第三步：滑动窗口平滑
        int max = adPath.stream().mapToInt(p -> (int) p[2]).max().orElse(0);
        int min = adPath.stream().mapToInt(p -> (int) p[2]).min().orElse(0);
        int framed2 = ((max - min) / 4) + 1;

        if (adPath.size() > framed2 * 2 && framed2 * 2 >= 3) {
            // 平滑首末
            double fh = startY;
            double lh = endY;
            if (adPath.size() > framed2 * 2 + 20) {
                for (int i = 1; i < framed2 + 10; i++) {
                    double t = (double) i / (framed2 + 10);
                    double sh = adPath.get(i)[2];
                    double eh = adPath.get(adPath.size() - 1 - i)[2];

                    adPath.get(i)[2] = fh * (1 - t) + sh * t;
                    adPath.get(adPath.size() - 1 - i)[2] = lh * (1 - t) + eh * t;
                }
            }

            // 平滑中间
            List<double[]> adPath1 = new ArrayList<>();
            adPath1.add(adPath.get(0));
            for (int i = 1; i < adPath.size() - 1; i++) {
                double mean = 0;
                int sum = 0;
                for (int j = i - framed2; j <= i + framed2; j++) {
                    if (j >= 0 && j < adPath.size()) {
                        mean += adPath.get(j)[2];
                        sum++;
                    } else if (j < 0) {
                        mean += adPath.get(0)[2];
                        sum++;
                    } else {
                        mean += adPath.get(adPath.size() - 1)[2];
                        sum++;
                    }
                }
                mean /= sum;
                adPath1.add(new double[]{adPath.get(i)[0], adPath.get(i)[1], mean});
            }
            adPath1.add(adPath.get(adPath.size() - 1));
            adPath = adPath1;
        }

        return adPath.stream()
                .map(arr -> Arrays.stream(arr)
                        .mapToInt(d -> (int) Math.round(d))
                        .toArray()
                )
                .collect(Collectors.toList());
    }

    /**
     * 智能高度调整算法
     * 根据地形自动判断是否需要桥梁或隧道
     */
    private static List<double[]> adjustmentHeight(List<double[]> path) {
        List<double[]> adjustedPath = new ArrayList<>();
        if (path.size() < 2)
            return new LinkedList<>();

        double hStart = path.get(0)[2];
        double hEnd = path.get(path.size() - 1)[2];
        double pNum = path.size() - 1;

        // 计算相对高度
        List<double[]> heightList0 = new ArrayList<>();
        Map<Integer, List<double[]>> heightGroups = new HashMap<>();
        double distance = 0;

        for (int i = 0; i < path.size(); i++) {
            double[] point = path.get(i);
            double h = point[2] - hStart * ((pNum - i) / pNum) - hEnd * (i / pNum);

            if (i > 0) {
                double h0 = point[2];
                double h1 = path.get(i - 1)[2];
                distance += 1 + Math.abs(h0 - h1);
            }

            double[] p = {point[0], point[1], h, i, distance};
            heightList0.add(p);

            int hi = (int) h;
            heightGroups.computeIfAbsent(hi, k -> new ArrayList<>()).add(p);
        }

        // 斜率系数
        double sec = Math.sqrt(Math.pow(heightList0.size(), 2) + Math.pow(Math.abs(hStart - hEnd), 2)) / (heightList0.size());

        // 遍历调整
        for (int j = 0; j < heightList0.size(); j++) {
            double[] thisPoint = heightList0.get(j);
            adjustedPath.add(new double[]{thisPoint[0], thisPoint[1], thisPoint[2]});

            int hd = 0;
            if (j < heightList0.size() - 1) {
                hd = (int) heightList0.get(j + 1)[2] - (int) thisPoint[2];
            }

            if (hd == 0)
                continue;

            double h = thisPoint[2];
            var group = heightGroups.get((int) h);
            if (group == null) continue;

            int groupIndex = group.indexOf(thisPoint);

            if (groupIndex < group.size() - 1) {
                double[] nextSameHeightPoint = group.get(groupIndex + 1);
                int nextPointIndex = heightList0.indexOf(nextSameHeightPoint);
                double dA = thisPoint[4], dB = nextSameHeightPoint[4];
                double iA = thisPoint[3], iB = nextSameHeightPoint[3];

                // 桥梁条件：下降且距离足够
                boolean conditionBridge = hd < 0 && (iB - iA) * 4 * sec < dB - dA;
                // 隧道条件：上升且距离足够
                boolean conditionTunnel = hd > 0 && (iB - iA) * 3 * sec < dB - dA;

                if (conditionBridge || conditionTunnel) {
                    // 保持高度
                    for (int k = j; k < nextPointIndex; k++) {
                        double[] np1 = heightList0.get(k + 1);
                        adjustedPath.add(new double[]{np1[0], np1[1], thisPoint[2]});
                    }
                    j = nextPointIndex;
                }
            }
        }

        // 将相对高度加回基线
        for (int i = 0; i < adjustedPath.size(); i++) {
            double[] p = adjustedPath.get(i);
            p[2] += hStart * ((pNum - i) / pNum) + hEnd * (i / pNum);
        }

        return adjustedPath;
    }

    /**
     * 将路径转换为曲线
     * 使用智能连接算法，根据角度选择最佳连接方式
     */
    private PlanResult connectTrack(List<int[]> path) {
        CurveRoute curve = new CurveRoute();
        List<TrackPlacement> placements = new ArrayList<>();

        if (path.size() < 2) return new PlanResult(curve, placements);

        // 转换为世界坐标
        List<Vec3> path0 = new ArrayList<>();
        for (int[] point : path) {
            path0.add(imageToWorldPos(point[0], point[1], point[2]));
        }

        // 每隔6个点取一个控制点，减少曲线段数量
        List<Vec3> controlPoints = new ArrayList<>();
        for (int i = 0; i < path0.size() - 10; i += 6) {
            controlPoints.add(path0.get(i));
        }

        // 添加中点确保平滑过渡到终点
        if (!controlPoints.isEmpty() && !path0.isEmpty()) {
            Vec3 a1 = controlPoints.get(controlPoints.size() - 1);
            Vec3 b1 = path0.get(path0.size() - 1);
            Vec3 c1 = a1.add(b1.subtract(a1).scale(0.5));
            controlPoints.add(new Vec3((int) c1.x(), (int) c1.y(), (int) c1.z()));
        }

        if (controlPoints.size() < 2) {
            // 路径太短，直接连接
            if (path0.size() >= 2) {
                Vec3 start = path0.get(0);
                Vec3 end = path0.get(path0.size() - 1);
                Vec3 dir = end.subtract(start).normalize();
                curve.addSegment(new CurveRoute.LineSegment(start, end));
                placements.add(new TrackPlacement(
                        new BlockPos((int) start.x, (int) start.y, (int) start.z),
                        dir, null));
            }
            return new PlanResult(curve, placements);
        }

        // 智能连接控制点
        Vec3 startDir = controlPoints.get(1).subtract(controlPoints.get(0)).multiply(1, 0, 1).normalize();

        for (int i = 0; i < controlPoints.size() - 1; i++) {
            Vec3 start = controlPoints.get(i);
            Vec3 end = controlPoints.get(i + 1);

            Vec3 nextDir = end.subtract(start).multiply(1, 0, 1).normalize();

            // 计算夹角
            double dot = startDir.dot(nextDir);
            double cross = startDir.x * nextDir.z - startDir.z * nextDir.x;

            Vec3 endDir;
            boolean maximiseTurn = Math.abs(start.y - end.y) < 1;

            if (dot > 0.9999) {
                // 几乎直线
                endDir = startDir.reverse();
                addBezierSegment(curve, placements, start, startDir, end.subtract(start), endDir);
            } else if (dot > 0.975) {
                // 前方平滑
                endDir = startDir.reverse();
                connectWay(curve, placements, start, end, startDir, endDir, maximiseTurn);
            } else if (dot > 0.75) {
                // 斜前方 135度钝角
                endDir = MyMth.rotateAroundY(startDir, cross, 45).reverse();
                connectWay(curve, placements, start, end, startDir, endDir, maximiseTurn);
            } else if (dot > 0.165) {
                // 侧前方 90度直角
                endDir = MyMth.rotateAroundY(startDir, cross, 90).reverse();
                connectWay(curve, placements, start, end, startDir, endDir, maximiseTurn);
            } else {
                // 侧方/后方 需要额外转折点
                endDir = MyMth.rotateAroundY(startDir, cross, 90).reverse();

                Vec3 d1 = new Vec3(MyMth.splitFunc(startDir.x), 0, MyMth.splitFunc(startDir.z));
                Vec3 d2 = new Vec3(MyMth.splitFunc(endDir.x), 0, MyMth.splitFunc(endDir.z)).reverse();
                Vec3 newPoint = start.add(d1.scale(8)).add(d2.scale(8));

                addBezierSegment(curve, placements, start, startDir, newPoint.subtract(start), endDir);

                startDir = endDir.reverse();
                // 插入新控制点
                controlPoints.add(i + 1, newPoint);
                continue;
            }

            startDir = endDir.reverse();
        }

        return new PlanResult(curve, placements);
    }

    /**
     * 智能连接两点
     */
    private void connectWay(CurveRoute curve, List<TrackPlacement> placements,
                            Vec3 start, Vec3 end, Vec3 startDir, Vec3 endDir, boolean maximiseTurn) {
        int h = (int) ((start.y + end.y) / 2);
        Vec3 s = new Vec3(start.x, h, start.z);
        Vec3 e = new Vec3(end.x, h, end.z);

        ConnectInfo connect = getConnect(BlockPos.containing(s), BlockPos.containing(e), startDir, endDir, maximiseTurn);

        if (connect != null) {
            if (connect.startExtent < 8)
                h = (int) start.y;
            else if (connect.endExtent < 8)
                h = (int) end.y;

            Vec3 conStart = new Vec3(connect.startPos.x, h, connect.startPos.z);
            Vec3 conEnd = new Vec3(connect.endPos.x, h, connect.endPos.z);

            if (connect.startExtent != 0) {
                addBezierSegment(curve, placements, start, startDir, conStart.subtract(start), startDir.reverse());
            }
            addBezierSegment(curve, placements, conStart, startDir, conEnd.subtract(conStart), endDir);
            if (connect.endExtent != 0) {
                addBezierSegment(curve, placements, conEnd, endDir.reverse(), end.subtract(conEnd), endDir);
            }
        } else {
            // 强制连接
            addBezierSegment(curve, placements, start, startDir, end.subtract(start), endDir);
            CreateRailsprawl.LOGGER.warn("无法确定连接位置，已强制连接: {} -> {}", start, end);
        }
    }

    /**
     * 获取连接信息
     */
    private static ConnectInfo getConnect(BlockPos pos1, BlockPos pos2, Vec3 axis1, Vec3 axis2, boolean maximiseTurn) {
        Vec3 normedAxis1 = axis1.normalize();
        Vec3 normedAxis2 = axis2.normalize();

        Vec3 end1 = MyMth.getCurveStart(pos1, axis1);
        Vec3 end2 = MyMth.getCurveStart(pos2, axis2);

        double[] intersect = VecHelper.intersect(end1, end2, normedAxis1, normedAxis2, Direction.Axis.Y);
        boolean parallel = intersect == null;
        boolean skipCurve = false;

        Vec3 cross2 = normedAxis2.cross(new Vec3(0, 1, 0));

        double a1 = Mth.atan2(normedAxis2.z, normedAxis2.x);
        double a2 = Mth.atan2(normedAxis1.z, normedAxis1.x);
        double angle = a1 - a2;
        double ascend = end2.subtract(end1).y;
        double absAscend = Math.abs(ascend);

        int end1Extent = 0;
        int end2Extent = 0;
        double dist = 0;

        // S曲线或直线
        if (parallel) {
            double[] sTest = VecHelper.intersect(end1, end2, normedAxis1, cross2, Direction.Axis.Y);
            if (sTest != null) {
                double t = Math.abs(sTest[0]);
                double u = Math.abs(sTest[1]);

                skipCurve = Mth.equal(u, 0);

                if (!skipCurve && sTest[0] < 0)
                    return new ConnectInfo(
                            new Vec3(pos1.getX(), pos1.getY(), pos1.getZ()), axis1,
                            new Vec3(pos2.getX(), pos2.getY(), pos2.getZ()), axis2,
                            end1Extent, end2Extent);

                if (skipCurve) {
                    dist = VecHelper.getCenterOf(pos1).distanceTo(VecHelper.getCenterOf(pos2));
                    end1Extent = (int) Math.round((dist + 1) / axis1.length());
                } else {
                    if (!Mth.equal(ascend, 0) || normedAxis1.y != 0)
                        return null;

                    double targetT = u <= 1 ? 3 : u * 2;
                    if (t < targetT)
                        return null;

                    if (t > targetT) {
                        int correction = (int) ((t - targetT) / axis1.length());
                        end1Extent = maximiseTurn ? 0 : correction / 2 + (correction % 2);
                        end2Extent = maximiseTurn ? 0 : correction / 2;
                    }
                }
            }
        }

        // 直线爬升
        if (skipCurve && !Mth.equal(ascend, 0)) {
            int hDistance = end1Extent;
            if (axis1.y == 0 || !Mth.equal(absAscend + 1, dist / axis1.length())) {
                if (axis1.y != 0 && axis1.y == -axis2.y)
                    return null;

                end1Extent = 0;
                double minHDistance = Math.max(absAscend < 4 ? absAscend * 4 : absAscend * 3, 6) / axis1.length();
                if (hDistance < minHDistance)
                    return null;
                if (hDistance > minHDistance) {
                    int correction = (int) (hDistance - minHDistance);
                    end1Extent = maximiseTurn ? 0 : correction / 2 + (correction % 2);
                    end2Extent = maximiseTurn ? 0 : correction / 2;
                }
                skipCurve = false;
            }
        }

        // 转弯
        if (!parallel) {
            float absAngle = Math.abs(AngleHelper.deg((float) angle));
            if (absAngle < 60 || absAngle > 300)
                return null;

            intersect = VecHelper.intersect(end1, end2, normedAxis1, normedAxis2, Direction.Axis.Y);
            if (intersect == null) return null;

            double dist1 = Math.abs(intersect[0]);
            double dist2 = Math.abs(intersect[1]);
            float ex1 = 0;
            float ex2 = 0;

            if (dist1 > dist2)
                ex1 = (float) ((dist1 - dist2) / axis1.length());
            if (dist2 > dist1)
                ex2 = (float) ((dist2 - dist1) / axis2.length());

            double turnSize = Math.min(dist1, dist2) - .1d;
            boolean ninety = (absAngle + .25f) % 90 < 1;

            if (intersect[0] < 0 || intersect[1] < 0)
                return null;

            double minTurnSize = ninety ? 7 : 3.25;
            double turnSizeToFitAscend = minTurnSize + (ninety ? Math.max(0, absAscend - 3) * 2f : Math.max(0, absAscend - 1.5f) * 1.5f);

            if (turnSize < minTurnSize || turnSize < turnSizeToFitAscend)
                return null;

            if (!maximiseTurn) {
                ex1 += (float) ((turnSize - turnSizeToFitAscend) / axis1.length());
                ex2 += (float) ((turnSize - turnSizeToFitAscend) / axis2.length());
            }
            end1Extent = Mth.floor(ex1);
            end2Extent = Mth.floor(ex2);
        }

        Vec3 offset1 = axis1.scale(end1Extent);
        Vec3 offset2 = axis2.scale(end2Extent);
        BlockPos startPos = pos1.offset(MyMth.myCeil(offset1));
        BlockPos endPos = pos2.offset(MyMth.myCeil(offset2));

        return new ConnectInfo(
                new Vec3(startPos.getX(), startPos.getY(), startPos.getZ()), axis1,
                new Vec3(endPos.getX(), endPos.getY(), endPos.getZ()), axis2,
                end1Extent, end2Extent);
    }

    /**
     * 添加贝塞尔曲线段
     */
    private void addBezierSegment(CurveRoute curve, List<TrackPlacement> placements,
                                   Vec3 start, Vec3 startDir, Vec3 endOffset, Vec3 endDir) {
        if (Math.abs(startDir.dot(endDir)) > 0.9999 && startDir.dot(endOffset.normalize()) > 0.9999) {
            // 近似直线
            Vec3 end = start.add(endOffset);
            curve.addSegment(new CurveRoute.LineSegment(start, end));
            placements.add(new TrackPlacement(
                    new BlockPos((int) start.x, (int) start.y, (int) start.z),
                    startDir, null));
        } else {
            // 贝塞尔曲线
            curve.addSegment(CurveRoute.BezierSegment.getCubicBezier(start, startDir, endOffset, endDir));
            Vec3 cut = new Vec3(MyMth.splitFunc(endDir.x), 0, MyMth.splitFunc(endDir.z));
            placements.add(new TrackPlacement(
                    new BlockPos((int) start.x, (int) start.y, (int) start.z),
                    startDir,
                    new BezierInfo(start, startDir, endOffset.add(cut), endDir)));
        }
    }

    /**
     * 连接信息记录
     */
    private record ConnectInfo(
            Vec3 startPos, Vec3 startAxis,
            Vec3 endPos, Vec3 endAxis,
            int startExtent, int endExtent
    ) {}

    // --- 坐标转换 ---

    private int[] worldToImagePos(int worldX, int worldZ) {
        int centerX = regionPos.x() * CHUNK_GROUP_SIZE * 16;
        int centerZ = regionPos.z() * CHUNK_GROUP_SIZE * 16;
        return new int[]{
                (worldX - centerX) * SAMPLING_NUM / 16 + CHUNK_GROUP_SIZE * SAMPLING_NUM,
                (worldZ - centerZ) * SAMPLING_NUM / 16 + CHUNK_GROUP_SIZE * SAMPLING_NUM
        };
    }

    private Vec3 imageToWorldPos(int imgX, int imgZ, int height) {
        int centerX = regionPos.x() * CHUNK_GROUP_SIZE * 16;
        int centerZ = regionPos.z() * CHUNK_GROUP_SIZE * 16;
        return new Vec3(
                (imgX - CHUNK_GROUP_SIZE * SAMPLING_NUM) * 16.0 / SAMPLING_NUM + centerX,
                height,
                (imgZ - CHUNK_GROUP_SIZE * SAMPLING_NUM) * 16.0 / SAMPLING_NUM + centerZ
        );
    }

    // --- 静态方法 ---

    /**
     * 简化的路线规划（不依赖ServerLevel，可在异步线程调用）
     * 直接生成起点到终点的直线/曲线连接
     */
    public static PlanResult planRouteSimple(BlockPos start, BlockPos end, int seaLevel) {
        CurveRoute curve = new CurveRoute();
        List<TrackPlacement> placements = new ArrayList<>();

        Vec3 startVec = Vec3.atCenterOf(start);
        Vec3 endVec = Vec3.atCenterOf(end);

        // 计算方向
        Vec3 direction = endVec.subtract(startVec).normalize();
        Vec3 horizontalDir = new Vec3(direction.x, 0, direction.z).normalize();

        // 确保高度在合理范围
        int startY = Math.max(start.getY(), seaLevel);
        int endY = Math.max(end.getY(), seaLevel);

        startVec = new Vec3(startVec.x, startY, startVec.z);
        endVec = new Vec3(endVec.x, endY, endVec.z);

        Vec3 offset = endVec.subtract(startVec);

        // 添加曲线段
        curve.addSegment(new CurveRoute.LineSegment(startVec, endVec));

        // 添加轨道放置信息
        placements.add(new TrackPlacement(
                new BlockPos((int) startVec.x, (int) startVec.y, (int) startVec.z),
                horizontalDir,
                new BezierInfo(startVec, horizontalDir, offset, horizontalDir.reverse())
        ));

        return new PlanResult(curve, placements);
    }

    /**
     * 清除缓存
     */
    public static void clearCache() {
        regionHeightMapCache.clear();
        regionStructureMapCache.clear();
    }

    /**
     * 根据世界坐标获取区域位置
     */
    public static RegionPos getRegionPos(BlockPos pos) {
        return new RegionPos(
                Math.floorDiv(pos.getX(), CHUNK_GROUP_SIZE * 16),
                Math.floorDiv(pos.getZ(), CHUNK_GROUP_SIZE * 16)
        );
    }

    // --- 内部类 ---

    /**
     * 区域位置记录
     */
    public record RegionPos(int x, int z) {
        @Override
        public String toString() {
            return String.format("Region[%d, %d]", x, z);
        }
    }

    /**
     * 规划结果
     */
    public record PlanResult(CurveRoute curve, List<TrackPlacement> placements) {
        public boolean isEmpty() {
            return placements.isEmpty();
        }
    }

    /**
     * 轨道放置信息
     */
    public record TrackPlacement(BlockPos pos, Vec3 direction, BezierInfo bezierInfo) {
        public boolean isCurve() {
            return bezierInfo != null;
        }
    }

    /**
     * 贝塞尔曲线信息
     */
    public record BezierInfo(Vec3 start, Vec3 startDir, Vec3 endOffset, Vec3 endDir) {}
}
