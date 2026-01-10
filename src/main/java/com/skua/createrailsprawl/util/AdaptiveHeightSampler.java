/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.util;

import java.util.concurrent.*;

/**
 * 自适应高度采样器
 * 使用四叉树结构优化采样，多线程并行处理
 * 自动调整采样密度以适应地形变化
 */
public class AdaptiveHeightSampler {
    private final double threshold;          // 分裂阈值
    private final int maxLevel;              // 最大深度
    private final int samplesPerNode;        // 每个节点采样数 (n x n)
    private QuadTree root;                   // 四叉树根节点
    private final ExecutorService executor;  // 线程池
    private final HeightFunction heightFunction;
    private CountDownLatch latch = null;
    private int count = 0;

    // 四叉树节点类
    private class QuadTree {
        double minX, minZ, maxX, maxZ; // 节点边界
        int level;                     // 节点层级
        double minHeight, maxHeight;   // 高度范围
        QuadTree[] children;           // 四个子节点
        boolean isLeaf;                // 是否为叶节点
        double[][] heightSamples;      // 采样的高度值

        QuadTree(double minX, double minZ, double maxX, double maxZ, int level) {
            this.minX = minX;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxZ = maxZ;
            this.level = level;
            this.children = null;
            this.isLeaf = true;
        }
    }

    /**
     * 构造函数
     * @param threshold 高度差阈值，超过此值则细分
     * @param maxLevel 最大细分深度
     * @param samplesPerNode 每个节点的采样点数 (n x n)
     * @param heightFunction 高度获取函数
     */
    public AdaptiveHeightSampler(double threshold, int maxLevel, int samplesPerNode, HeightFunction heightFunction) {
        this.threshold = threshold;
        this.maxLevel = maxLevel;
        this.samplesPerNode = samplesPerNode;
        this.heightFunction = heightFunction;
        // 创建固定大小的线程池，根据CPU核心数
        int coreCount = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(coreCount);
    }

    /**
     * 构建自适应采样
     * @param regionSize 区域大小 (正方形区域的边长)
     */
    public void buildQuadTree(double regionSize) throws InterruptedException {
        latch = new CountDownLatch(1);
        addCount();
        root = new QuadTree(0, 0, regionSize, regionSize, 0);
        buildNode(root);
        latch.await();
    }

    /**
     * 递归构建四叉树节点
     */
    private void buildNode(QuadTree node) {
        // 采样当前节点
        sampleNode(node);

        // 检查是否需要继续分割
        if (shouldSplit(node)) {
            splitNode(node);
            // 使用多线程并行处理四个子节点
            processChildrenInParallel(node);
        }
        subCount();
    }

    /**
     * 使用多线程并行处理四个子节点
     */
    private void processChildrenInParallel(QuadTree parent) {
        // 为每个子节点提交到线程池
        for (int i = 0; i < 4; i++) {
            addCount();
            final QuadTree child = parent.children[i];
            executor.submit(() -> buildNode(child));
        }
    }

