/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.worldgen;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import net.createmod.catnip.data.Couple;
import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.block.ModBlocks;
import com.skua.createrailsprawl.block.TrackSpawnerBlockEntity;
import com.skua.createrailsprawl.config.RailwayConfig;
import com.skua.createrailsprawl.mixin.ITrackPreGenExtension;
import com.skua.createrailsprawl.planner.RoutePlanner.RegionPos;
import com.skua.createrailsprawl.planner.StationPlanner;
import com.skua.createrailsprawl.structure.ModStructureManager;
import com.skua.createrailsprawl.structure.RailwayTemplate;
import com.skua.createrailsprawl.structure.StationTemplate;
import com.skua.createrailsprawl.structure.TrackPutInfo;
import com.skua.createrailsprawl.util.CurveRoute;
import com.skua.createrailsprawl.util.MyMth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Set;

/**
 * 铁路世界生成Feature
 * 在区块生成时放置铁轨、路基、桥梁、隧道、车站
 */
public class RailwayFeature extends Feature<NoneFeatureConfiguration> {

    public RailwayFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel world = ctx.level();
        BlockPos origin = ctx.origin();
        ChunkPos chunkPos = new ChunkPos(origin);

        // 获取ServerLevel
        ServerLevel serverLevel = world.getLevel();
        if (serverLevel == null) {
            return false;
        }

        // 获取区域位置
        RegionPos regionPos = MyMth.regionPosFromChunkPos(chunkPos);

        // 获取RailwayBuilder和RailwayMap（只获取已生成的数据，不阻塞等待）
        RailwayBuilder builder = RailwayBuilder.getInstance(world.getSeed());
        RailwayMap railwayMap = builder.regionRailways.get(regionPos);

        // 如果数据还没准备好，跳过（异步生成会在 NoiseBasedChunkGeneratorMixin 中触发）
        if (railwayMap == null || !railwayMap.isPlanned()) {
            return false;
        }

        ChunkAccess chunk = world.getChunk(chunkPos.x, chunkPos.z);

        // 1. 放置路基结构
        if (railwayMap.routeMap.containsKey(chunkPos)) {
            placeRoadbed(railwayMap, chunkPos, chunk, world);
        }

        // 2. 放置车站
        List<StationPlanner.StationGenInfo> stations = StationPlanner.generateStation(regionPos, serverLevel, railwayMap.seed);
        for (StationPlanner.StationGenInfo stationInfo : stations) {
            StationTemplate station = (StationTemplate) stationInfo.stationTemplate();
            if (station == null) continue;

            BlockPos stationPos = stationInfo.placePos();
            Vec3 center = Vec3.atCenterOf(stationPos);

            if (station.getBoundChunks(center).contains(chunkPos)) {
                placeStation(chunkPos, center, station, chunk);
            }
        }

        // 3. 放置轨道
        if (railwayMap.trackMap.containsKey(chunkPos)) {
            List<TrackPutInfo> tracks = railwayMap.trackMap.get(chunkPos);
            if (!tracks.isEmpty()) {
                boolean useTrackSpawner = RailwayConfig.COMMON.enableTrackSpawner.get();

                if (useTrackSpawner) {
                    // 使用 TrackSpawner 延迟放置轨道
                    placeTrackSpawner(world, chunkPos, tracks);
                } else {
                    // 直接放置轨道
                    placeTracksDirectly(world, tracks);
                }
            }
        }

