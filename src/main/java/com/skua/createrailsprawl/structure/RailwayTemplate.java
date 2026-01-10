/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;

/**
 * 铁路模板
 * 用于路基/桥梁/隧道结构
 */
public class RailwayTemplate extends ModTemplate {
    private final int id;
    private final String type;

    public RailwayTemplate(Path path, int heightOffset, int id, String type) {
        super(path, heightOffset);
        this.id = id;
        this.type = type;
    }

    public RailwayTemplate(CompoundTag nbt, int heightOffset, int id, String type) {
        super(nbt, heightOffset);
        this.id = id;
        this.type = type;
    }

    @Override
    public boolean isInVoxel(double x, double y, double z) {
        int originalX = (int) Math.floor(x) % getWidth();
        int originalY = (int) Math.round(y + heightOffset + 1);
        int originalZ = (int) Math.floor(z + getDepth() / 2.0);

        return voxelGrid.isInVoxel(originalX, originalY, originalZ);
    }

    /**
     * 坐标系原点在z轴正方向的方块的方块正中处
     */
    @Override
    public BlockState getBlockState(double x, double y, double z) {
        // 映射到原始坐标系
        // 原始X：沿铁路方向循环
        int originalX = (int) Math.floor(x) % getWidth();
        if (originalX < 0) originalX += getWidth();

        // 原始Y/Z：由局部坐标系决定
        int originalY = (int) Math.round(y + heightOffset + 1);
        int originalZ = (int) Math.floor(z + getDepth() / 2.0);

        // 重复最底层方块
        if (originalY < 0)
            originalY = 0;

        return voxelGrid.getBlockState(originalX, originalY, originalZ);
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }
}
