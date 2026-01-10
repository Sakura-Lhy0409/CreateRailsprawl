/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.util;

import net.minecraft.core.Direction;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * 曲线路线系统
 * 支持直线段和贝塞尔曲线段，使用KD-Tree加速最近点查询
 */
public class CurveRoute {

    // --- 内部接口和类定义 ---

    public interface CurveSegment {
        double getLength();
        Vec3 getPointAt(double t); // t 范围 [0, 1]
        Vec3 getTangentAt(double t);
        List<Vec3> rasterize(int n); // 在xz平面最小n格间隔栅格化
    }

    /**
     * 采样点信息类，用于KD-Tree存储和插值
     */
    private static class SamplePoint {
        Vec3 position;
        Vec3 tangent;
        double segmentU;      // 该点在片段内的参数u
        double globalDist;    // 该点距离曲线起点的距离
        int segmentIndex;

        SamplePoint(Vec3 pos, Vec3 tan, double u, double dist, int idx) {
            this.position = pos;
            this.tangent = tan;
            this.segmentU = u;
            this.globalDist = dist;
            this.segmentIndex = idx;
        }
    }

    // --- 成员变量 ---

    private final List<CurveSegment> segments = new ArrayList<>();
    private final List<SamplePoint> allSamplePoints = new ArrayList<>();
    private KDNode kdTreeRoot;
    private double totalLength = 0;
    private final int SAMPLES_PER_SEGMENT = 50; // 每一段的采样密度

    // --- 核心方法 ---

    public void addSegment(CurveSegment segment) {
        segments.add(segment);
        buildSamplePoints();
    }

    public double getTotalLength() {
        return totalLength;
    }

    public List<CurveSegment> getSegments() {
        return segments;
    }

    /**
     * 构建所有采样点列表、KD-Tree和计算总长度
     */
    public void buildSamplePoints() {
        allSamplePoints.clear();
        totalLength = 0;

        for (int i = 0; i < segments.size(); i++) {
            CurveSegment seg = segments.get(i);
            double segLen = seg.getLength();

            for (int j = 0; j <= SAMPLES_PER_SEGMENT; j++) {
                double u = (double) j / SAMPLES_PER_SEGMENT;
                Vec3 pos = seg.getPointAt(u);
                Vec3 tan = seg.getTangentAt(u);
                // 全局距离 = 之前段的总长 + 当前段内的距离比例
                double currentGlobalDist = totalLength + (u * segLen);

                allSamplePoints.add(new SamplePoint(pos, tan, u, currentGlobalDist, i));
            }
            totalLength += segLen;
        }

        // 构建用于快速查找的KD-Tree
        if (!allSamplePoints.isEmpty()) {
            kdTreeRoot = buildKDTree(new ArrayList<>(allSamplePoints), 0);
        }
    }

    /**
     * 寻找最近点：通过KD-Tree找到距离查询点最近的采样点，并进行线性插值
     */
    public Frame getFrame(Vec3 point) {
        if (kdTreeRoot == null || totalLength == 0) return null;

        // 1. 查找两个最近的采样点
        PriorityQueue<Neighbor> neighbors = new PriorityQueue<>(Comparator.comparingDouble(n -> -n.distance));
        searchNearest(kdTreeRoot, point, 2, 0, neighbors);

        if (neighbors.size() < 2) return null;

        Neighbor n2 = neighbors.poll();
        Neighbor n1 = neighbors.poll(); // n1 是最近的，n2 是次近的

        SamplePoint p1 = n1.node.point;
        SamplePoint p2 = n2.node.point;

        // 2. 在两点之间投影插值
        Vec3 v12 = p2.position.subtract(p1.position);
        double lineLenSq = v12.lengthSqr();
        double fraction = 0;

        if (lineLenSq > 1e-9) {
            Vec3 v1P = point.subtract(p1.position);
            fraction = Math.max(0, Math.min(1, v1P.dot(v12) / lineLenSq));
        }

        // 3. 计算结果
        Vec3 closestPos = p1.position.add(v12.scale(fraction));
        Vec3 closestTangent = p1.tangent.add(p2.tangent.subtract(p1.tangent).scale(fraction)).normalize();
        double closestDist = p1.globalDist + (p2.globalDist - p1.globalDist) * fraction;
        double tGlobal = closestDist / totalLength;

        return new Frame(closestPos, closestTangent, tGlobal, p1.segmentU + (p2.segmentU - p1.segmentU) * fraction);
    }

    // --- 内部实现类：LineSegment ---

    public static class LineSegment implements CurveSegment {
        private final Vec3 start, end;

        public LineSegment(Vec3 start, Vec3 end) {
            this.start = start;
            this.end = end;
        }

        public Vec3 getStart() { return start; }
        public Vec3 getEnd() { return end; }

        @Override
        public double getLength() {
            return start.distanceTo(end);
        }

        @Override
        public Vec3 getPointAt(double t) {
            return start.add(end.subtract(start).scale(t));
        }

        @Override
        public Vec3 getTangentAt(double t) {
            return end.subtract(start).normalize();
        }

