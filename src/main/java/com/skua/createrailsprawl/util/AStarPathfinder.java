/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.util;

import java.util.*;

/**
 * A*寻路算法
 * 支持8方向移动，考虑高度差代价
 * 参考: https://www.redblobgames.com/pathfinding/a-star/introduction.html
 */
public class AStarPathfinder {

    // 8方向移动：上、下、左、右、左上、左下、右上、右下
    private static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1},    // 正方向
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}   // 斜向
    };

    // 移动代价：正方向为1，斜向为√2≈1.414
    private static final double[] MOVEMENT_COST = {
            1.0, 1.0, 1.0, 1.0,
            1.414, 1.414, 1.414, 1.414
    };

    /**
     * 寻找从起点到终点的最优路径
     * @param heightMap 高度图（用于计算高度差代价）
     * @param start 起点坐标 [x, y]
     * @param end 终点坐标 [x, y]
     * @param costFunction 额外代价函数（用于避开结构等）
     * @return 路径点列表，如果无路径则返回空列表
     */
    public static List<int[]> findPath(int[][] heightMap, int[] start, int[] end, AdditionalCostFunction costFunction) {
        if (heightMap == null || heightMap.length == 0 || heightMap[0].length == 0) {
            return new ArrayList<>();
        }

        int rows = heightMap.length;
        int cols = heightMap[0].length;

        // 验证起点和终点是否在图范围内
        if (!isValidCoordinate(start[0], start[1], rows, cols) ||
                !isValidCoordinate(end[0], end[1], rows, cols)) {
            return new ArrayList<>();
        }

        // 优先队列，按f值排序
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(node -> node.f));

        // 记录每个节点的g值（起点到该点的实际代价）
        double[][] gScore = new double[rows][cols];
        for (double[] row : gScore) {
            Arrays.fill(row, Double.MAX_VALUE);
        }

        // 记录每个节点的父节点，用于重建路径
        Node[][] cameFrom = new Node[rows][cols];

        // 初始化起点
        Node startNode = new Node(start[0], start[1]);
        startNode.g = 0;
        startNode.h = heuristic(start, end);
        startNode.f = startNode.g + startNode.h;

        gScore[start[0]][start[1]] = 0;
        openSet.offer(startNode);

        // 记录节点是否在开放集中
        boolean[][] inOpenSet = new boolean[rows][cols];
        inOpenSet[start[0]][start[1]] = true;

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            int currentX = current.x;
            int currentY = current.y;

            // 如果到达终点，重建路径
            if (currentX == end[0] && currentY == end[1]) {
                return reconstructPath(cameFrom, current);
            }

            inOpenSet[currentX][currentY] = false;

            // 遍历所有可能的移动方向
            for (int i = 0; i < DIRECTIONS.length; i++) {
                int[] direction = DIRECTIONS[i];
                int newX = currentX + direction[0];
                int newY = currentY + direction[1];

                // 检查新坐标是否有效
                if (!isValidCoordinate(newX, newY, rows, cols)) {
                    continue;
                }

                // 计算移动代价
                double movementCost = MOVEMENT_COST[i];
                double heightCost = Math.abs(heightMap[currentX][currentY] - heightMap[newX][newY]);
                double additionalCost = costFunction != null ? costFunction.cost(newX, newY) : 0;
                double tentativeG = current.g + movementCost + heightCost + additionalCost;

                // 如果找到更好路径
                if (tentativeG < gScore[newX][newY]) {
                    Node neighbor = new Node(newX, newY);
                    neighbor.g = tentativeG;
                    neighbor.h = heuristic(new int[]{newX, newY}, end);
                    neighbor.f = neighbor.g + neighbor.h;

                    cameFrom[newX][newY] = current;
                    gScore[newX][newY] = tentativeG;

                    if (!inOpenSet[newX][newY]) {
                        openSet.offer(neighbor);
                        inOpenSet[newX][newY] = true;
                    }
                }
            }
        }

        // 开放集为空且未到达终点，说明无路径
        return new ArrayList<>();
    }

    /**
     * 简化版寻路（无额外代价函数）
     */
    public static List<int[]> findPath(int[][] heightMap, int[] start, int[] end) {
        return findPath(heightMap, start, end, null);
    }

    // 检查坐标是否有效
    private static boolean isValidCoordinate(int x, int y, int rows, int cols) {
        return x >= 0 && x < rows && y >= 0 && y < cols;
    }

    // 启发式函数：使用欧几里得距离
    private static double heuristic(int[] a, int[] b) {
        int dx = Math.abs(a[0] - b[0]);
        int dy = Math.abs(a[1] - b[1]);
        return Math.sqrt(dx * dx + dy * dy);
    }

    // 重建路径
    private static List<int[]> reconstructPath(Node[][] cameFrom, Node current) {
        List<int[]> path = new ArrayList<>();

        // 从终点反向追踪到起点
        while (current != null) {
            path.add(0, new int[]{current.x, current.y});
            current = cameFrom[current.x][current.y];
        }

        return path;
    }

    // 节点类
    static class Node {
        int x, y;
        double g; // 起点到该节点的实际代价
        double h; // 启发式估计代价
        double f; // 总代价 f = g + h

        Node(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * 额外代价函数接口
     */
    @FunctionalInterface
    public interface AdditionalCostFunction {
        double cost(int x, int y);
    }

    /**
     * 将世界坐标转换为图片坐标
     */
    public static int[] worldToImagePos(int worldX, int worldZ, int centerX, int centerZ, int samplingNum) {
        return new int[]{
                (worldX - centerX) * samplingNum / 16 + samplingNum / 2,
                (worldZ - centerZ) * samplingNum / 16 + samplingNum / 2
        };
    }

    /**
     * 将图片坐标转换为世界坐标
     */
    public static int[] imageToWorldPos(int imgX, int imgZ, int centerX, int centerZ, int samplingNum) {
        return new int[]{
                (imgX - samplingNum / 2) * 16 / samplingNum + centerX,
                (imgZ - samplingNum / 2) * 16 / samplingNum + centerZ
        };
    }
}
