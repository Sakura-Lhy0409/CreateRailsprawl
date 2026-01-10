/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.block;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.palettes.AllPaletteStoneTypes;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackMaterial;
import net.createmod.catnip.data.Couple;
import com.skua.createrailsprawl.config.RailwayConfig;
import com.skua.createrailsprawl.structure.TrackPutInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 轨道生成器方块实体
 * 玩家靠近时延迟生成轨道，避免世界生成时的性能问题
 */
public class TrackSpawnerBlockEntity extends BlockEntity {
    private static final int SPAWN_RANGE = 100;
    private boolean spawnedTrack = false;
    private final List<TrackPutInfo> trackPutInfos = new ArrayList<>();

    public TrackSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRACK_SPAWNER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TrackSpawnerBlockEntity entity) {
        if (entity.spawnedTrack || !entity.anyPlayerInRange(level)) {
            return;
        }
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            if (!RailwayConfig.COMMON.enableTrackSpawner.get()) {
                return;
            }
            for (TrackPutInfo track : entity.trackPutInfos) {
                if (track.bezier() != null) {
                    placeCurveTrack(serverLevel, track);
                    serverLevel.getServer().execute(() -> placeCurveTrackEntity(serverLevel, track));
                } else {
                    if (!serverLevel.getBlockState(track.pos()).is(AllBlocks.TRACK.get())) {
                        serverLevel.setBlock(track.pos(), AllBlocks.TRACK.getDefaultState()
                                .setValue(TrackBlock.SHAPE, track.shape()), 3);
                    }
                }
            }
            // 替换为石灯方块
            level.destroyBlock(pos, false);
            level.setBlock(pos, AllPaletteStoneTypes.LIMESTONE.getBaseBlock().get().defaultBlockState(), 3);
            entity.spawnedTrack = true;
        }
    }

    public void addTrackPutInfo(List<TrackPutInfo> infos) {
        this.trackPutInfos.addAll(infos);
    }

    public boolean anyPlayerInRange(Level level) {
        BlockPos pos = this.getBlockPos();
        int range = RailwayConfig.COMMON.trackSpawnerRange.get();
        return level.hasNearbyAlivePlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, range);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        ListTag tracksTag = new ListTag();
        for (TrackPutInfo track : trackPutInfos) {
            tracksTag.add(track.toNBT());
        }
        tag.put("tracks", tracksTag);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        trackPutInfos.clear();
        ListTag tracksTag = tag.getList("tracks", Tag.TAG_COMPOUND);
        for (int i = 0; i < tracksTag.size(); i++) {
            trackPutInfos.add(TrackPutInfo.fromNBT(tracksTag.getCompound(i)));
        }
    }

    private static void placeCurveTrack(ServerLevel level, TrackPutInfo track) {
        BlockPos startPos = track.pos();
        BlockState trackState = AllBlocks.TRACK.getDefaultState()
                .setValue(TrackBlock.SHAPE, track.shape())
                .setValue(TrackBlock.HAS_BE, true);
        level.setBlock(startPos, trackState, 3);

        Vec3 offset = track.bezier().endOffset();
        BlockPos endPos = startPos.offset((int) offset.x, (int) offset.y, (int) offset.z);
        BlockState trackState2 = AllBlocks.TRACK.getDefaultState()
                .setValue(TrackBlock.SHAPE, track.endShape())
                .setValue(TrackBlock.HAS_BE, true);
        level.setBlock(endPos, trackState2, 3);
    }

    private static void placeCurveTrackEntity(ServerLevel level, TrackPutInfo track) {
        BlockPos startPos = track.pos();
        Vec3 offset = track.bezier().endOffset();
        BlockPos endPos = startPos.offset((int) offset.x, (int) offset.y, (int) offset.z);

        BlockEntity be1 = level.getBlockEntity(startPos);
        BlockEntity be2 = level.getBlockEntity(endPos);

        if (be1 instanceof TrackBlockEntity tbe1 && be2 instanceof TrackBlockEntity tbe2) {
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
            tbe1.setLevel(level);
            tbe2.setLevel(level);
            tbe1.addConnection(connection);
            tbe2.addConnection(connection.secondary());
        }
    }

    private static Vec3 getStartVec(Vec3 dir) {
        double offX = Math.abs(dir.x) < 1e-6 ? 0.5 : (dir.x < 0 ? 0 : 1);
        double offZ = Math.abs(dir.z) < 1e-6 ? 0.5 : (dir.z < 0 ? 0 : 1);
        return new Vec3(offX, 0, offZ);
    }
}
