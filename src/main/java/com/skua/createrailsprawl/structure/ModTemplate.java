package com.skua.createrailsprawl.structure;

import com.skua.createrailsprawl.CreateRailsprawl;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

public abstract class ModTemplate {
    protected VoxelGrid voxelGrid;
    protected int heightOffset = 0;

    public ModTemplate(Path path, int heightOffset) {
        try (DataInputStream stream = new DataInputStream(new BufferedInputStream(
                new GZIPInputStream(Files.newInputStream(path, StandardOpenOption.READ))))) {
            CompoundTag rootTag = NbtIo.read(stream, NbtAccounter.UNLIMITED);
            voxelGrid = parseStructureNBT(rootTag);
        } catch (Exception e) {
            CreateRailsprawl.LOGGER.error(e.getMessage());
        }
        this.heightOffset = heightOffset;
    }

    public ModTemplate(CompoundTag nbt, int heightOffset) {
        voxelGrid = parseStructureNBT(nbt);
        this.heightOffset = heightOffset;
    }

    public int getWidth() { return voxelGrid.getWidth(); }
    public int getHeight() { return voxelGrid.getHeight(); }
    public int getDepth() { return voxelGrid.getDepth(); }
    public int getUpperBound() { return voxelGrid.getHeight() - heightOffset + 1; }
    public int getLowerBound() { return -(heightOffset + 1); }

    public abstract boolean isInVoxel(double x, double y, double z);
    public boolean isInVoxel(Vec3 pos) { return isInVoxel(pos.x(), pos.y(), pos.z()); }
    public abstract BlockState getBlockState(double x, double y, double z);
    public BlockState getBlockState(Vec3 pos) { return getBlockState(pos.x(), pos.y(), pos.z()); }

    private VoxelGrid parseStructureNBT(CompoundTag rootTag) {
        if (rootTag == null) return null;
        ListTag sizeTag = rootTag.getList("size", Tag.TAG_INT);
        int x = sizeTag.getInt(0), y = sizeTag.getInt(1), z = sizeTag.getInt(2);
        BlockPos size = new BlockPos(x, y, z);
        List<BlockState> palette = parsePalette(rootTag.getList("palette", Tag.TAG_COMPOUND));
        int[][][] voxelGrid = parseBlocks(rootTag.getList("blocks", Tag.TAG_COMPOUND), size);
        return new VoxelGrid(palette, voxelGrid, size);
    }

    private List<BlockState> parsePalette(ListTag paletteTag) {
        List<BlockState> palette = new ArrayList<>();
        palette.add(Blocks.STRUCTURE_VOID.defaultBlockState());
        for (int i = 0; i < paletteTag.size(); i++) {
            CompoundTag blockTag = paletteTag.getCompound(i);
            palette.add(parseBlockState(blockTag));
        }
        return palette;
    }

    private BlockState parseBlockState(CompoundTag blockTag) {
        try {
            String blockName = blockTag.getString("Name");
            ResourceLocation blockId = ResourceLocation.tryParse(blockName);
            Block block = BuiltInRegistries.BLOCK.get(blockId);
            BlockState blockState = block.defaultBlockState();
            if (blockTag.contains("Properties", Tag.TAG_COMPOUND)) {
                blockState = applyProperties(blockState, blockTag.getCompound("Properties"));
            }
            return blockState;
        } catch (Exception e) {
            CreateRailsprawl.LOGGER.error(e.getMessage());
            return Blocks.AIR.defaultBlockState();
        }
    }

    private BlockState applyProperties(BlockState blockState, CompoundTag propertiesTag) {
        BlockState resultState = blockState;
        for (String propertyName : propertiesTag.getAllKeys()) {
            String propertyValue = propertiesTag.getString(propertyName);
            Optional<Property<?>> property = findProperty(blockState, propertyName);
            if (property.isPresent()) {
                resultState = setPropertyValue(resultState, property.get(), propertyValue);
            }
        }
        return resultState;
    }

    private Optional<Property<?>> findProperty(BlockState blockState, String propertyName) {
        return blockState.getProperties().stream()
                .filter(prop -> prop.getName().equals(propertyName))
                .findFirst();
    }

    private <T extends Comparable<T>> BlockState setPropertyValue(BlockState blockState, Property<T> property, String value) {
        Optional<T> propertyValue = property.getValue(value);
        return propertyValue.map(t -> blockState.setValue(property, t)).orElse(blockState);
    }

    private int[][][] parseBlocks(ListTag blocksTag, BlockPos size) {
        int[][][] voxelGrid = new int[size.getX()][size.getY()][size.getZ()];
        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag blockTag = blocksTag.getCompound(i);
            ListTag posTag = blockTag.getList("pos", Tag.TAG_INT);
            int x = posTag.getInt(0), y = posTag.getInt(1), z = posTag.getInt(2);
            int state = blockTag.getInt("state");
            if (x >= 0 && x < size.getX() && y >= 0 && y < size.getY() && z >= 0 && z < size.getZ()) {
                voxelGrid[x][y][z] = state + 1;
            }
        }
        return voxelGrid;
    }
}