        return true;
    }

    /**
     * 放置车站结构
     */
    private void placeStation(ChunkPos cPos, Vec3 center, StationTemplate station, ChunkAccess chunk) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                Vec3 testPos = new Vec3(cPos.x * 16 + x, center.y + 1, cPos.z * 16 + z);
                if (!station.isInVoxel(testPos.subtract(center))) {
                    continue;
                }

                for (int oy = station.getLowerBound(); oy < station.getUpperBound(); oy++) {
                    int y = oy + (int) center.y;
                    Vec3 p = new Vec3(cPos.x * 16 + x, y, cPos.z * 16 + z);
                    BlockState blockState = station.getBlockState(p.subtract(center));

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

    /**
     * 直接放置轨道（不使用 TrackSpawner）
     */
    private void placeTracksDirectly(WorldGenLevel world, List<TrackPutInfo> tracks) {
        for (TrackPutInfo track : tracks) {
            if (track.bezier() != null) {
                // 检查高度差是否过大
                if (Math.abs(track.bezier().endOffset().y) > 15) {
                    CreateRailsprawl.LOGGER.warn("轨道高度差过大，跳过生成: {}", track.pos());
                    continue;
                }
                placeCurveTrack(world, track);
            } else {
                // 直线轨道
                if (!world.getBlockState(track.pos()).is(AllBlocks.TRACK.get())) {
                    world.setBlock(track.pos(), AllBlocks.TRACK.getDefaultState()
                            .setValue(TrackBlock.SHAPE, track.shape()), 3);
                }
            }
        }
    }

    /**
     * 放置曲线轨道
     */
    private void placeCurveTrack(WorldGenLevel world, TrackPutInfo track) {
        BlockPos startPos = track.pos();
        world.setBlock(startPos, AllBlocks.TRACK.getDefaultState()
                .setValue(TrackBlock.SHAPE, track.shape())
                .setValue(TrackBlock.HAS_BE, true), 3);

        Vec3 offset = track.bezier().endOffset();
        BlockPos endPos = startPos.offset((int) offset.x, (int) offset.y, (int) offset.z);
        world.setBlock(endPos, AllBlocks.TRACK.getDefaultState()
                .setValue(TrackBlock.SHAPE, track.endShape())
                .setValue(TrackBlock.HAS_BE, true), 3);

        // 创建贝塞尔曲线连接
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

        // 添加连接到方块实体
        BlockEntity tbe1 = world.getBlockEntity(startPos);
        BlockEntity tbe2 = world.getBlockEntity(endPos);

        if (tbe1 != null && tbe2 != null) {
            ((ITrackPreGenExtension) tbe1).addConnectionToPreGen(connection);
            ((ITrackPreGenExtension) tbe2).addConnectionToPreGen(connection.secondary());
        }
    }

    private static Vec3 getStartVec(Vec3 dir) {
        double offX = Math.abs(dir.x) < 1e-6 ? 0.5 : (dir.x < 0 ? 0 : 1);
        double offZ = Math.abs(dir.z) < 1e-6 ? 0.5 : (dir.z < 0 ? 0 : 1);
        return new Vec3(offX, 0, offZ);
    }

    /**
     * 放置TrackSpawner方块，延迟生成轨道
     */
    private void placeTrackSpawner(WorldGenLevel world, ChunkPos chunkPos, List<TrackPutInfo> tracks) {
        if (tracks.isEmpty()) return;

        // 在第一个轨道位置下方放置 TrackSpawner
        TrackPutInfo firstTrack = tracks.get(0);
        BlockPos spawnerPos = firstTrack.pos().below();

        if (!world.getBlockState(spawnerPos).is(ModBlocks.TRACK_SPAWNER.get())) {
            world.setBlock(spawnerPos, ModBlocks.TRACK_SPAWNER.get().defaultBlockState(), 3);
        }

        BlockEntity be = world.getBlockEntity(spawnerPos);
        if (be instanceof TrackSpawnerBlockEntity spawner) {
            spawner.addTrackPutInfo(tracks);
        }
    }

    /**
     * 放置路基/桥梁/隧道结构
     */
    private void placeRoadbed(RailwayMap railwayMap, ChunkPos chunkPos, ChunkAccess chunk, WorldGenLevel world) {
        Set<CurveRoute> routes = railwayMap.routeMap.get(chunkPos);
        if (routes == null || routes.isEmpty()) return;

        long seed = railwayMap.seed + chunkPos.hashCode();

        RailwayTemplate groundTemplate = ModStructureManager.getRandomGround(seed);
        RailwayTemplate bridgeTemplate = ModStructureManager.getRandomBridge(seed);
        RailwayTemplate tunnelTemplate = ModStructureManager.getRandomTunnel(seed);

        boolean useDefaultBlocks = groundTemplate == null;

        for (CurveRoute route : routes) {
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    int worldX = chunkPos.x * 16 + localX;
                    int worldZ = chunkPos.z * 16 + localZ;

                    Vec3 testPoint = new Vec3(worldX + 0.5, 0, worldZ + 0.5);
                    CurveRoute.Frame frame = route.getFrame(testPoint);
                    if (frame == null) continue;

                    Vec3 nearest = frame.nearestPoint;
                    double distance = Math.sqrt(Math.pow(worldX - nearest.x, 2) + Math.pow(worldZ - nearest.z, 2));

                    if (distance > 5) continue;

                    int groundHeight = world.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, worldX, worldZ);
                    int trackHeight = (int) nearest.y;

                    boolean isBridge = trackHeight > groundHeight + 10;
                    boolean isTunnel = trackHeight < groundHeight - 9;

                    if (useDefaultBlocks) {
                        placeDefaultRoadbed(chunk, localX, localZ, trackHeight, groundHeight, isBridge, isTunnel, distance);
                    } else {
                        RailwayTemplate template;
                        if (isBridge && bridgeTemplate != null) {
                            template = bridgeTemplate;
                        } else if (isTunnel && tunnelTemplate != null) {
                            template = tunnelTemplate;
                        } else {
                            template = groundTemplate;
                        }

                        if (template != null) {
                            placeTemplateRoadbed(chunk, localX, localZ, trackHeight, template, frame, distance);
                        }
                    }
                }
            }
        }
    }

    private void placeDefaultRoadbed(ChunkAccess chunk, int localX, int localZ, int trackHeight,
                                      int groundHeight, boolean isBridge, boolean isTunnel, double distance) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        if (distance <= 2) {
            pos.set(localX, trackHeight - 1, localZ);
            chunk.setBlockState(pos, Blocks.STONE_BRICKS.defaultBlockState(), false);

            if (isBridge) {
                for (int y = trackHeight - 2; y >= groundHeight; y--) {
                    pos.set(localX, y, localZ);
                    if (chunk.getBlockState(pos).isAir()) {
                        chunk.setBlockState(pos, Blocks.STONE_BRICK_WALL.defaultBlockState(), false);
                    } else {
                        break;
                    }
                }
            } else if (isTunnel) {
                for (int y = trackHeight + 1; y <= trackHeight + 3; y++) {
                    pos.set(localX, y, localZ);
                    chunk.setBlockState(pos, Blocks.AIR.defaultBlockState(), false);
                }
                pos.set(localX, trackHeight + 4, localZ);
                chunk.setBlockState(pos, Blocks.STONE_BRICKS.defaultBlockState(), false);
            }
        } else if (distance <= 4) {
            if (isTunnel) {
                for (int y = trackHeight - 1; y <= trackHeight + 4; y++) {
                    pos.set(localX, y, localZ);
                    chunk.setBlockState(pos, Blocks.STONE_BRICKS.defaultBlockState(), false);
                }
            } else if (isBridge) {
                pos.set(localX, trackHeight, localZ);
                chunk.setBlockState(pos, Blocks.STONE_BRICK_WALL.defaultBlockState(), false);
            }
        }
    }

    private void placeTemplateRoadbed(ChunkAccess chunk, int localX, int localZ, int trackHeight,
                                       RailwayTemplate template, CurveRoute.Frame frame, double distance) {
        double localTrackX = frame.globalT * 100;

        for (int oy = template.getLowerBound(); oy <= template.getUpperBound(); oy++) {
            int worldY = trackHeight + oy;

            BlockState state = template.getBlockState(localTrackX, oy, distance);
            if (state != null && !state.isAir()) {
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(localX, worldY, localZ);
                chunk.setBlockState(pos, state, false);
            }
        }
    }
}
