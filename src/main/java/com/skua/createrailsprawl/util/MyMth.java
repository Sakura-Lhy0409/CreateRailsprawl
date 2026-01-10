/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.util;

import com.skua.createrailsprawl.planner.RoutePlanner.RegionPos;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import static com.skua.createrailsprawl.planner.RoutePlanner.CHUNK_GROUP_SIZE;

/**
 * 数学工具类
 * 从TongDaRailway借鉴优化
 */
public class MyMth {

    /**
     * 从区域坐标获取区块X坐标
     */
    public static int chunkPosXFromRegionPos(RegionPos regionPos, int chunkIndexX) {
        return regionPos.x() * CHUNK_GROUP_SIZE + chunkIndexX;
    }

    /**
     * 从区域坐标获取区块Z坐标
     */
    public static int chunkPosZFromRegionPos(RegionPos regionPos, int chunkIndexZ) {
        return regionPos.z() * CHUNK_GROUP_SIZE + chunkIndexZ;
    }

    /**
     * 从区块坐标获取区域坐标
     */
    public static RegionPos regionPosFromChunkPos(ChunkPos chunkPos) {
        return new RegionPos(Math.floorDiv(chunkPos.x, CHUNK_GROUP_SIZE), Math.floorDiv(chunkPos.z, CHUNK_GROUP_SIZE));
    }

    public static ChunkPos getChunkPos(int x, int z) {
        return new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
    }

    /**
     * 区域内坐标转世界坐标
     */
    public static Vec3 inRegionPos2WorldPos(RegionPos regionPos, Vec3 vec3) {
        return vec3.add(new Vec3(regionPos.x() * CHUNK_GROUP_SIZE * 16, 0, regionPos.z() * CHUNK_GROUP_SIZE * 16));
    }

    public static int getSign(double number) {
        return number > 0 ? 1 : (number < 0 ? -1 : 0);
    }

    /**
     * 绕Y轴旋转向量（仅支持45度倍数）
     * @param vec 输入向量
     * @param cross 叉积值，决定旋转方向
     * @param angleDeg 旋转角度（度），仅支持0、45、90、135等
     * @return 旋转后的Vec3
     */
    public static Vec3 rotateAroundY(Vec3 vec, double cross, double angleDeg) {
        int angle = ((int) Math.round(angleDeg / 45.0) * 45) % 360;
        if (angle < 0) angle += 360;

        int rotateAngle;
        if (cross > 0) {
            rotateAngle = angle;
        } else if (cross < 0) {
            rotateAngle = -angle;
        } else {
            rotateAngle = 0;
        }

        double x = vec.x;
        double z = vec.z;

        int steps = (rotateAngle / 45) % 8;
        if (steps < 0) steps += 8;

        for (int i = 0; i < steps; i++) {
            double newX, newZ;
            double sqrt2_2 = 0.7071067811865476;

            if (x == 1 && z == 0) { newX = sqrt2_2; newZ = sqrt2_2; }
            else if (Math.abs(x - sqrt2_2) < 1e-10 && Math.abs(z - sqrt2_2) < 1e-10) { newX = 0; newZ = 1; }
            else if (x == 0 && z == 1) { newX = -sqrt2_2; newZ = sqrt2_2; }
            else if (Math.abs(x + sqrt2_2) < 1e-10 && Math.abs(z - sqrt2_2) < 1e-10) { newX = -1; newZ = 0; }
            else if (x == -1 && z == 0) { newX = -sqrt2_2; newZ = -sqrt2_2; }
            else if (Math.abs(x + sqrt2_2) < 1e-10 && Math.abs(z + sqrt2_2) < 1e-10) { newX = 0; newZ = -1; }
            else if (x == 0 && z == -1) { newX = sqrt2_2; newZ = -sqrt2_2; }
            else if (Math.abs(x - sqrt2_2) < 1e-10 && Math.abs(z + sqrt2_2) < 1e-10) { newX = 1; newZ = 0; }
            else {
                return new Vec3(x, 0, z);
            }

            x = newX;
            z = newZ;
        }

        return new Vec3(x, 0, z);
    }

    /**
     * 获取曲线起点
     */
    public static Vec3 getCurveStart(BlockPos pos, Vec3 axis) {
        boolean vertical = axis.y != 0;
        return VecHelper.getCenterOf(pos)
                .add(0, (vertical ? 0 : -.5f), 0)
                .add(axis.scale(.5));
    }

    /**
     * 分割函数：返回-1、0或1
     */
    public static int splitFunc(double x) {
        if (x < 0.0001 && x > -0.0001)
            return 0;
        if (x > 0)
            return 1;
        else if (x < 0)
            return -1;
        return 0;
    }

    /**
     * 自定义ceil函数
     */
    public static Vec3i myCeil(Vec3 v) {
        int x = (int) (v.x > 0 ? Math.ceil(v.x) : Math.floor(v.x));
        int y = (int) (v.y > 0 ? Math.ceil(v.y) : Math.floor(v.y));
        int z = (int) (v.z > 0 ? Math.ceil(v.z) : Math.floor(v.z));
        return new Vec3i(x, y, z);
    }
}
