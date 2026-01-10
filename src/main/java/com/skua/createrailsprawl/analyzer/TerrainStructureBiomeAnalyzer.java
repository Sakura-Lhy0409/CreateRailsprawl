/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.analyzer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

import java.util.Optional;

/**
 * 地形、建筑、群系分析器
 * 整合地形、建筑、群系检测逻辑，提供统一分析接口供双模式调用
 */
public class TerrainStructureBiomeAnalyzer {

    /**
     * 地形类型枚举
     */
    public enum TerrainType {
        FLAT,       // 平地 - 直轨
        MOUNTAIN,   // 山地 - 隧道
        WATER,      // 水域 - 桥梁
        CANYON      // 峡谷 - 高架桥
    }

    /**
     * 分析结果类
     */
    public static class AnalysisResult {
        public final TerrainType terrainType;
        public final BlockState surfaceMaterial;
        public final boolean nearStructure;
        public final BlockPos structureEntrance;
        public final String biomeCategory;

        public AnalysisResult(TerrainType terrainType, BlockState surfaceMaterial,
                              boolean nearStructure, BlockPos structureEntrance, String biomeCategory) {
            this.terrainType = terrainType;
            this.surfaceMaterial = surfaceMaterial;
            this.nearStructure = nearStructure;
            this.structureEntrance = structureEntrance;
            this.biomeCategory = biomeCategory;
        }
    }

    /**
     * 分析指定位置的地形、建筑和群系
     * @param level 服务端世界
     * @param pos 分析位置
     * @return 分析结果
     */
    public static AnalysisResult analyze(ServerLevel level, BlockPos pos) {
        TerrainType terrainType = analyzeTerrainType(level, pos);
        BlockState surfaceMaterial = getBiomeMaterial(level, pos);
        boolean nearStructure = isNearStructure(level, pos);
        BlockPos structureEntrance = nearStructure ? findStructureEntrance(level, pos) : null;
        String biomeCategory = getBiomeCategory(level, pos);

        return new AnalysisResult(terrainType, surfaceMaterial, nearStructure, structureEntrance, biomeCategory);
    }

    /**
     * 分析地形类型
     */
    private static TerrainType analyzeTerrainType(ServerLevel level, BlockPos pos) {
        int waterCount = 0;
        int airBelowCount = 0;
        int solidAboveCount = 0;

        // 扫描周围区域判断地形
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos checkPos = pos.offset(dx, 0, dz);
                BlockPos below = checkPos.below();
                BlockPos above = checkPos.above(3);

                if (level.getBlockState(checkPos).is(Blocks.WATER) ||
                    level.getBlockState(below).is(Blocks.WATER)) {
                    waterCount++;
                }
                if (level.getBlockState(below).isAir()) {
                    airBelowCount++;
                }
                if (!level.getBlockState(above).isAir()) {
                    solidAboveCount++;
                }
            }
        }

        if (waterCount > 20) return TerrainType.WATER;
        if (airBelowCount > 30) return TerrainType.CANYON;
        if (solidAboveCount > 20) return TerrainType.MOUNTAIN;
        return TerrainType.FLAT;
    }

    /**
     * 根据群系获取匹配材质
     */
    private static BlockState getBiomeMaterial(ServerLevel level, BlockPos pos) {
        Holder<Biome> biomeHolder = level.getBiome(pos);

        if (biomeHolder.is(BiomeTags.IS_BADLANDS)) {
            return Blocks.RED_SANDSTONE.defaultBlockState();
        }
        if (biomeHolder.is(BiomeTags.HAS_DESERT_PYRAMID)) {
            return Blocks.SANDSTONE.defaultBlockState();
        }
        if (biomeHolder.is(BiomeTags.IS_TAIGA)) {
            return Blocks.SPRUCE_LOG.defaultBlockState();
        }
        if (biomeHolder.is(BiomeTags.IS_JUNGLE)) {
            return Blocks.JUNGLE_LOG.defaultBlockState();
        }
        if (biomeHolder.is(BiomeTags.HAS_IGLOO)) {
            return Blocks.SNOW_BLOCK.defaultBlockState();
        }

        return Blocks.OAK_LOG.defaultBlockState();
    }

    /**
     * 获取群系类别名称
     */
    private static String getBiomeCategory(ServerLevel level, BlockPos pos) {
        Holder<Biome> biomeHolder = level.getBiome(pos);

        if (biomeHolder.is(BiomeTags.HAS_DESERT_PYRAMID)) return "desert";
        if (biomeHolder.is(BiomeTags.IS_TAIGA)) return "taiga";
        if (biomeHolder.is(BiomeTags.IS_JUNGLE)) return "jungle";
        if (biomeHolder.is(BiomeTags.HAS_IGLOO)) return "snowy";
        if (biomeHolder.is(BiomeTags.IS_SAVANNA)) return "savanna";
        if (biomeHolder.is(BiomeTags.IS_BADLANDS)) return "badlands";

        return "plains";
    }

    /**
     * 检查是否靠近建筑结构
     */
    private static boolean isNearStructure(ServerLevel level, BlockPos pos) {
        return level.structureManager().hasAnyStructureAt(pos);
    }

    /**
     * 查找建筑入口位置（向建筑偏移3-5格）
     */
    private static BlockPos findStructureEntrance(ServerLevel level, BlockPos pos) {
        if (!level.structureManager().hasAnyStructureAt(pos)) {
            return pos;
        }
        // 简化处理：向随机方向偏移4格作为入口
        int offsetX = level.random.nextBoolean() ? 4 : -4;
        int offsetZ = level.random.nextBoolean() ? 4 : -4;
        return pos.offset(offsetX, 0, offsetZ);
    }

    /**
     * 查找合适的地面高度
     */
    public static int findSurfaceY(ServerLevel level, int x, int z) {
        for (int y = level.getMaxBuildHeight(); y > level.getMinBuildHeight(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            BlockState above = level.getBlockState(pos.above());

            if (!state.isAir() && !state.is(Blocks.WATER) && above.isAir()) {
                return y + 1;
            }
        }
        return 64;
    }
}
