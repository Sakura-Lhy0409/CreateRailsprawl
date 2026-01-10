package com.skua.createrailsprawl.block;

import com.skua.createrailsprawl.RailwayConfig;
import com.skua.createrailsprawl.structure.TrackPutInfo;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class TrackSpawnerBlockEntity extends BlockEntity {
    private List<TrackPutInfo> tracksToBuild = new ArrayList<>();
    private int tickCounter = 0;
    private static final int TICKS_PER_TRACK = 2;

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

    public static void tick(Level level, BlockPos pos, BlockState state, TrackSpawnerBlockEntity entity) {
        if (level.isClientSide() || !RailwayConfig.enableTrackSpawner) {
            return;
        }

        if (entity.tracksToBuild.isEmpty()) {
            return;
        }

        entity.tickCounter++;
        if (entity.tickCounter < TICKS_PER_TRACK) {
            return;
        }
        entity.tickCounter = 0;

        TrackPutInfo track = entity.tracksToBuild.remove(0);
        if (track != null && level instanceof ServerLevel serverLevel) {
            if (!serverLevel.getBlockState(track.pos()).is(AllBlocks.TRACK.get())) {
                // TODO: implement track placement
            }
        }

        entity.setChanged();

        if (entity.tracksToBuild.isEmpty()) {
            level.removeBlock(pos, false);
        }
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
