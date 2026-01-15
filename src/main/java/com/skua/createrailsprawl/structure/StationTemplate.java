package com.skua.createrailsprawl.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.nio.file.Path;
import java.util.*;

public class StationTemplate extends ModTemplate {
    public enum StationType { NORMAL, UNDER_GROUND }

    private final List<Exit> exits = new ArrayList<>();
    private final int id;
    private final StationType type;

    public StationTemplate(CompoundTag rootTag, int heightOffset, int id, StationType type) {
        super(rootTag, heightOffset);
        this.id = id;
        this.type = type;
        searchExit();
    }

    @Override
    public boolean isInVoxel(double x, double y, double z) {
        int originalX = (int) Math.floor(x + getWidth() / 2.0);
        int originalY = (int) Math.floor(y) + heightOffset;
        int originalZ = (int) Math.floor(z + getDepth() / 2.0);
        return originalX >= 0 && originalX < getWidth() && originalY >= 0 && originalY < getHeight() && originalZ >= 0 && originalZ < getDepth();
    }

    @Override
    public BlockState getBlockState(double x, double y, double z) {
        int originalX = (int) Math.floor(x + getWidth() / 2.0);
        int originalY = (int) Math.floor(y) + heightOffset;
        int originalZ = (int) Math.floor(z + getDepth() / 2.0);
        var blockState = voxelGrid.getBlockState(originalX, originalY, originalZ);
        if (blockState != null && blockState.is(Blocks.JIGSAW)) return Blocks.AIR.defaultBlockState();
        return blockState;
    }

    public Set<ChunkPos> getBoundChunks(Vec3 center) {
        Set<ChunkPos> chunks = new HashSet<>();
        double minX = center.x - getWidth() / 2.0, maxX = center.x + getWidth() / 2.0;
        double minZ = center.z - getDepth() / 2.0, maxZ = center.z + getDepth() / 2.0;
        int minChunkX = (int) Math.floor(minX / 16.0), maxChunkX = (int) Math.floor(maxX / 16.0);
        int minChunkZ = (int) Math.floor(minZ / 16.0), maxChunkZ = (int) Math.floor(maxZ / 16.0);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunks.add(new ChunkPos(chunkX, chunkZ));
            }
        }
        return chunks;
    }

    public List<Exit> getExits() { return exits; }
    public int getExitCount() { return exits.size(); }
    public StationType getType() { return type; }
    public int getId() { return id; }

    private void searchExit() {
        var palette = voxelGrid.getPalette();
        BlockPos off = new BlockPos(
                -(int) Math.floor(getWidth() / 2.0),
                -heightOffset,
                -(int) Math.floor(getDepth() / 2.0)
        );
        for (int i = 0; i < voxelGrid.getWidth(); i++) {
            for (int j = 0; j < voxelGrid.getHeight(); j++) {
                for (int k = 0; k < voxelGrid.getDepth(); k++) {
                    int index = voxelGrid.getVoxel(i, j, k);
                    if (index != -1 && palette.get(index).is(Blocks.JIGSAW)) {
                        var dir = JigsawBlock.getFrontFacing(palette.get(index));
                        switch (dir) {
                            case NORTH -> exits.add(new Exit(new BlockPos(i, j, k).offset(off), new Vec3(0, 0, -1)));
                            case EAST -> exits.add(new Exit(new BlockPos(i, j, k).offset(off), new Vec3(1, 0, 0)));
                            case SOUTH -> exits.add(new Exit(new BlockPos(i, j, k).offset(off), new Vec3(0, 0, 1)));
                            case WEST -> exits.add(new Exit(new BlockPos(i, j, k).offset(off), new Vec3(-1, 0, 0)));
                        }
                    }
                }
            }
        }
    }

    public record Exit(BlockPos exitPos, Vec3 dir) {}
}
