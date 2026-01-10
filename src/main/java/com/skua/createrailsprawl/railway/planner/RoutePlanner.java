package com.skua.createrailsprawl.railway.planner;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.railway.RailwayBuilder;
import com.skua.createrailsprawl.railway.RegionPos;
import com.skua.createrailsprawl.structure.TrackPutInfo;
import com.skua.createrailsprawl.util.*;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.skua.createrailsprawl.CreateRailsprawl.CHUNK_GROUP_SIZE;
import static com.skua.createrailsprawl.CreateRailsprawl.HEIGHT_MAX_INCREMENT;
import static com.skua.createrailsprawl.railway.RailwayMap.samplingNum;

public class RoutePlanner {
    private final RegionPos regionPos;

    public RoutePlanner(RegionPos regionPos) {
        this.regionPos = regionPos;
    }

    public int[][] getCostMap(WorldGenRegion level) {
        int[][] heightMap = new int[CHUNK_GROUP_SIZE*samplingNum*3][CHUNK_GROUP_SIZE*samplingNum*3];
        for (int[] ints : heightMap) Arrays.fill(ints, 50000);
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (Math.abs(i) == 1 && Math.abs(j) == 1) continue;
                RegionPos rPos = new RegionPos(regionPos.x() + i, regionPos.z() + j);
                RailwayBuilder builder = RailwayBuilder.getInstance(level.getSeed());
                int[][] map = builder != null ? builder.regionHeightMap.computeIfAbsent(rPos, k -> getHeightMap(level.getLevel(), rPos)) : getHeightMap(level.getLevel(), rPos);
                for (int x = 0; x < map.length; x++) {
                    for (int z = 0; z < map[0].length; z++) {
                        heightMap[(i+1)*CHUNK_GROUP_SIZE*samplingNum+x][(j+1)*CHUNK_GROUP_SIZE*samplingNum+z] = map[x][z];
                    }
                }
            }
        }
        return heightMap;
    }

    public int[][] getStructureCostMap(WorldGenRegion level) {
        int[][] structureMap = new int[CHUNK_GROUP_SIZE*samplingNum*3][CHUNK_GROUP_SIZE*samplingNum*3];
        for (int[] ints : structureMap) Arrays.fill(ints, 50000);
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (Math.abs(i) == 1 && Math.abs(j) == 1) continue;
                RegionPos rPos = new RegionPos(regionPos.x() + i, regionPos.z() + j);
                RailwayBuilder builder = RailwayBuilder.getInstance(level.getSeed());
                int[][] map = builder != null ? builder.regionStructureMap.computeIfAbsent(rPos, k -> getStructureMap(level, rPos)) : getStructureMap(level, rPos);
                for (int x = 0; x < map.length; x++) {
                    for (int z = 0; z < map[0].length; z++) {
                        structureMap[(i+1)*CHUNK_GROUP_SIZE*samplingNum+x][(j+1)*CHUNK_GROUP_SIZE*samplingNum+z] = map[x][z];
                    }
                }
            }
        }
        return structureMap;
    }

    private int[][] getHeightMap(ServerLevel serverLevel, RegionPos regionPos) {
        ChunkGenerator gen = serverLevel.getChunkSource().getGenerator();
        RandomState cfg = serverLevel.getChunkSource().randomState();
        AdaptiveHeightSampler sampler = new AdaptiveHeightSampler(10, 3, 4, (x, z) -> {
            int wx = (int) (x*(16.0/samplingNum) + regionPos.x()*CHUNK_GROUP_SIZE*16);
            int wz = (int) (z*(16.0/samplingNum) + regionPos.z()*CHUNK_GROUP_SIZE*16);
            return gen.getBaseHeight(wx, wz, Heightmap.Types.WORLD_SURFACE_WG, serverLevel, cfg);
        });
        try {
            sampler.buildQuadTree(CHUNK_GROUP_SIZE*samplingNum);
        } catch (InterruptedException e) {
            CreateRailsprawl.LOGGER.error(e.getMessage());
        } finally {
            sampler.shutdown();
        }
        return sampler.generateImage(CHUNK_GROUP_SIZE*samplingNum, CHUNK_GROUP_SIZE*samplingNum);
    }

    private int[][] getStructureMap(WorldGenRegion level, RegionPos regionPos) {
        var serverLevel = level.getLevel();
        var registryAccess = level.registryAccess();
        var chunkGeneratorStructureState = serverLevel.getChunkSource().getGeneratorState();
        var structureManager = serverLevel.structureManager();
        var structureFeatureManager = serverLevel.getStructureManager();
        var dimensionType = level.dimensionType();
        LevelHeightAccessor levelHeightAccessor = LevelHeightAccessor.create(dimensionType.minY(), dimensionType.height());
        var biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
        List<BlockPos> structurePos = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(16);
        try {
            CountDownLatch latch = new CountDownLatch(CHUNK_GROUP_SIZE * CHUNK_GROUP_SIZE);
            for (int gx = 0; gx < CHUNK_GROUP_SIZE; gx++) {
                for (int gz = 0; gz < CHUNK_GROUP_SIZE; gz++) {
                    int finalGx = gx, finalGz = gz;
                    executor.execute(() -> {
                        try {
                            var protoChunk = new ProtoChunk(new ChunkPos(regionPos.x() * CHUNK_GROUP_SIZE + finalGx, regionPos.z() * CHUNK_GROUP_SIZE + finalGz), UpgradeData.EMPTY, levelHeightAccessor, biomeRegistry, null);
                            serverLevel.getChunkSource().getGenerator().createStructures(registryAccess, chunkGeneratorStructureState, structureManager, protoChunk, structureFeatureManager);
                            var res = protoChunk.getAllStarts();
                            res.forEach((key, value) -> structurePos.add(new BlockPos(protoChunk.getPos().x * 16, 0, protoChunk.getPos().z * 16)));
                        } finally {
                            latch.countDown();
                        }
                    });
                }
            }
            latch.await();
        } catch (InterruptedException e) {
            CreateRailsprawl.LOGGER.error("Search Feature Err: ", e);
        }

        int[][] costMap = new int[CHUNK_GROUP_SIZE*samplingNum][CHUNK_GROUP_SIZE*samplingNum];
        for (BlockPos pos : structurePos) {
            int[] p = new int[] {(pos.getX() - regionPos.x()*CHUNK_GROUP_SIZE*16)*samplingNum/16, (pos.getZ() - regionPos.z()*CHUNK_GROUP_SIZE*16)*samplingNum/16};
            for (int x = -5*samplingNum; x < 5*samplingNum; x++) {
                for (int z = -5*samplingNum; z < 5*samplingNum; z++) {
                    int px = p[0]+x, pz = p[1]+z;
                    if (px > 0 && px < costMap.length && pz > 0 && pz < costMap[0].length) costMap[px][pz] = 500;
                }
            }
        }
        return costMap;
    }

    public ResultWay getWay(List<int[]> way, int[][] costMap, StationPlanner.ConnectionGenInfo connectionGenInfo, ServerLevel level) {
        List<int[]> handledHeightWay = handleHeight(way, level, costMap, connectionGenInfo);
        handledHeightWay = handledHeightWay.stream().map(AStarPathfinder::pic2RegionPos).toList();
        return connectTrackNew2(handledHeightWay, connectionGenInfo);
    }

    public List<int[]> handleHeight(List<int[]> path, ServerLevel level, int[][] heightMap, StationPlanner.ConnectionGenInfo con) {
        List<double[]> adPath = new LinkedList<>();
        int seaLevel = level.getSeaLevel();
        for (int[] p : path) {
            int h = heightMap[p[0]][p[1]];
            h = Math.max(h, seaLevel + 5);
            h = Math.min(h, seaLevel + HEIGHT_MAX_INCREMENT);
            adPath.add(new double[]{p[0], p[1], h});
        }
        adPath.get(0)[2] = con.connectStart()[2];
        adPath.get(adPath.size()-1)[2] = con.connectEnd()[2];
        adPath = adjustmentHeight(adPath);

        int max = adPath.stream().mapToInt(p -> (int) p[2]).max().orElse(0);
        int min = adPath.stream().mapToInt(p -> (int) p[2]).min().orElse(0);
        int framed2 = ((max - min) / 4) + 1;

        if (adPath.size() > framed2*2 && framed2*2 >= 3) {
            double fh = con.connectStart()[2], lh = con.connectEnd()[2];
            if (adPath.size() > framed2*2+20) {
                for (int i = 1; i < framed2+10; i++) {
                    double t = (double) i / (framed2+10);
                    adPath.get(i)[2] = fh * (1 - t) + adPath.get(i)[2] * t;
                    adPath.get(adPath.size() - 1 - i)[2] = lh * (1 - t) + adPath.get(adPath.size() - 1 - i)[2] * t;
                }
            }
            List<double[]> adPath1 = new ArrayList<>();
            adPath1.add(adPath.get(0));
            for (int i = 1; i < adPath.size()-1; i++) {
                double mean = 0; int sum = 0;
                for (int j = i-framed2; j <= i+framed2; j++) {
                    if (j >= 0 && j < adPath.size()) { mean += adPath.get(j)[2]; sum++; }
                    else if (j < 0) { mean += adPath.get(0)[2]; sum++; }
                    else { mean += adPath.get(adPath.size()-1)[2]; sum++; }
                }
                adPath1.add(new double[] {adPath.get(i)[0], adPath.get(i)[1], mean/sum});
            }
            adPath1.add(adPath.get(adPath.size()-1));
            adPath = adPath1;
        }
        return adPath.stream().map(arr -> Arrays.stream(arr).mapToInt(d -> (int) Math.round(d)).toArray()).collect(Collectors.toList());
    }

    private ResultWay connectTrackNew2(List<int[]> path, StationPlanner.ConnectionGenInfo con) {
        List<Vec3> path0 = new ArrayList<>();
        for (int i = 2; i < path.size() - 2; i++) {
            int[] point = path.get(i);
            path0.add(MyMth.inRegionPos2WorldPos(regionPos, new Vec3(point[0], point[2], point[1]).multiply(16.0/samplingNum, 1, 16.0/samplingNum)));
        }
        List<Vec3> path1 = new ArrayList<>();
        for (int i = 0; i < path0.size()-10; i+=6) path1.add(path0.get(i));
        Vec3 a1 = path1.get(path1.size()-1), b1 = path0.get(path0.size()-1);
        Vec3 c1 = a1.add(b1.subtract(a1).scale(0.5));
        path1.add(new Vec3((int) c1.x(), (int) c1.y(), (int) c1.z()));
        Vec3 last = path1.get(path1.size()-1);

        ResultWay result = new ResultWay(new CurveRoute(), new ArrayList<>());
        Vec3 pA = con.start().add(con.startDir().scale(30)).add(con.exitDir().scale(30));
        if (con.startDir().dot(con.exitDir()) > 0.999) result.addLine(con.start(), pA);
        else result.connectWay(con.start(), pA, con.startDir(), con.exitDir().reverse(), true);

        path1.add(0, pA);
        int size = path1.size(), i = 0;
        Vec3 startDir = con.exitDir();
        while (i < size - 1) {
            Vec3 start = path1.get(i), end = path1.get(i + 1);
            Vec3 nextDir = end.subtract(start).multiply(1,0,1).normalize();
            double dot = startDir.dot(nextDir);
            double cross = startDir.x * nextDir.z - startDir.z * nextDir.x;
            boolean maximiseTurn = start.y == end.y;
            Vec3 endDir;
            if (dot > 0.9999) { endDir = startDir.reverse(); result.addBezier(start, startDir, end.subtract(start), endDir); i++; continue; }
            else if (dot > 0.975) endDir = startDir.reverse();
            else if (dot > 0.75) endDir = MyMth.rotateAroundY(startDir, cross, 45).reverse();
            else if (dot > 0.165) endDir = MyMth.rotateAroundY(startDir, cross, 90).reverse();
            else {
                endDir = MyMth.rotateAroundY(startDir, cross, 90).reverse();
                Vec3 d1 = new Vec3(MyMth.splitFunc(startDir.x), 0, MyMth.splitFunc(startDir.z));
                Vec3 d2 = new Vec3(MyMth.splitFunc(endDir.x), 0, MyMth.splitFunc(endDir.z)).reverse();
                Vec3 newPoint = start.add(d1.scale(8)).add(d2.scale(8));
                result.addBezier(start, startDir, newPoint.subtract(start), endDir);
                path1.add(i+1, newPoint);
                startDir = endDir.reverse(); i++; size++; continue;
            }
            result.connectWay(start, end, startDir, endDir, maximiseTurn);
            startDir = endDir.reverse(); i++;
        }

        Vec3 pB = con.end().add(con.endDir().scale(30)).add(con.exitDir().reverse().scale(30));
        result.connectWay(last, pB, startDir, con.exitDir().reverse(), false);
        if (con.endDir().dot(con.exitDir().reverse()) > 0.999) result.addLine(pB, con.end());
        else result.connectWay(pB, con.end(), con.exitDir(), con.endDir(), true);
        return result;
    }

    private static List<double[]> adjustmentHeight(List<double[]> path) {
        List<double[]> adjustedPath = new ArrayList<>();
        if (path.size() < 2) return new LinkedList<>();
        double hStart = path.get(0)[2], hEnd = path.get(path.size()-1)[2];
        double pNum = path.size() - 1;
        List<double[]> heightList0 = new ArrayList<>();
        Map<Integer, List<double[]>> heightGroups = new HashMap<>();
        double distance = 0;
        for (int i = 0; i < path.size(); i++) {
            double[] point = path.get(i);
            double h = point[2] - hStart * ((pNum - i) / pNum) - hEnd * (i / pNum);
            if (i > 0) distance += 1 + Math.abs(point[2] - path.get(i-1)[2]);
            double[] p = {point[0], point[1], h, i, distance};
            heightList0.add(p);
            heightGroups.computeIfAbsent((int) h, k -> new ArrayList<>()).add(p);
        }
        double sec = Math.sqrt(Math.pow(heightList0.size(), 2) + Math.pow(Math.abs(hStart - hEnd), 2)) / heightList0.size();
        for (int j = 0; j < heightList0.size(); j++) {
            double[] thisPoint = heightList0.get(j);
            adjustedPath.add(new double[] {thisPoint[0], thisPoint[1], thisPoint[2]});
            int hd = 0;
            if (j < heightList0.size() - 1) hd = (int)heightList0.get(j+1)[2] - (int)thisPoint[2];
            if (hd == 0) continue;
            double h = thisPoint[2];
            var group = heightGroups.get((int)h);
            int groupIndex = group.indexOf(thisPoint);
            if (groupIndex < group.size() - 1) {
                double[] nextSameHeightPoint = group.get(groupIndex+1);
                int nextPointIndex = heightList0.indexOf(nextSameHeightPoint);
                double dA = thisPoint[4], dB = nextSameHeightPoint[4];
                double iA = thisPoint[3], iB = nextSameHeightPoint[3];
                boolean conditionBridge = hd < 0 && (iB - iA) * 4 * sec < dB - dA;
                boolean conditionTunnel = hd > 0 && (iB - iA) * 3 * sec < dB - dA;
                if (conditionBridge || conditionTunnel) {
                    for (int k = j; k < nextPointIndex; k++) {
                        double[] np1 = heightList0.get(k+1);
                        adjustedPath.add(new double[] {np1[0], np1[1], thisPoint[2]});
                    }
                    j = nextPointIndex;
                }
            }
        }
        for (int i = 0; i < adjustedPath.size(); i++) {
            adjustedPath.get(i)[2] += hStart * ((pNum - i) / pNum) + hEnd * (i / pNum);
        }
        return adjustedPath;
    }

    public record ResultWay(CurveRoute way, List<TrackPutInfo> trackPutInfos) {
        public void connectWay(Vec3 start, Vec3 end, Vec3 startDir, Vec3 endDir, boolean maximiseTurn) {
            int h = (int) ((start.y + end.y) / 2);
            Vec3 s = new Vec3(start.x, h, start.z), e = new Vec3(end.x, h, end.z);
            var connect = getConnect(BlockPos.containing(s), BlockPos.containing(e), startDir, endDir, maximiseTurn);
            if (connect != null) {
                if (connect.startExtent < 8) h = (int) start.y;
                else if (connect.endExtent < 8) h = (int) end.y;
                Vec3 conStart = new Vec3(connect.startPos.x, h, connect.startPos.z);
                Vec3 conEnd = new Vec3(connect.endPos.x, h, connect.endPos.z);
                if (connect.startExtent != 0) addBezier(start, startDir, conStart.subtract(start), startDir.reverse());
                addBezier(conStart, startDir, conEnd.subtract(conStart), endDir);
                if (connect.endExtent != 0) addBezier(conEnd, endDir.reverse(), end.subtract(conEnd), endDir);
            } else {
                addBezier(start, startDir, end.subtract(start), endDir);
                CreateRailsprawl.LOGGER.warn("!!!!! => The road position cannot be determined, and the line has been forced to connect. {} {}", start, end);
            }
        }

        private static ConnectInfo getConnect(BlockPos pos1, BlockPos pos2, Vec3 axis1, Vec3 axis2, boolean maximiseTurn) {
            Vec3 normedAxis1 = axis1.normalize(), normedAxis2 = axis2.normalize();
            Vec3 end1 = MyMth.getCurveStart(pos1, axis1), end2 = MyMth.getCurveStart(pos2, axis2);
            double[] intersect = VecHelper.intersect(end1, end2, normedAxis1, normedAxis2, Direction.Axis.Y);
            boolean parallel = intersect == null, skipCurve = false;
            Vec3 cross2 = normedAxis2.cross(new Vec3(0, 1, 0));
            double a1 = Mth.atan2(normedAxis2.z, normedAxis2.x), a2 = Mth.atan2(normedAxis1.z, normedAxis1.x);
            double angle = a1 - a2, ascend = end2.subtract(end1).y, absAscend = Math.abs(ascend);
            int end1Extent = 0, end2Extent = 0;
            double dist = 0;

            if (parallel) {
                double[] sTest = VecHelper.intersect(end1, end2, normedAxis1, cross2, Direction.Axis.Y);
                if (sTest != null) {
                    double t = Math.abs(sTest[0]), u = Math.abs(sTest[1]);
                    skipCurve = Mth.equal(u, 0);
                    if (!skipCurve && sTest[0] < 0) return new ConnectInfo(new Vec3(pos1.getX(), pos1.getY(), pos1.getZ()), axis1, new Vec3(pos2.getX(), pos2.getY(), pos2.getZ()), axis2, end1Extent, end2Extent);
                    if (skipCurve) {
                        dist = VecHelper.getCenterOf(pos1).distanceTo(VecHelper.getCenterOf(pos2));
                        end1Extent = (int) Math.round((dist + 1) / axis1.length());
                    } else {
                        if (!Mth.equal(ascend, 0) || normedAxis1.y != 0) return null;
                        double targetT = u <= 1 ? 3 : u * 2;
                        if (t < targetT) return null;
                        if (t > targetT) {
                            int correction = (int) ((t - targetT) / axis1.length());
                            end1Extent = maximiseTurn ? 0 : correction / 2 + (correction % 2);
                            end2Extent = maximiseTurn ? 0 : correction / 2;
                        }
                    }
                }
            }

            if (skipCurve && !Mth.equal(ascend, 0)) {
                int hDistance = end1Extent;
                if (axis1.y == 0 || !Mth.equal(absAscend + 1, dist / axis1.length())) {
                    if (axis1.y != 0 && axis1.y == -axis2.y) return null;
                    end1Extent = 0;
                    double minHDistance = Math.max(absAscend < 4 ? absAscend * 4 : absAscend * 3, 6) / axis1.length();
                    if (hDistance < minHDistance) return null;
                    if (hDistance > minHDistance) {
                        int correction = (int) (hDistance - minHDistance);
                        end1Extent = maximiseTurn ? 0 : correction / 2 + (correction % 2);
                        end2Extent = maximiseTurn ? 0 : correction / 2;
                    }
                    skipCurve = false;
                }
            }

            if (!parallel) {
                float absAngle = Math.abs(AngleHelper.deg(angle));
                if (absAngle < 60 || absAngle > 300) return null;
                intersect = VecHelper.intersect(end1, end2, normedAxis1, normedAxis2, Direction.Axis.Y);
                double dist1 = Math.abs(intersect[0]), dist2 = Math.abs(intersect[1]);
                float ex1 = 0, ex2 = 0;
                if (dist1 > dist2) ex1 = (float) ((dist1 - dist2) / axis1.length());
                if (dist2 > dist1) ex2 = (float) ((dist2 - dist1) / axis2.length());
                double turnSize = Math.min(dist1, dist2) - .1d;
                boolean ninety = (absAngle + .25f) % 90 < 1;
                if (intersect[0] < 0 || intersect[1] < 0) return null;
                double minTurnSize = ninety ? 7 : 3.25;
                double turnSizeToFitAscend = minTurnSize + (ninety ? Math.max(0, absAscend - 3) * 2f : Math.max(0, absAscend - 1.5f) * 1.5f);
                if (turnSize < minTurnSize || turnSize < turnSizeToFitAscend) return null;
                if (!maximiseTurn) {
                    ex1 += (float) ((turnSize - turnSizeToFitAscend) / axis1.length());
                    ex2 += (float) ((turnSize - turnSizeToFitAscend) / axis2.length());
                }
                end1Extent = Mth.floor(ex1);
                end2Extent = Mth.floor(ex2);
            }

            Vec3 offset1 = axis1.scale(end1Extent), offset2 = axis2.scale(end2Extent);
            BlockPos startPos = pos1.offset(MyMth.myCeil(offset1)), endPos = pos2.offset(MyMth.myCeil(offset2));
            return new ConnectInfo(new Vec3(startPos.getX(), startPos.getY(), startPos.getZ()), axis1, new Vec3(endPos.getX(), endPos.getY(), endPos.getZ()), axis2, end1Extent, end2Extent);
        }

        public void addLine(Vec3 start, Vec3 end) {
            way.addSegment(new CurveRoute.LineSegment(start, end));
            int n = Math.max((int) Math.abs(start.x - end.x), (int) Math.abs(start.z - end.z));
            for (int k = 0; k <= n; k++) {
                int x = (int) (start.x + MyMth.getSign(end.x - start.x)*k);
                int z = (int) (start.z + MyMth.getSign(end.z - start.z)*k);
                trackPutInfos.add(TrackPutInfo.getByDir(new BlockPos(x, (int) start.y, z), end.subtract(start), null));
            }
        }

        public void addBezier(Vec3 start, Vec3 startDir, Vec3 endOffset, Vec3 endDir) {
            if (Math.abs(startDir.dot(endDir)) > 0.9999 && startDir.dot(endOffset.normalize()) > 0.9999) {
                Vec3 end = start.add(endOffset);
                way.addSegment(new CurveRoute.LineSegment(start, end));
                int n = Math.max((int) Math.abs(start.x - end.x), (int) Math.abs(start.z - end.z));
                for (int k = 0; k <= n; k++) {
                    int x = (int) (start.x + MyMth.getSign(end.x - start.x)*k);
                    int z = (int) (start.z + MyMth.getSign(end.z - start.z)*k);
                    trackPutInfos.add(TrackPutInfo.getByDir(new BlockPos(x, (int) start.y, z), end.subtract(start), null));
                }
            } else {
                way.addSegment(CurveRoute.BezierSegment.getCubicBezier(start, startDir, endOffset, endDir));
                Vec3 cut = new Vec3(MyMth.splitFunc(endDir.x), 0, MyMth.splitFunc(endDir.z));
                trackPutInfos.add(TrackPutInfo.getByDir(new BlockPos((int) start.x, (int) start.y, (int) start.z), startDir, new TrackPutInfo.BezierInfo(start, startDir, endOffset.add(cut), endDir)));
            }
        }
    }

    private record ConnectInfo(Vec3 startPos, Vec3 startAxis, Vec3 endPos, Vec3 endAxis, int startExtent, int endExtent) {}
}
