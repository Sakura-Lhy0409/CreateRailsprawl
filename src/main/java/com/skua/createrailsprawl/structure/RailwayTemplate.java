package com.skua.createrailsprawl.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;

public class RailwayTemplate extends ModTemplate {
    public RailwayTemplate(Path path, int heightOffset) {
        super(path, heightOffset);
    }

    public RailwayTemplate(CompoundTag nbt, int heightOffset) {
        super(nbt, heightOffset);
    }

    @Override
    public boolean isInVoxel(double x, double y, double z) {
        int originalX = (int) Math.floor(x) % getWidth();
        int originalY = (int) Math.round(y + heightOffset + 1);
        int originalZ = (int) Math.floor(z + getDepth() / 2.0);
        return voxelGrid.isInVoxel(originalX, originalY, originalZ);
    }

    @Override
    public BlockState getBlockState(double x, double y, double z) {
        int originalX = (int) Math.floor(x) % getWidth();
        int originalY = (int) Math.round(y + heightOffset + 1);
        int originalZ = (int) Math.floor(z + getDepth() / 2.0);
        if (originalY < 0) originalY = 0;
        return voxelGrid.getBlockState(originalX, originalY, originalZ);
    }
}
