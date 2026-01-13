package com.skua.createrailsprawl.block;

import com.skua.createrailsprawl.RailwayConfig;
import com.skua.createrailsprawl.structure.TrackPutInfo;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.redstone.RoseQuartzLampBlock;
import com.simibubi.create.AllBlocks;
import net.createmod.catnip.data.Couple;
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
import java.util.Objects;

public class TrackSpawnerBlockEntity extends BlockEntity {
    private static final int SPAWN_RANGE = 100;
    protected boolean spawnedTrack = false;
    private List<TrackPutInfo> tracksToBuild = new ArrayList<>();

    public TrackSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRACK_SPAWNER.get(), pos, state);
    }

    public void addTrack(TrackPutInfo track) {
        tracksToBuild.add(track);
        setChanged();
    }

    public void addTracks(List<TrackPutInfo> tracks) {
        tracksToBuild.addAll(tracks);
        setChanged();
    }

    public boolean anyPlayerInRange(Level level) {
        return level.hasNearbyAlivePlayer(this.getBlockPos().getX() + 0.5D, this.getBlockPos().getY() + 0.5D, this.getBlockPos().getZ() + 0.5D, SPAWN_RANGE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TrackSpawnerBlockEntity entity) {
        if (entity.spawnedTrack || !entity.anyPlayerInRange(level)) {
            return;
        }
        if (level.isClientSide() || !RailwayConfig.enableTrackSpawner) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            for (TrackPutInfo track : entity.tracksToBuild) {
                if (track.bezier() != null) {
                    placeCurveTrack(serverLevel, track);
                    Objects.requireNonNull(level.getServer()).execute(() -> {
                        placeCurveTrackEntity(serverLevel, track);
                    });
                } else {
                    if (!serverLevel.getBlockState(track.pos()).is(AllBlocks.TRACK.get())) {
                        serverLevel.setBlock(track.pos(), AllBlocks.TRACK.getDefaultState().setValue(TrackBlock.SHAPE, track.shape()), 3);
                    }
                }
            }
        }

        level.destroyBlock(pos, false);
        level.setBlock(pos, AllBlocks.ROSE_QUARTZ_LAMP.getDefaultState().setValue(RoseQuartzLampBlock.POWERING, true), 3);
        entity.spawnedTrack = true;
    }

    private static void placeCurveTrack(ServerLevel world, TrackPutInfo track) {
        BlockPos startPos = track.pos();
        BlockState trackState = AllBlocks.TRACK.getDefaultState()
                .setValue(TrackBlock.SHAPE, track.shape())
                .setValue(TrackBlock.HAS_BE, true);
        world.setBlock(startPos, trackState, 3);

        Vec3 offset = track.bezier().endOffset();
        BlockPos endPos = startPos.offset((int) offset.x, (int) offset.y, (int) offset.z);
        BlockState trackState2 = AllBlocks.TRACK.getDefaultState()
                .setValue(TrackBlock.SHAPE, track.endShape())
                .setValue(TrackBlock.HAS_BE, true);
        world.setBlock(endPos, trackState2, 3);
    }

    private static void placeCurveTrackEntity(ServerLevel world, TrackPutInfo track) {
        BlockPos startPos = track.pos();
        Vec3 offset = track.bezier().endOffset();
        BlockPos endPos = startPos.offset((int) offset.x, (int) offset.y, (int) offset.z);

        TrackBlockEntity tbe1 = (TrackBlockEntity) world.getBlockEntity(startPos);
        TrackBlockEntity tbe2 = (TrackBlockEntity) world.getBlockEntity(endPos);

        if (tbe1 != null && tbe2 != null) {
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
            tbe1.setLevel(world);
            tbe2.setLevel(world);

            tbe1.addConnection(connection);
            tbe2.addConnection(connection.secondary());
        }
    }

    private static Vec3 getStartVec(Vec3 dir) {
        double offX = Math.abs(dir.x) < 1e-6 ? 0.5 : (dir.x < 0 ? 0 : 1);
        double offZ = Math.abs(dir.z) < 1e-6 ? 0.5 : (dir.z < 0 ? 0 : 1);
        return new Vec3(offX, 0, offZ);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        ListTag trackList = new ListTag();
        for (TrackPutInfo track : tracksToBuild) {
            trackList.add(track.toNBT());
        }
        nbt.put("tracks", trackList);
    }

    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        tracksToBuild.clear();
        ListTag trackList = nbt.getList("tracks", Tag.TAG_COMPOUND);
        for (int i = 0; i < trackList.size(); i++) {
            tracksToBuild.add(TrackPutInfo.fromNBT(trackList.getCompound(i)));
        }
    }
}
