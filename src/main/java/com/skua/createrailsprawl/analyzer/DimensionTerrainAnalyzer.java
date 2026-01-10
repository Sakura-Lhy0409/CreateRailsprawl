/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.analyzer;

import com.skua.createrailsprawl.config.RailwayConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * 跨维度地形分析器
 * 支持主世界、下界、末地的不同生成规则
 */
public class DimensionTerrainAnalyzer {

    /**
     * 维度类型枚举
     */
    public enum DimensionCategory {
        OVERWORLD,
        NETHER,
        END,
        OTHER
    }

    /**
     * 获取维度类别
     */
    public static DimensionCategory getDimensionCategory(ServerLevel level) {
        if (level.dimension() == Level.OVERWORLD) {
            return DimensionCategory.OVERWORLD;
        } else if (level.dimension() == Level.NETHER) {
            return DimensionCategory.NETHER;
        } else if (level.dimension() == Level.END) {
            return DimensionCategory.END;
        }
        return DimensionCategory.OTHER;
    }

    /**
     * 检查维度是否启用生成
     */
    public static boolean isDimensionEnabled(ServerLevel level) {
        DimensionCategory dim = getDimensionCategory(level);
        return switch (dim) {
            case OVERWORLD -> RailwayConfig.COMMON.enableOverworld.get();
            case NETHER -> RailwayConfig.COMMON.enableNether.get();
            case END -> RailwayConfig.COMMON.enableEnd.get();
            case OTHER -> false;
        };
    }

    /**
     * 检查维度是否启用被动生成
     */
    public static boolean isPassiveGenerationEnabled(ServerLevel level) {
        DimensionCategory dim = getDimensionCategory(level);
        return switch (dim) {
            case OVERWORLD -> RailwayConfig.COMMON.enableOverworld.get();
            case NETHER -> RailwayConfig.COMMON.netherPassiveGeneration.get();
            case END -> RailwayConfig.COMMON.endPassiveGeneration.get();
            case OTHER -> false;
        };
    }

    /**
     * 获取维度特定的桥墩材质
     */
    public static BlockState getPillarMaterial(ServerLevel level, BlockPos pos) {
        DimensionCategory dim = getDimensionCategory(level);
        return switch (dim) {
            case NETHER -> Blocks.BASALT.defaultBlockState();  // 下界用玄武岩
            case END -> Blocks.OBSIDIAN.defaultBlockState();   // 末地用黑曜石
            default -> TerrainStructureBiomeAnalyzer.analyze(level, pos).surfaceMaterial;
        };
    }

    /**
     * 获取维度特定的路灯材质
     */
    public static BlockState getLampMaterial(ServerLevel level) {
        DimensionCategory dim = getDimensionCategory(level);
        return switch (dim) {
            case NETHER -> Blocks.GLOWSTONE.defaultBlockState();  // 下界用荧石
            case END -> Blocks.END_ROD.defaultBlockState();       // 末地用末地烛
            default -> Blocks.JACK_O_LANTERN.defaultBlockState();
        };
    }

    /**
     * 获取维度特定的栅栏材质
     */
    public static BlockState getFenceMaterial(ServerLevel level) {
        DimensionCategory dim = getDimensionCategory(level);
        return switch (dim) {
            case NETHER -> Blocks.NETHER_BRICK_FENCE.defaultBlockState();
            case END -> Blocks.PURPUR_PILLAR.defaultBlockState();
            default -> Blocks.OAK_FENCE.defaultBlockState();
        };
    }

    /**
     * 检查位置是否安全（避开危险区域）
     */
    public static boolean isSafePosition(ServerLevel level, BlockPos pos) {
        DimensionCategory dim = getDimensionCategory(level);

        switch (dim) {
            case NETHER -> {
                // 避开岩浆湖
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        for (int dy = -3; dy <= 0; dy++) {
                            BlockPos checkPos = pos.offset(dx, dy, dz);
                            if (level.getBlockState(checkPos).is(Blocks.LAVA)) {
                                return false;
                            }
                        }
                    }
                }
            }
            case END -> {
                // 避开末影水晶和黑曜石柱
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        BlockPos checkPos = pos.offset(dx, 0, dz);
                        BlockState state = level.getBlockState(checkPos);
                        if (state.is(Blocks.BEDROCK) || state.is(Blocks.OBSIDIAN)) {
                            // 检查是否是末影龙复活柱
                            if (isEndCrystalPillar(level, checkPos)) {
                                return false;
                            }
                        }
                    }
                }
            }
        }

        return true;
    }

    /**
     * 检查是否是末影水晶柱
     */
    private static boolean isEndCrystalPillar(ServerLevel level, BlockPos pos) {
        // 简化检测：检查是否有连续的黑曜石柱
        int obsidianCount = 0;
        for (int y = 0; y < 10; y++) {
            if (level.getBlockState(pos.above(y)).is(Blocks.OBSIDIAN)) {
                obsidianCount++;
            }
        }
        return obsidianCount > 5;
    }

    /**
     * 查找下界安全的地面高度
     */
    public static int findNetherSurfaceY(ServerLevel level, int x, int z) {
        // 下界从中间向下找安全平台
        for (int y = 100; y > 30; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            BlockState above = level.getBlockState(pos.above());
            BlockState above2 = level.getBlockState(pos.above(2));

            // 需要实心方块，上方两格空气，且不是岩浆
            if (!state.isAir() && !state.is(Blocks.LAVA) &&
                above.isAir() && above2.isAir()) {
                return y + 1;
            }
        }
        return 64;
    }

    /**
     * 查找末地安全的地面高度
     */
    public static int findEndSurfaceY(ServerLevel level, int x, int z) {
        // 末地从上向下找末地石平台
        for (int y = 100; y > 0; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            BlockState above = level.getBlockState(pos.above());

            if (state.is(Blocks.END_STONE) && above.isAir()) {
                return y + 1;
            }
        }
        return 64;
    }

    /**
     * 获取维度适配的地面高度
     */
    public static int findSurfaceY(ServerLevel level, int x, int z) {
        DimensionCategory dim = getDimensionCategory(level);
        return switch (dim) {
            case NETHER -> findNetherSurfaceY(level, x, z);
            case END -> findEndSurfaceY(level, x, z);
            default -> TerrainStructureBiomeAnalyzer.findSurfaceY(level, x, z);
        };
    }

    /**
     * 获取维度群系类别（用于HUD颜色）
     */
    public static String getBiomeCategoryForHud(ServerLevel level, BlockPos pos) {
        DimensionCategory dim = getDimensionCategory(level);
        return switch (dim) {
            case NETHER -> "nether";
            case END -> "end";
            default -> TerrainStructureBiomeAnalyzer.analyze(level, pos).biomeCategory;
        };
    }
}
