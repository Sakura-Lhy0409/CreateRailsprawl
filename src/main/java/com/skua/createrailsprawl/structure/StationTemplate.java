/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
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

/**
 * 车站模板
 * 包含出口搜索、区块边界计算
 */
public class StationTemplate extends ModTemplate {
    public enum StationType {
        NORMAL,
        UNDER_GROUND
    }

    private final List<Exit> exits = new ArrayList<>();
    private final int id;
    private final StationType type;

    public StationTemplate(Path path, int heightOffset, int id, StationType type) {
        super(path, heightOffset);
        this.id = id;
        this.type = type;
        searchExit();
    }

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

        return originalX >= 0 && originalX < getWidth() &&
               originalY >= 0 && originalY < getHeight() &&
               originalZ >= 0 && originalZ < getDepth();
    }

    @Override
    public BlockState getBlockState(double x, double y, double z) {
        int originalX = (int) Math.floor(x + getWidth() / 2.0);
        int originalY = (int) Math.floor(y) + heightOffset;
        int originalZ = (int) Math.floor(z + getDepth() / 2.0);

        var blockState = voxelGrid.getBlockState(originalX, originalY, originalZ);

        // 将Jigsaw方块替换为空气
        if (blockState != null && blockState.is(Blocks.JIGSAW))
            return Blocks.AIR.defaultBlockState();

        return blockState;
    }

    /**
     * 获取车站覆盖的区块
     */
    public Set<ChunkPos> getBoundChunks(Vec3 center) {
        Set<ChunkPos> chunks = new HashSet<>();

        double minX = center.x - getWidth() / 2.0;
        double maxX = center.x + getWidth() / 2.0;
        double minZ = center.z - getDepth() / 2.0;
        double maxZ = center.z + getDepth() / 2.0;

        int minChunkX = (int) Math.floor(minX / 16.0);
        int maxChunkX = (int) Math.floor(maxX / 16.0);
        int minChunkZ = (int) Math.floor(minZ / 16.0);
        int maxChunkZ = (int) Math.floor(maxZ / 16.0);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunks.add(new ChunkPos(chunkX, chunkZ));
            }
        }

        return chunks;
    }

    public List<Exit> getExits() {
        return exits;
    }

    public int getExitCount() {
        return exits.size();
    }

    public StationType getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    /**
     * 搜索出口（Jigsaw方块）
     */
    private void searchExit() {
        if (voxelGrid == null) return;

        var palette = voxelGrid.getPalette();
        BlockPos off = new BlockPos(
                -(int) Math.floor(getWidth() / 2.0) + 1,
                -heightOffset + 1,
                -(int) Math.floor(getDepth() / 2.0) + 1
        );

        for (int i = 0; i < voxelGrid.getWidth(); i++) {
            for (int j = 0; j < voxelGrid.getHeight(); j++) {
                for (int k = 0; k < voxelGrid.getDepth(); k++) {
                    int index = voxelGrid.getVoxel(i, j, k);
                    if (index > 0 && index < palette.size() && palette.get(index).is(Blocks.JIGSAW)) {
                        var dir = JigsawBlock.getFrontFacing(palette.get(index));
                        Vec3 dirVec = switch (dir) {
                            case NORTH -> new Vec3(0, 0, -1);
                            case EAST -> new Vec3(1, 0, 0);
                            case SOUTH -> new Vec3(0, 0, 1);
                            case WEST -> new Vec3(-1, 0, 0);
                            default -> new Vec3(0, 0, 0);
                        };
                        exits.add(new Exit(new BlockPos(i, j, k).offset(off), dirVec));
                    }
                }
            }
        }
    }

    /**
     * 出口记录
     */
    public record Exit(BlockPos exitPos, Vec3 dir) {}
}