        @Override
        public List<Vec3> rasterize(int n) {
            List<Vec3> points = new ArrayList<>();
            double len = getLength();
            int steps = (int) Math.max(1, len / n);
            for (int i = 0; i <= steps; i++) {
                Vec3 p = getPointAt((double) i / steps);
                points.add(new Vec3(p.x / n, 0, p.z / n));
            }
            return points;
        }
    }

    // --- 内部实现类：BezierSegment (三次贝塞尔曲线) ---

    public static class BezierSegment implements CurveSegment {
        private final Vec3 p0, p1, p2, p3;

        public BezierSegment(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
            this.p0 = p0;
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
        }

        public Vec3 getP0() { return p0; }
        public Vec3 getP1() { return p1; }
        public Vec3 getP2() { return p2; }
        public Vec3 getP3() { return p3; }

        /**
         * 根据起点、方向和终点偏移创建贝塞尔曲线
         */
        public static BezierSegment getCubicBezier(
                Vec3 startPos,           // 起点坐标
                Vec3 startAxis,          // 起点切线方向
                Vec3 endOffset,          // 终点相对起点的偏移
                Vec3 endAxis             // 终点切线方向
        ) {
            // 计算终点的绝对坐标
            Vec3 endPos = startPos.add(endOffset);

            // 归一化方向向量
            Vec3 axis1 = startAxis.normalize();
            Vec3 axis2 = endAxis.normalize();

            // 计算控制点距离
            double handleLength = determineHandleLength(startPos, endPos, axis1, axis2);

            // 计算四个控制点
            Vec3 cp0 = startPos;                                    // 起点
            Vec3 cp1 = startPos.add(axis1.scale(handleLength));    // 第一控制点
            Vec3 cp2 = endPos.add(axis2.scale(handleLength));      // 第二控制点
            Vec3 cp3 = endPos;                                      // 终点

            return new BezierSegment(cp0, cp1, cp2, cp3);
        }

        private static double determineHandleLength(Vec3 end1, Vec3 end2, Vec3 axis1, Vec3 axis2) {
            // 计算两个方向的夹角
            double a1 = Mth.atan2(-axis2.z, -axis2.x);
            double a2 = Mth.atan2(axis1.z, axis1.x);
            double angle = a1 - a2;

            float circle = 2 * Mth.PI;
            angle = (angle + circle) % circle;
            if (Math.abs(circle - angle) < Math.abs(angle))
                angle = circle - angle;

            // 如果两个方向平行
            if (Mth.equal((float) angle, 0)) {
                return end2.distanceTo(end1) / 3;
            }

            // 如果两个方向不平行，使用圆弧公式计算
            double n = circle / angle;
            double factor = 4 / 3d * Math.tan(Math.PI / (2 * n));

            Vec3 cross1 = axis1.cross(new Vec3(0, 1, 0));
            Vec3 cross2 = axis2.cross(new Vec3(0, 1, 0));
            double[] intersect = intersectLines(end1, end2, cross1, cross2);

            if (intersect == null) {
                return end2.distanceTo(end1) / 3;
            }

            double radius = Math.abs(intersect[1]);
            double handleLength = radius * factor;
            if (Mth.equal((float) handleLength, 0))
                handleLength = 1;

            return handleLength;
        }

        private static double[] intersectLines(Vec3 p1, Vec3 p2, Vec3 d1, Vec3 d2) {
            // 简化的线段交点计算
            double denom = d1.x * d2.z - d1.z * d2.x;
            if (Math.abs(denom) < 1e-9) return null;

            Vec3 diff = p2.subtract(p1);
            double t = (diff.x * d2.z - diff.z * d2.x) / denom;
            double u = (diff.x * d1.z - diff.z * d1.x) / denom;

            return new double[]{t, u};
        }

        @Override
        public Vec3 getPointAt(double t) {
            double u = 1 - t;
            return p0.scale(u * u * u)
                    .add(p1.scale(3 * u * u * t))
                    .add(p2.scale(3 * u * t * t))
                    .add(p3.scale(t * t * t));
        }

        @Override
        public Vec3 getTangentAt(double t) {
            double u = 1 - t;
            // 一阶导数公式: 3(1-t)^2(p1-p0) + 6(1-t)t(p2-p1) + 3t^2(p3-p2)
            Vec3 tan = p1.subtract(p0).scale(3 * u * u)
                    .add(p2.subtract(p1).scale(6 * u * t))
                    .add(p3.subtract(p2).scale(3 * t * t));
            return tan.normalize();
        }

        @Override
        public double getLength() {
            // 数值积分近似长度
            double length = 0;
            int steps = 20;
            Vec3 prev = getPointAt(0);
            for (int i = 1; i <= steps; i++) {
                Vec3 curr = getPointAt((double) i / steps);
                length += prev.distanceTo(curr);
                prev = curr;
            }
            return length;
        }

        @Override
        public List<Vec3> rasterize(int n) {
            List<Vec3> points = new ArrayList<>();
            int steps = 20;
            for (int i = 0; i <= steps; i++) {
                Vec3 p = getPointAt((double) i / steps);
                points.add(new Vec3(p.x / n, 0, p.z / n));
            }
            return points;
        }
    }

