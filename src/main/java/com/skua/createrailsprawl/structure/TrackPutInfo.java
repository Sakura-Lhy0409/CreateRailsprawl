/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.structure;

import com.simibubi.create.content.trains.track.TrackShape;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 轨道放置信息
 * 封装单个轨道段的所有放置信息
 */
public record TrackPutInfo(
        BlockPos pos,           // 轨道起点位置
        TrackShape shape,       // 起点轨道形状
        BezierInfo bezier,      // 贝塞尔曲线信息 (null表示直线)
        TrackShape endShape     // 终点轨道形状
) {
    /**
     * 根据方向创建TrackPutInfo
     */
    public static TrackPutInfo getByDir(BlockPos pos, Vec3 dir, BezierInfo bezier) {
        TrackShape shape = getShape(dir);
        TrackShape endShape = shape;
        if (bezier != null) {
            endShape = getShape(bezier.endAxis());
        }
        return new TrackPutInfo(pos, shape, bezier, endShape);
    }

    /**
     * 根据方向向量确定轨道形状
     */
    private static TrackShape getShape(Vec3 dir) {
        if (Math.abs(dir.x) < 1e-6) {
            return TrackShape.ZO;  // Z轴方向
        } else if (Math.abs(dir.z) < 1e-6) {
            return TrackShape.XO;  // X轴方向
        } else if (dir.x * dir.z > 0) {
            return TrackShape.PD;  // 正对角线
        } else {
            return TrackShape.ND;  // 负对角线
        }
    }

    /**
     * 是否为曲线轨道
     */
    public boolean isCurve() {
        return bezier != null;
    }

    /**
     * 获取终点位置
     */
    public BlockPos getEndPos() {
        if (bezier == null) return pos;
        Vec3 offset = bezier.endOffset();
        return pos.offset((int) offset.x, (int) offset.y, (int) offset.z);
    }

    // --- NBT序列化 ---

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putString("shape", shape.name());
        tag.putString("endShape", endShape.name());
        if (bezier != null) {
            tag.put("bezier", bezier.toNBT());
        }
        return tag;
    }

    public static TrackPutInfo fromNBT(CompoundTag tag) {
        BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
        TrackShape shape = TrackShape.valueOf(tag.getString("shape"));
        TrackShape endShape = TrackShape.valueOf(tag.getString("endShape"));
        BezierInfo bezier = null;
        if (tag.contains("bezier")) {
            bezier = BezierInfo.fromNBT(tag.getCompound("bezier"));
        }
        return new TrackPutInfo(pos, shape, bezier, endShape);
    }

    public static ListTag listToNBT(List<TrackPutInfo> list) {
        ListTag listTag = new ListTag();
        for (TrackPutInfo info : list) {
            listTag.add(info.toNBT());
        }
        return listTag;
    }

    public static List<TrackPutInfo> listFromNBT(ListTag listTag) {
        List<TrackPutInfo> list = new ArrayList<>();
        for (Tag tag : listTag) {
            list.add(fromNBT((CompoundTag) tag));
        }
        return list;
    }

    /**
     * 贝塞尔曲线信息
     */
    public record BezierInfo(
            Vec3 start,       // 起点世界坐标
            Vec3 startAxis,   // 起点切线方向
            Vec3 endOffset,   // 终点相对起点的偏移
            Vec3 endAxis      // 终点切线方向
    ) {
        public CompoundTag toNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("sx", start.x);
            tag.putDouble("sy", start.y);
            tag.putDouble("sz", start.z);
            tag.putDouble("sax", startAxis.x);
            tag.putDouble("say", startAxis.y);
            tag.putDouble("saz", startAxis.z);
            tag.putDouble("eox", endOffset.x);
            tag.putDouble("eoy", endOffset.y);
            tag.putDouble("eoz", endOffset.z);
            tag.putDouble("eax", endAxis.x);
            tag.putDouble("eay", endAxis.y);
            tag.putDouble("eaz", endAxis.z);
            return tag;
        }

        public static BezierInfo fromNBT(CompoundTag tag) {
            return new BezierInfo(
                    new Vec3(tag.getDouble("sx"), tag.getDouble("sy"), tag.getDouble("sz")),
                    new Vec3(tag.getDouble("sax"), tag.getDouble("say"), tag.getDouble("saz")),
                    new Vec3(tag.getDouble("eox"), tag.getDouble("eoy"), tag.getDouble("eoz")),
                    new Vec3(tag.getDouble("eax"), tag.getDouble("eay"), tag.getDouble("eaz"))
            );
        }
    }
}