    /**
     * 对节点进行采样
     */
    private void sampleNode(QuadTree node) {
        int n = samplesPerNode;
        node.heightSamples = new double[n][n];
        node.minHeight = Double.MAX_VALUE;
        node.maxHeight = Double.MIN_VALUE;

        double stepX = (node.maxX - node.minX) / (n - 1);
        double stepZ = (node.maxZ - node.minZ) / (n - 1);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double x = node.minX + i * stepX;
                double z = node.minZ + j * stepZ;
                double height = getHeight(x, z);

                node.heightSamples[i][j] = height;
                node.minHeight = Math.min(node.minHeight, height);
                node.maxHeight = Math.max(node.maxHeight, height);
            }
        }
    }

    /**
     * 判断节点是否需要分割
     */
    private boolean shouldSplit(QuadTree node) {
        // 达到最大深度不再分割
        if (node.level >= maxLevel) {
            return false;
        }
        // 高度差超过阈值需要分割
        double range = node.maxHeight - node.minHeight;
        return range > threshold;
    }

    /**
     * 分割节点为四个子节点
     */
    private void splitNode(QuadTree node) {
        node.isLeaf = false;
        node.children = new QuadTree[4];

        double midX = (node.minX + node.maxX) / 2.0;
        double midZ = (node.minZ + node.maxZ) / 2.0;
        int nextLevel = node.level + 1;

        // 创建四个子节点
        node.children[0] = new QuadTree(node.minX, node.minZ, midX, midZ, nextLevel); // 左上
        node.children[1] = new QuadTree(midX, node.minZ, node.maxX, midZ, nextLevel); // 右上
        node.children[2] = new QuadTree(node.minX, midZ, midX, node.maxZ, nextLevel); // 左下
        node.children[3] = new QuadTree(midX, midZ, node.maxX, node.maxZ, nextLevel); // 右下
    }

    /**
     * 获取指定位置的高度值
     */
    public double getHeight(double x, double z) {
        return heightFunction.getHeight(x, z);
    }

    /**
     * 生成指定大小的插值图像
     * @param width 图像宽度
     * @param height 图像高度
     * @return 高度值数组（转换为int）
     */
    public int[][] generateImage(int width, int height) {
        int[][] image = new int[width][height];
        double scaleX = root.maxX / width;
        double scaleZ = root.maxZ / height;

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                double worldX = x * scaleX;
                double worldZ = z * scaleZ;
                image[x][z] = (int) getInterpolatedHeight(worldX, worldZ);
            }
        }

        return image;
    }

    /**
     * 获取插值后的高度值
     */
    private double getInterpolatedHeight(double x, double z) {
        return getHeightFromNode(root, x, z);
    }

    /**
     * 从四叉树节点中获取高度值（递归）
     */
    private double getHeightFromNode(QuadTree node, double x, double z) {
        if (node.isLeaf) {
            // 在叶节点中进行双线性插值
            return bilinearInterpolate(node, x, z);
        }

        // 确定点位于哪个子节点
        double midX = (node.minX + node.maxX) / 2.0;
        double midZ = (node.minZ + node.maxZ) / 2.0;

        int childIndex;
        if (x < midX) {
            if (z < midZ) {
                childIndex = 0; // 左上
            } else {
                childIndex = 2; // 左下
            }
        } else {
            if (z < midZ) {
                childIndex = 1; // 右上
            } else {
                childIndex = 3; // 右下
            }
        }

        return getHeightFromNode(node.children[childIndex], x, z);
    }

    /**
     * 双线性插值
     */
    private double bilinearInterpolate(QuadTree node, double x, double z) {
        int n = samplesPerNode;
        double stepX = (node.maxX - node.minX) / (n - 1);
        double stepZ = (node.maxZ - node.minZ) / (n - 1);

        // 计算在采样网格中的位置
        double gridX = (x - node.minX) / stepX;
        double gridZ = (z - node.minZ) / stepZ;

        int x1 = (int) Math.floor(gridX);
        int z1 = (int) Math.floor(gridZ);
        int x2 = Math.min(x1 + 1, n - 1);
        int z2 = Math.min(z1 + 1, n - 1);

        // 边界处理
        x1 = Math.max(0, x1);
        z1 = Math.max(0, z1);

        double dx = gridX - x1;
        double dz = gridZ - z1;

        // 双线性插值
        double h1 = node.heightSamples[x1][z1];
        double h2 = node.heightSamples[x2][z1];
        double h3 = node.heightSamples[x1][z2];
        double h4 = node.heightSamples[x2][z2];

        return h1 * (1 - dx) * (1 - dz) +
                h2 * dx * (1 - dz) +
                h3 * (1 - dx) * dz +
                h4 * dx * dz;
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 获取四叉树统计信息
     */
    public Stats getStatistics() {
        Stats stats = new Stats();
        collectStats(root, stats);
        return stats;
    }

    private void collectStats(QuadTree node, Stats stats) {
        stats.totalNodes++;
        stats.maxDepth = Math.max(stats.maxDepth, node.level);
        stats.totalSamples += samplesPerNode * samplesPerNode;

        if (node.isLeaf) {
            stats.leafNodes++;
        } else {
            for (QuadTree child : node.children) {
                collectStats(child, stats);
            }
        }
    }

    private synchronized void addCount() {
        count++;
    }

    private synchronized void subCount() {
        count--;
        if (count == 0)
            latch.countDown();
    }

    public static class Stats {
        public int totalNodes = 0;
        public int leafNodes = 0;
        public int maxDepth = 0;
        public int totalSamples = 0;

        @Override
        public String toString() {
            return String.format("节点总数: %d, 叶节点: %d, 最大深度: %d, 采样点: %d",
                    totalNodes, leafNodes, maxDepth, totalSamples);
        }
    }

    @FunctionalInterface
    public interface HeightFunction {
        double getHeight(double x, double z);
    }
}
