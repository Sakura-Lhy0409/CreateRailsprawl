package com.skua.createrailsprawl.worldgen;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.RailwayConfig;
import com.skua.createrailsprawl.block.ModBlocks;
import com.skua.createrailsprawl.block.TrackSpawnerBlockEntity;
import com.skua.createrailsprawl.mixin.ITrackPreGenExtension;
import com.skua.createrailsprawl.railway.RailwayBuilder;
import com.skua.createrailsprawl.railway.RailwayMap;
import com.skua.createrailsprawl.railway.RegionPos;
import com.skua.createrailsprawl.railway.planner.StationPlanner;
import com.skua.createrailsprawl.structure.ModStructureManager;
import com.skua.createrailsprawl.structure.RailwayTemplate;
import com.skua.createrailsprawl.structure.StationTemplate;
import com.skua.createrailsprawl.structure.TrackPutInfo;
import com.skua.createrailsprawl.util.CurveRoute;
import com.skua.createrailsprawl.util.MyMth;
import com.mojang.serialization.Codec;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.track.*;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class RailwayFeature extends Feature<RailwayFeatureConfig> {
    public RailwayFeature(Codec<RailwayFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<RailwayFeatureConfig> ctx) {
        ChunkPos cPos = new ChunkPos(ctx.origin());
        RegionPos regionPos = MyMth.regionPosFromChunkPos(cPos);
        WorldGenLevel world = ctx.level();
        ChunkAccess chunk = world.getChunk(cPos.x, cPos.z);

        // Try to get existing instance first
        RailwayBuilder builder = RailwayBuilder.getInstance(ctx.level().getSeed());
        
        // If not initialized and we have a WorldGenRegion, initialize it
        if (builder == null && world instanceof WorldGenRegion worldGenRegion) {
            builder = RailwayBuilder.getInstance(ctx.level().getSeed(), worldGenRegion);
            builder.generateRailway(regionPos, worldGenRegion);
        }
        
        if (builder == null) {
            // Still not initialized, skip for now
            return true;
        }

        RailwayMap railwayMap = builder.regionRailways.get(regionPos);
        if (railwayMap == null && world instanceof WorldGenRegion worldGenRegion) {
            builder.generateRailway(regionPos, worldGenRegion);
            railwayMap = builder.regionRailways.get(regionPos);
        }
        if (railwayMap == null) {
            return true;
        }

        // 根据路线生成路基
        if (railwayMap.routeMap.containsKey(cPos)) {
            placeRoadbed(railwayMap, cPos, chunk, world);
        }

        // 放置车站
        for (StationPlanner.StationGenInfo stationPlace : railwayMap.stations) {
            var station = stationPlace.stationTemplate();
            if (station == null) continue;
            var pos = stationPlace.placePos();
            var center = new Vec3(pos.getX(), pos.getY(), pos.getZ());

            if (station.getBoundChunks(center).contains(cPos)) {
                placeStation(cPos, center, station, chunk);
            }
        }

        // 放置铁轨刷怪笼
        if (RailwayConfig.useTrackSpawnerPlaceTrack && RailwayConfig.generateTrackSpawner) {
            if (railwayMap.trackMap.containsKey(cPos)) {
                var firstInfo = railwayMap.trackMap.get(cPos).get(0);
                BlockPos checkPos = firstInfo.pos().offset(0, -1, 0);
                if (!world.getBlockState(checkPos).is(ModBlocks.TRACK_SPAWNER.get())) {
                    world.setBlock(checkPos, ModBlocks.TRACK_SPAWNER.get().defaultBlockState(), 3);
                }
                if (world.getBlockEntity(checkPos) instanceof TrackSpawnerBlockEntity trackSpawner) {
                    trackSpawner.addTracks(railwayMap.trackMap.get(cPos));
                }
            }
        }

        if (!RailwayConfig.useTrackSpawnerPlaceTrack) {
            // 放置铁轨
            if (railwayMap.trackMap.containsKey(cPos)) {
                List<TrackPutInfo> tracks = railwayMap.trackMap.get(cPos);
                tracks.forEach(track -> {
                    if (track.bezier() != null) {
                        if (Math.abs(track.bezier().endOffset().y) > 15) {
                            CreateRailsprawl.LOGGER.warn("Railway track height offset is too large. Generation Failed at {}", track.pos());
                            return;
                        }
                        placeCurveTrack(world, track);
                    } else {
                        if (!world.getBlockState(track.pos()).is(AllBlocks.TRACK.get())) {
                            world.setBlock(track.pos(), AllBlocks.TRACK.getDefaultState().setValue(TrackBlock.SHAPE, track.shape()), 3);
                        }
                    }
                });
            }
        }

        return true;
    }

    private void placeCurveTrack(WorldGenLevel world, TrackPutInfo track) {
        BlockPos startPos = track.pos();
        world.setBlock(startPos, AllBlocks.TRACK.getDefaultState().setValue(TrackBlock.SHAPE, track.shape()).setValue(TrackBlock.HAS_BE, true), 3);

        Vec3 offset = track.bezier().endOffset();
        BlockPos endPos = startPos.offset((int) offset.x, (int) offset.y, (int) offset.z);
        world.setBlock(endPos, AllBlocks.TRACK.getDefaultState().setValue(TrackBlock.SHAPE, track.endShape()).setValue(TrackBlock.HAS_BE, true), 3);

        Vec3 start1 = track.bezier().start().add(getStartVec(track.bezier().startAxis()));
        Vec3 start2 = track.bezier().start().add(track.bezier().endOffset()).add(getStartVec(track.bezier().endAxis()));

        Vec3 axis1 = track.bezier().startAxis();
        Vec3 axis2 = track.bezier().endAxis();

        Vec3 normal1 = new Vec3(0, 1, 0);
        Vec3 normal2 = new Vec3(0, 1, 0);

        BezierConnection connection = new BezierConnection(
                Couple.create(startPos, endPos),
                Couple.create(start1, start2),
                Couple.create(axis1, axis2),
                Couple.create(normal1, normal2),
                true,
                false,
                TrackMaterial.ANDESITE
        );

        var tbe1 = world.getBlockEntity(startPos);
        var tbe2 = world.getBlockEntity(endPos);

        if (tbe1 != null && tbe2 != null) {
            ((ITrackPreGenExtension) tbe1).addConnectionToPreGen(connection);
            ((ITrackPreGenExtension) tbe2).addConnectionToPreGen(connection.secondary());
        }
    }

    private static void placeStation(ChunkPos cPos, Vec3 center, StationTemplate station, ChunkAccess chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                var test = new Vec3(cPos.x * 16 + x, center.y + 1, cPos.z * 16 + z);
                if (!station.isInVoxel(test.subtract(center)))
                    continue;
                for (int oy = station.getLowerBound(); oy < station.getUpperBound(); oy++) {
                    int y = oy + (int) center.y;
                    var p = new Vec3(cPos.x * 16 + x, y, cPos.z * 16 + z);
                    var blockState = station.getBlockState(p.subtract(center));
                    if (blockState == null) {
                        if (station.isInVoxel(p.subtract(center))) {
                            blockState = Blocks.AIR.defaultBlockState();
                        } else {
                            continue;
                        }
                    }
                    chunk.setBlockState(new BlockPos(x, y, z), blockState, true);
                }
            }
        }
    }

    private static Vec3 getStartVec(Vec3 dir) {
        double offX = Math.abs(dir.x) < 1e-6 ? 0.5 : (dir.x < 0 ? 0 : 1);
        double offZ = Math.abs(dir.z) < 1e-6 ? 0.5 : (dir.z < 0 ? 0 : 1);
        return new Vec3(offX, 0, offZ);
    }

    private static void placeRoadbed(RailwayMap railwayMap, ChunkPos cPos, ChunkAccess chunk, WorldGenLevel world) {
        var routes = railwayMap.routeMap.get(cPos);
        if (routes == null) return;
        for (CurveRoute route : routes) {
            int seed = route.getSegments().size();
            RailwayTemplate ground = ModStructureManager.getRandomGround(seed);
            RailwayTemplate bridge = ModStructureManager.getRandomBridge(seed);
            RailwayTemplate tunnel = ModStructureManager.getRandomTunnel(seed);

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    var testPoint0 = new Vec3(cPos.x * 16 + x, 80, cPos.z * 16 + z);
                    CurveRoute.Frame frame = route.getFrame(testPoint0);

                    var nearest0 = frame.nearestPoint;
                    double t = frame.globalT;
                    var normal0 = frame.normal0;
                    var binormal0 = frame.binormal0;

                    Vec3 vec0 = testPoint0.subtract(nearest0);

                    BlockPos nearestPos = new BlockPos((int) nearest0.x, (int) nearest0.y, (int) nearest0.z);
                    int h = world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, nearestPos.getX(), nearestPos.getZ());

                    boolean conditionBridge = nearest0.y > h + 10;
                    boolean conditionTunnel = nearest0.y < h - 9;

                    RailwayTemplate structureTemplate;
                    if (conditionBridge) {
                        structureTemplate = bridge;
                    } else if (conditionTunnel) {
                        structureTemplate = tunnel;
                    } else {
                        structureTemplate = ground;
                    }

                    double z0 = vec0.dot(binormal0);

                    if (structureTemplate == null || !structureTemplate.isInVoxel(1, 1, z0))
                        continue;

                    for (int oy = structureTemplate.getLowerBound(); oy <= structureTemplate.getUpperBound(); oy++) {
                        int y = oy + (int) nearest0.y;
                        var testPoint = new Vec3(cPos.x * 16 + x, y, cPos.z * 16 + z);
                        var vec = testPoint.subtract(nearest0);

                        double localX = t * route.getTotalLength();
                        double localY = vec.dot(normal0);
                        double localZ = vec.dot(binormal0);

                        BlockState blockState = structureTemplate.getBlockState(localX, localY, localZ);
                        if (blockState != null) {
                            BlockPos blockPos = new BlockPos(x, y, z);
                            chunk.setBlockState(blockPos, blockState, true);
                        }
                    }

                    if (conditionTunnel)
                        continue;

                    for (int oy = structureTemplate.getLowerBound() - 1; oy > structureTemplate.getLowerBound() - 100; oy--) {
                        int y = oy + (int) nearest0.y;
                        BlockPos blockPos = new BlockPos(x, y, z);

                        if (chunk.getBlockState(blockPos).isFaceSturdy(world, blockPos, Direction.UP)) {
                            break;
                        }

                        var testPoint = new Vec3(cPos.x * 16 + x, y, cPos.z * 16 + z);
                        var vec = testPoint.subtract(nearest0);

                        double localX = t * route.getTotalLength();
                        double localY = vec.dot(normal0);
                        double localZ = vec.dot(binormal0);

                        BlockState blockState = structureTemplate.getBlockState(localX, localY, localZ);
                        if (blockState != null) {
                            chunk.setBlockState(blockPos, blockState, true);
                        }
                    }
                }
            }
        }
    }
}