    // --- KD-Tree 内部结构 ---

    private static class KDNode {
        SamplePoint point;
        KDNode left, right;
        int axis; // 0:x, 1:y, 2:z

        KDNode(SamplePoint p, int axis) {
            this.point = p;
            this.axis = axis;
        }
    }

    private KDNode buildKDTree(List<SamplePoint> points, int depth) {
        if (points.isEmpty()) return null;
        int axis = depth % 3;
        points.sort((a, b) -> Double.compare(getCoord(a.position, axis), getCoord(b.position, axis)));
        int mid = points.size() / 2;
        KDNode node = new KDNode(points.get(mid), axis);
        node.left = buildKDTree(points.subList(0, mid), depth + 1);
        node.right = buildKDTree(points.subList(mid + 1, points.size()), depth + 1);
        return node;
    }

    private void searchNearest(KDNode node, Vec3 target, int k, int depth, PriorityQueue<Neighbor> pq) {
        if (node == null) return;

        double dist = target.distanceTo(node.point.position);
        pq.add(new Neighbor(node, dist));
        if (pq.size() > k) pq.poll();

        int axis = node.axis;
        double diff = getCoord(target, axis) - getCoord(node.point.position, axis);

        KDNode near = diff < 0 ? node.left : node.right;
        KDNode far = diff < 0 ? node.right : node.left;

        searchNearest(near, target, k, depth + 1, pq);
        if (pq.size() < k || Math.abs(diff) < pq.peek().distance) {
            searchNearest(far, target, k, depth + 1, pq);
        }
    }

    private double getCoord(Vec3 v, int axis) {
        return axis == 0 ? v.x : (axis == 1 ? v.y : v.z);
    }

    private static class Neighbor {
        KDNode node;
        double distance;
        Neighbor(KDNode n, double d) { this.node = n; this.distance = d; }
    }

    // --- NBT序列化 ---

    public ListTag toNBT() {
        ListTag curveTag = new ListTag();
        for (var segment : segments) {
            ListTag parameters = new ListTag();
            if (segment instanceof LineSegment line) {
                parameters.add(vec2NBT(line.getStart()));
                parameters.add(vec2NBT(line.getEnd()));
            } else if (segment instanceof BezierSegment bezier) {
                parameters.add(vec2NBT(bezier.getP0()));
                parameters.add(vec2NBT(bezier.getP1()));
                parameters.add(vec2NBT(bezier.getP2()));
                parameters.add(vec2NBT(bezier.getP3()));
            }
            curveTag.add(parameters);
        }
        return curveTag;
    }

    public static CurveRoute fromNBT(ListTag curveTag) {
        CurveRoute curve = new CurveRoute();
        for (int i = 0; i < curveTag.size(); i++) {
            ListTag parameters = curveTag.getList(i);
            if (parameters.size() == 2) {
                Vec3 start = nbt2Vec((ListTag) parameters.get(0));
                Vec3 end = nbt2Vec((ListTag) parameters.get(1));
                curve.segments.add(new LineSegment(start, end));
            } else if (parameters.size() == 4) {
                Vec3 cp0 = nbt2Vec((ListTag) parameters.get(0));
                Vec3 cp1 = nbt2Vec((ListTag) parameters.get(1));
                Vec3 cp2 = nbt2Vec((ListTag) parameters.get(2));
                Vec3 cp3 = nbt2Vec((ListTag) parameters.get(3));
                curve.segments.add(new BezierSegment(cp0, cp1, cp2, cp3));
            }
        }
        curve.buildSamplePoints();
        return curve;
    }

    /**
     * 帧信息类，包含最近点、切线和参数
     */
    public static class Frame {
        public final Vec3 nearestPoint;
        public final Vec3 tangent;
        public final Vec3 tangent0;     // XZ平面切线
        public final Vec3 normal0;      // 法线（向上）
        public final Vec3 binormal0;    // 副法线
        public final double globalT;    // 全局参数 [0,1]
        public final double localU;     // 局部参数

        public Frame(Vec3 pos, Vec3 tangent, double globalT, double localU) {
            this.nearestPoint = pos;
            this.tangent = tangent;
            this.tangent0 = new Vec3(tangent.x, 0, tangent.z).normalize();
            this.normal0 = new Vec3(0, 1, 0);
            this.binormal0 = tangent0.cross(normal0);
            this.globalT = globalT;
            this.localU = localU;
        }
    }

    private static ListTag vec2NBT(Vec3 point) {
        ListTag pointTag = new ListTag();
        pointTag.add(DoubleTag.valueOf(point.x));
        pointTag.add(DoubleTag.valueOf(point.y));
        pointTag.add(DoubleTag.valueOf(point.z));
        return pointTag;
    }

    private static Vec3 nbt2Vec(ListTag pointTag) {
        return new Vec3(pointTag.getDouble(0), pointTag.getDouble(1), pointTag.getDouble(2));
    }
}
