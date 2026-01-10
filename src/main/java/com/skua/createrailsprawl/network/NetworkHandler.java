/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.network;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.task.RailwayTaskManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

/**
 * 网络通信处理器
 * 处理客户端-服务端数据同步
 */
public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CreateRailsprawl.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        // 进度更新包 (S->C)
        CHANNEL.registerMessage(packetId++, ProgressUpdatePacket.class,
                ProgressUpdatePacket::encode,
                ProgressUpdatePacket::decode,
                ProgressUpdatePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // 任务完成包 (S->C)
        CHANNEL.registerMessage(packetId++, TaskCompletePacket.class,
                TaskCompletePacket::encode,
                TaskCompletePacket::decode,
                TaskCompletePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));

        // HUD切换包 (C->S)
        CHANNEL.registerMessage(packetId++, HudTogglePacket.class,
                HudTogglePacket::encode,
                HudTogglePacket::decode,
                HudTogglePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));

        CreateRailsprawl.LOGGER.info("[RailwayMod] 网络通道注册完成");
    }

    /**
     * 发送进度更新到玩家
     */
    public static void sendProgressUpdate(ServerPlayer player, RailwayTaskManager.GenerationTask task) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new ProgressUpdatePacket(
                        task.taskId,
                        task.currentIndex,
                        task.chunks.size(),
                        task.result.trackSegments,
                        getCurrentTerrainType(task)
                ));
    }

    /**
     * 发送任务完成通知
     */
    public static void sendTaskComplete(ServerPlayer player, RailwayTaskManager.GenerationTask task) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new TaskCompletePacket(
                        task.taskId,
                        task.result.trackSegments,
                        task.result.curves,
                        task.result.stations,
                        task.result.bridges,
                        task.result.tunnels
                ));
    }

    private static String getCurrentTerrainType(RailwayTaskManager.GenerationTask task) {
        // 简化实现，实际应从生成结果获取
        if (task.result.tunnels > 0) return "隧道";
        if (task.result.bridges > 0) return "桥梁";
        return "平地";
    }

    /**
     * 进度更新数据包
     */
    public static class ProgressUpdatePacket {
        public final String taskId;
        public final int currentChunk;
        public final int totalChunks;
        public final int trackSegments;
        public final String terrainType;

        public ProgressUpdatePacket(String taskId, int currentChunk, int totalChunks,
                                    int trackSegments, String terrainType) {
            this.taskId = taskId;
            this.currentChunk = currentChunk;
            this.totalChunks = totalChunks;
            this.trackSegments = trackSegments;
            this.terrainType = terrainType;
        }

        public static void encode(ProgressUpdatePacket msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.taskId);
            buf.writeInt(msg.currentChunk);
            buf.writeInt(msg.totalChunks);
            buf.writeInt(msg.trackSegments);
            buf.writeUtf(msg.terrainType);
        }

        public static ProgressUpdatePacket decode(FriendlyByteBuf buf) {
            return new ProgressUpdatePacket(
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readUtf()
            );
        }

        public static void handle(ProgressUpdatePacket msg, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                // 客户端处理 - 更新HUD
                com.skua.createrailsprawl.client.RailwayHudRenderer.updateProgress(msg);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /**
     * 任务完成数据包
     */
    public static class TaskCompletePacket {
        public final String taskId;
        public final int trackSegments;
        public final int curves;
        public final int stations;
        public final int bridges;
        public final int tunnels;

        public TaskCompletePacket(String taskId, int trackSegments, int curves,
                                  int stations, int bridges, int tunnels) {
            this.taskId = taskId;
            this.trackSegments = trackSegments;
            this.curves = curves;
            this.stations = stations;
            this.bridges = bridges;
            this.tunnels = tunnels;
        }

        public static void encode(TaskCompletePacket msg, FriendlyByteBuf buf) {
            buf.writeUtf(msg.taskId);
            buf.writeInt(msg.trackSegments);
            buf.writeInt(msg.curves);
            buf.writeInt(msg.stations);
            buf.writeInt(msg.bridges);
            buf.writeInt(msg.tunnels);
        }

        public static TaskCompletePacket decode(FriendlyByteBuf buf) {
            return new TaskCompletePacket(
                    buf.readUtf(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            );
        }

        public static void handle(TaskCompletePacket msg, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                com.skua.createrailsprawl.client.RailwayHudRenderer.onTaskComplete(msg);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /**
     * HUD切换数据包
     */
    public static class HudTogglePacket {
        public final boolean enabled;

        public HudTogglePacket(boolean enabled) {
            this.enabled = enabled;
        }

        public static void encode(HudTogglePacket msg, FriendlyByteBuf buf) {
            buf.writeBoolean(msg.enabled);
        }

        public static HudTogglePacket decode(FriendlyByteBuf buf) {
            return new HudTogglePacket(buf.readBoolean());
        }

        public static void handle(HudTogglePacket msg, java.util.function.Supplier<net.minecraftforge.network.NetworkEvent.Context> ctx) {
            ctx.get().setPacketHandled(true);
        }
    }
}
