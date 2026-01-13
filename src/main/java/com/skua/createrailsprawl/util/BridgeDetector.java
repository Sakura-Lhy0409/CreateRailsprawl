package com.skua.createrailsprawl.util;

import com.skua.createrailsprawl.RailwayConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * 桥梁检测器 - 检测连续水域段并优化桥梁路径
 */
public class BridgeDetector {

    /**
     * 桥梁段信息
     */
    public record BridgeSegment(int startIndex, int endIndex, List<BlockPos> nodes) {
        public int length() {
            return endIndex - startIndex + 1;
        }
    }

    /**
     * 检测路径中的所有桥梁段
     */
    public static List<BridgeSegment> detectBridgeSegments(List<BlockPos> nodes, ServerLevel level, TerrainSamplingCache cache) {
        List<BridgeSegment> segments = new ArrayList<>();
        if (nodes == null || nodes.size() < 2) return segments;

        int bridgeMinWaterDepth = RailwayConfig.bridgeMinWaterDepth;
        boolean[] mask = detectBridgeMask(nodes, level, cache, bridgeMinWaterDepth);

        int i = 0;
        while (i < nodes.size()) {
            if (mask[i]) {
                int start = i;
                while (i < nodes.size() && mask[i]) {
                    i++;
                }
                int end = i - 1;

                List<BlockPos> segmentNodes = new ArrayList<>();
                for (int j = start; j <= end; j++) {
                    segmentNodes.add(nodes.get(j));
                }
                segments.add(new BridgeSegment(start, end, segmentNodes));
            } else {
                i++;
            }
        }

        return segments;
    }

    /**
     * 检测桥梁掩码
     */
    public static boolean[] detectBridgeMask(List<BlockPos> nodes, ServerLevel level, TerrainSamplingCache cache, int bridgeMinWaterDepth) {
        boolean[] mask = new boolean[nodes.size()];
        int seaLevel = level.getSeaLevel();

        for (int i = 0; i < nodes.size(); i++) {
            BlockPos p = nodes.get(i);
            boolean isWater = cache.isColumnWater(level, p.getX(), p.getZ());
            if (!isWater) continue;

            int oceanFloor = cache.oceanFloor(level, p.getX(), p.getZ());
            int waterDepth = Math.max(0, seaLevel - oceanFloor);
            mask[i] = waterDepth >= bridgeMinWaterDepth;
        }

        return mask;
    }

    /**
     * 拉直桥梁段 - 将连续的桥梁节点拉直为一条直线
     */
    public static List<BlockPos> straightenBridgeSegment(List<BlockPos> nodes, int startIndex, int endIndex) {
        if (endIndex - startIndex < 2) {
            return new ArrayList<>(nodes.subList(startIndex, endIndex + 1));
        }

        List<BlockPos> result = new ArrayList<>();
        BlockPos startPos = nodes.get(startIndex);
        BlockPos endPos = nodes.get(endIndex);

        int segmentCount = endIndex - startIndex;

        for (int i = 0; i <= segmentCount; i++) {
            double t = (double) i / segmentCount;
            int x = (int) (startPos.getX() + t * (endPos.getX() - startPos.getX()));
            int y = (int) (startPos.getY() + t * (endPos.getY() - startPos.getY()));
            int z = (int) (startPos.getZ() + t * (endPos.getZ() - startPos.getZ()));
            result.add(new BlockPos(x, y, z));
        }

        return result;
    }

    /**
     * 处理整条路径，拉直所有桥梁段
     */
    public static List<BlockPos> processBridges(List<BlockPos> nodes, ServerLevel level, TerrainSamplingCache cache) {
        if (nodes == null || nodes.size() < 3) return nodes;

        List<BridgeSegment> segments = detectBridgeSegments(nodes, level, cache);
        if (segments.isEmpty()) return nodes;

        List<BlockPos> result = new ArrayList<>();
        int lastEnd = -1;

        for (BridgeSegment segment : segments) {
            // 添加桥梁段之前的非桥梁节点
            for (int i = lastEnd + 1; i < segment.startIndex; i++) {
                result.add(nodes.get(i));
            }

            // 添加拉直后的桥梁段
            if (segment.length() >= 3) {
                List<BlockPos> straightened = straightenBridgeSegment(nodes, segment.startIndex, segment.endIndex);
                result.addAll(straightened);
            } else {
                // 桥梁段太短，直接添加
                result.addAll(segment.nodes);
            }

            lastEnd = segment.endIndex;
        }

        // 添加最后一个桥梁段之后的节点
        for (int i = lastEnd + 1; i < nodes.size(); i++) {
            result.add(nodes.get(i));
        }

        return result;
    }

    /**
     * 计算桥梁高度 - 确保桥梁高于水面
     */
    public static int calculateBridgeHeight(ServerLevel level, int x, int z, TerrainSamplingCache cache, int minClearance) {
        int seaLevel = level.getSeaLevel();
        int surfaceHeight = cache.height(level, x, z);

        if (cache.isColumnWater(level, x, z)) {
            return seaLevel + minClearance;
        }
        return Math.max(surfaceHeight, seaLevel + minClearance);
    }

    /**
     * 检查是否需要桥梁
     */
    public static boolean needsBridge(ServerLevel level, int x, int z, TerrainSamplingCache cache) {
        int bridgeMinWaterDepth = RailwayConfig.bridgeMinWaterDepth;
        int waterDepth = cache.waterDepth(level, x, z);
        return waterDepth >= bridgeMinWaterDepth;
    }
}
