package com.skua.createrailsprawl.util;

import com.skua.createrailsprawl.RailwayConfig;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * 路径后处理器 - 简化、松弛、平滑路径，让铁路更加整齐优雅
 */
public class PathPostProcessor {

    /**
     * 完整的路径后处理流程
     */
    public static List<int[]> process(List<int[]> path, ServerLevel level, TerrainSamplingCache cache) {
        if (path == null || path.size() < 3) return path;

        List<int[]> result = path;
        // 1. 路径简化 - 移除共线点
        result = simplify(result, 0.5);
        // 2. 路径松弛 - 消除尖锐折角
        result = relax(result, 2);
        // 3. 桥梁段拉直
        if (level != null && cache != null) {
            result = straightenBridges(result, level, cache);
        }
        return result;
    }

    /**
     * 路径简化 - 使用叉积判断移除共线点
     * @param path 原始路径
     * @param threshold 共线阈值（越小越严格）
     */
    public static List<int[]> simplify(List<int[]> path, double threshold) {
        if (path == null || path.size() < 3) return path;

        List<int[]> result = new ArrayList<>();
        result.add(path.get(0));

        for (int i = 1; i < path.size() - 1; i++) {
            int[] prev = result.get(result.size() - 1);
            int[] curr = path.get(i);
            int[] next = path.get(i + 1);

            // 使用叉积判断三点是否共线
            long dx1 = curr[0] - prev[0];
            long dz1 = curr[1] - prev[1];
            long dx2 = next[0] - curr[0];
            long dz2 = next[1] - curr[1];

            long crossProduct = dx1 * dz2 - dz1 * dx2;

            // 如果叉积大于阈值，说明不共线，保留该点
            if (Math.abs(crossProduct) > threshold) {
                result.add(curr);
            }
        }

        result.add(path.get(path.size() - 1));
        return result;
    }

    /**
     * 路径松弛 - 加权平均消除尖锐折角
     * @param path 原始路径
     * @param iterations 迭代次数
     */
    public static List<int[]> relax(List<int[]> path, int iterations) {
        if (path == null || path.size() < 3) return path;

        List<int[]> result = new ArrayList<>(path);

        for (int iter = 0; iter < iterations; iter++) {
            List<int[]> newPath = new ArrayList<>();
            newPath.add(result.get(0)); // 保留起点

            for (int i = 1; i < result.size() - 1; i++) {
                int[] prev = result.get(i - 1);
                int[] curr = result.get(i);
                int[] next = result.get(i + 1);

                // 加权平均：(前一点 + 2*当前点 + 后一点) / 4
                int nx = (prev[0] + curr[0] * 2 + next[0]) / 4;
                int nz = (prev[1] + curr[1] * 2 + next[1]) / 4;

                // 保留高度信息（如果有）
                if (curr.length > 2) {
                    int ny = (prev.length > 2 ? prev[2] : curr[2]) + curr[2] * 2 + (next.length > 2 ? next[2] : curr[2]);
                    ny /= 4;
                    newPath.add(new int[]{nx, nz, ny});
                } else {
                    newPath.add(new int[]{nx, nz});
                }
            }

            newPath.add(result.get(result.size() - 1)); // 保留终点
            result = newPath;
        }

        return result;
    }

    /**
     * 桥梁段拉直 - 检测连续水域段并拉直
     */
    public static List<int[]> straightenBridges(List<int[]> path, ServerLevel level, TerrainSamplingCache cache) {
        if (path == null || path.size() < 3) return path;

        int bridgeMinWaterDepth = RailwayConfig.bridgeMinWaterDepth;
        boolean[] bridgeMask = detectBridgeMask(path, level, cache, bridgeMinWaterDepth);

        List<int[]> result = new ArrayList<>();
        int i = 0;

        while (i < path.size()) {
            if (!bridgeMask[i]) {
                result.add(path.get(i));
                i++;
            } else {
                // 找到桥梁段的起点和终点
                int bridgeStart = i;
                while (i < path.size() && bridgeMask[i]) {
                    i++;
                }
                int bridgeEnd = i - 1;

                // 如果桥梁段足够长，进行拉直
                if (bridgeEnd - bridgeStart >= 2) {
                    int[] startPoint = path.get(bridgeStart);
                    int[] endPoint = path.get(bridgeEnd);

                    // 添加起点
                    result.add(startPoint);

                    // 线性插值中间点
                    int segmentCount = bridgeEnd - bridgeStart;
                    for (int j = 1; j < segmentCount; j++) {
                        double t = (double) j / segmentCount;
                        int x = (int) (startPoint[0] + t * (endPoint[0] - startPoint[0]));
                        int z = (int) (startPoint[1] + t * (endPoint[1] - startPoint[1]));

                        if (startPoint.length > 2 && endPoint.length > 2) {
                            int y = (int) (startPoint[2] + t * (endPoint[2] - startPoint[2]));
                            result.add(new int[]{x, z, y});
                        } else {
                            result.add(new int[]{x, z});
                        }
                    }

                    // 添加终点
                    result.add(endPoint);
                } else {
                    // 桥梁段太短，直接添加
                    for (int j = bridgeStart; j <= bridgeEnd; j++) {
                        result.add(path.get(j));
                    }
                }
            }
        }

        return result;
    }

    /**
     * 检测桥梁掩码 - 标记哪些点需要桥梁
     */
    private static boolean[] detectBridgeMask(List<int[]> path, ServerLevel level, TerrainSamplingCache cache, int bridgeMinWaterDepth) {
        boolean[] mask = new boolean[path.size()];

        for (int i = 0; i < path.size(); i++) {
            int[] p = path.get(i);
            int waterDepth = cache.waterDepth(level, p[0], p[1]);
            mask[i] = waterDepth >= bridgeMinWaterDepth;
        }

        return mask;
    }

    /**
     * Catmull-Rom 样条曲线插值（备选平滑方式）
     */
    public static List<int[]> catmullRomSmooth(List<int[]> path, int segmentsPerSpan) {
        if (path == null || path.size() < 4) return path;

        List<int[]> result = new ArrayList<>();
        result.add(path.get(0));

        for (int i = 0; i < path.size() - 3; i++) {
            int[] p0 = path.get(i);
            int[] p1 = path.get(i + 1);
            int[] p2 = path.get(i + 2);
            int[] p3 = path.get(i + 3);

            for (int j = 1; j <= segmentsPerSpan; j++) {
                double t = (double) j / segmentsPerSpan;
                double t2 = t * t;
                double t3 = t2 * t;

                // Catmull-Rom 系数
                double c0 = -0.5 * t3 + t2 - 0.5 * t;
                double c1 = 1.5 * t3 - 2.5 * t2 + 1;
                double c2 = -1.5 * t3 + 2 * t2 + 0.5 * t;
                double c3 = 0.5 * t3 - 0.5 * t2;

                int x = (int) (c0 * p0[0] + c1 * p1[0] + c2 * p2[0] + c3 * p3[0]);
                int z = (int) (c0 * p0[1] + c1 * p1[1] + c2 * p2[1] + c3 * p3[1]);

                if (p0.length > 2) {
                    int y = (int) (c0 * p0[2] + c1 * p1[2] + c2 * p2[2] + c3 * p3[2]);
                    result.add(new int[]{x, z, y});
                } else {
                    result.add(new int[]{x, z});
                }
            }
        }

        // 添加最后两个点
        result.add(path.get(path.size() - 2));
        result.add(path.get(path.size() - 1));

        return result;
    }
}
