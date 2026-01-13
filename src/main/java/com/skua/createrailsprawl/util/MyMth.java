package com.skua.createrailsprawl.util;

import com.skua.createrailsprawl.railway.RegionPos;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import static com.skua.createrailsprawl.CreateRailsprawl.CHUNK_GROUP_SIZE;

public class MyMth {
    public static int chunkPosXFromRegionPos(RegionPos regionPos, int chunkIndexX) {
        return regionPos.x() * CHUNK_GROUP_SIZE + chunkIndexX;
    }

    public static int chunkPosZFromRegionPos(RegionPos regionPos, int chunkIndexZ) {
        return regionPos.z() * CHUNK_GROUP_SIZE + chunkIndexZ;
    }

    public static RegionPos regionPosFromChunkPos(ChunkPos chunkPos) {
        return new RegionPos(Math.floorDiv(chunkPos.x, CHUNK_GROUP_SIZE), Math.floorDiv(chunkPos.z, CHUNK_GROUP_SIZE));
    }

    public static ChunkPos getChunkPos(int x, int z) {
        return new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
    }

    public static Vec3 inRegionPos2WorldPos(RegionPos regionPos, Vec3 vec3) {
        return vec3.add(new Vec3(regionPos.x()*CHUNK_GROUP_SIZE*16, 0, regionPos.z()*CHUNK_GROUP_SIZE*16));
    }

    public static int getSign(double number) {
        return number > 0 ? 1 : (number < 0 ? -1 : 0);
    }

    public static Vec3 rotateAroundY(Vec3 vec, double cross, double angleDeg) {
        int angle = ((int)Math.round(angleDeg / 45.0) * 45) % 360;
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

            if (x == 1 && z == 0) { newX = 0.7071067811865476; newZ = 0.7071067811865476; }
            else if (Math.abs(x - 0.7071067811865476) < 1e-10 && Math.abs(z - 0.7071067811865476) < 1e-10) { newX = 0; newZ = 1; }
            else if (x == 0 && z == 1) { newX = -0.7071067811865476; newZ = 0.7071067811865476; }
            else if (Math.abs(x + 0.7071067811865476) < 1e-10 && Math.abs(z - 0.7071067811865476) < 1e-10) { newX = -1; newZ = 0; }
            else if (x == -1 && z == 0) { newX = -0.7071067811865476; newZ = -0.7071067811865476; }
            else if (Math.abs(x + 0.7071067811865476) < 1e-10 && Math.abs(z + 0.7071067811865476) < 1e-10) { newX = 0; newZ = -1; }
            else if (x == 0 && z == -1) { newX = 0.7071067811865476; newZ = -0.7071067811865476; }
            else if (Math.abs(x - 0.7071067811865476) < 1e-10 && Math.abs(z + 0.7071067811865476) < 1e-10) { newX = 1; newZ = 0; }
            else {
                return new Vec3(x, 0, z);
            }

            x = newX;
            z = newZ;
        }

        return new Vec3(x, 0, z);
    }

    public static Vec3 getCurveStart(BlockPos pos, Vec3 axis) {
        boolean vertical = axis.y != 0;
        return VecHelper.getCenterOf(pos)
                .add(0, (vertical ? 0 : -.5f), 0)
                .add(axis.scale(.5));
    }

    public static int splitFunc(double x) {
        if (x < 0.0001 && x > -0.0001)
            return 0;
        if (x > 0)
            return 1;
        else if (x < 0)
            return -1;
        return 0;
    }

    public static Vec3i myCeil(Vec3 v) {
        int x = (int) (v.x > 0 ? Math.ceil(v.x) : Math.floor(v.x));
        int y = (int) (v.y > 0 ? Math.ceil(v.y) : Math.floor(v.y));
        int z = (int) (v.z > 0 ? Math.ceil(v.z) : Math.floor(v.z));
        return new Vec3i(x, y, z);
    }
}
