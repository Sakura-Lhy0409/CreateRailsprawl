package com.skua.createrailsprawl.util;

import com.skua.createrailsprawl.railway.RegionPos;

import java.util.*;

import static com.skua.createrailsprawl.CreateRailsprawl.CHUNK_GROUP_SIZE;
import static com.skua.createrailsprawl.railway.RailwayMap.samplingNum;

public class AStarPathfinder {

    private static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1},
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    private static final double[] MOVEMENT_COST = {
            1.0, 1.0, 1.0, 1.0,
            1.414, 1.414, 1.414, 1.414
    };

    public static List<int[]> findPath(int[][] image, int[] start, int[] end, AdditionalCostFunction additionalCostFunction) {
        if (image == null || image.length == 0 || image[0].length == 0) {
            return new ArrayList<>();
        }

        int rows = image.length;
        int cols = image[0].length;

        if (!isValidCoordinate(start[0], start[1], rows, cols) ||
                !isValidCoordinate(end[0], end[1], rows, cols)) {
            return new ArrayList<>();
        }

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(node -> node.f));
        double[][] gScore = new double[rows][cols];
        for (double[] row : gScore) {
            Arrays.fill(row, Double.MAX_VALUE);
        }
        Node[][] cameFrom = new Node[rows][cols];

        Node startNode = new Node(start[0], start[1]);
        startNode.g = 0;
        startNode.h = heuristic(start, end);
        startNode.f = startNode.g + startNode.h;

        gScore[start[0]][start[1]] = 0;
        openSet.offer(startNode);

        boolean[][] inOpenSet = new boolean[rows][cols];
        inOpenSet[start[0]][start[1]] = true;

        while (!openSet.isEmpty()) {
            Node current = openSet.poll();
            int currentX = current.x;
            int currentY = current.y;

            if (currentX == end[0] && currentY == end[1]) {
                return reconstructPath(cameFrom, current);
            }

            inOpenSet[currentX][currentY] = false;

            for (int i = 0; i < DIRECTIONS.length; i++) {
                int[] direction = DIRECTIONS[i];
                int newX = currentX + direction[0];
                int newY = currentY + direction[1];

                if (!isValidCoordinate(newX, newY, rows, cols)) {
                    continue;
                }

                double movementCost = MOVEMENT_COST[i];
                double pixelCost = Math.abs(image[currentX][currentY] - image[newX][newY]);
                double tentativeG = current.g + movementCost + pixelCost + additionalCostFunction.cost(currentX, currentY);

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

        return new ArrayList<>();
    }

    private static boolean isValidCoordinate(int x, int y, int rows, int cols) {
        return x >= 0 && x < rows && y >= 0 && y < cols;
    }

    private static double heuristic(int[] a, int[] b) {
        int dx = Math.abs(a[0] - b[0]);
        int dy = Math.abs(a[1] - b[1]);
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static List<int[]> reconstructPath(Node[][] cameFrom, Node current) {
        List<int[]> path = new ArrayList<>();
        while (current != null) {
            path.add(0, new int[]{current.x, current.y});
            current = cameFrom[current.x][current.y];
        }
        return path;
    }

    static class Node {
        int x, y;
        double g, h, f;
        Node(int x, int y) { this.x = x; this.y = y; }
    }

    @FunctionalInterface
    public interface AdditionalCostFunction {
        double cost(int x, int y);
    }

    public static int[] world2PicPos(int[] worldPos, RegionPos centerRegionPos) {
        int wx = worldPos[0];
        int wz = worldPos[1];
        return new int[] {
                (wx - (centerRegionPos.x()-1)*CHUNK_GROUP_SIZE*16)*samplingNum/16,
                (wz - (centerRegionPos.z()-1)*CHUNK_GROUP_SIZE*16)*samplingNum/16
        };
    }

    public static int[] pic2RegionPos(int[] picPos) {
        int px = picPos[0];
        int pz = picPos[1];
        return new int[] {
                px - CHUNK_GROUP_SIZE*samplingNum,
                pz - CHUNK_GROUP_SIZE*samplingNum,
                picPos[2]
        };
    }
}
