/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端HUD渲染器
 * 显示铁轨生成进度条和状态信息
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = CreateRailsprawl.MOD_ID, value = Dist.CLIENT)
public class RailwayHudRenderer {

    private static boolean hudEnabled = true;
    private static boolean isGenerating = false;
    private static String currentTaskId = "";
    private static int currentChunk = 0;
    private static int totalChunks = 0;
    private static int trackSegments = 0;
    private static String terrainType = "";
    private static String biomeCategory = "plains";

    // 完成后显示计时
    private static long completeTime = 0;
    private static int completedTracks = 0;
    private static int completedCurves = 0;
    private static int completedStations = 0;

    /**
     * 更新进度（从服务端数据包调用）
     */
    public static void updateProgress(NetworkHandler.ProgressUpdatePacket packet) {
        isGenerating = true;
        currentTaskId = packet.taskId;
        currentChunk = packet.currentChunk;
        totalChunks = packet.totalChunks;
        trackSegments = packet.trackSegments;
        terrainType = packet.terrainType;
    }

    /**
     * 任务完成（从服务端数据包调用）
     */
    public static void onTaskComplete(NetworkHandler.TaskCompletePacket packet) {
        isGenerating = false;
        completeTime = System.currentTimeMillis();
        completedTracks = packet.trackSegments;
        completedCurves = packet.curves;
        completedStations = packet.stations;

        // 发送聊天消息
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(
                    String.format("§a铁轨网络生成完成！§r 共创建 %d 段轨道 + %d 个弧形转弯 + %d 个站台",
                            packet.trackSegments, packet.curves, packet.stations)));
        }
    }

    /**
     * 切换HUD显示
     */
    public static void toggleHud() {
        hudEnabled = !hudEnabled;
    }

    /**
     * 设置群系类别（用于颜色适配）
     */
    public static void setBiomeCategory(String category) {
        biomeCategory = category;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!hudEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        // 显示生成进度
        if (isGenerating) {
            renderProgressHud(graphics, screenWidth);
        }
        // 完成后显示3秒
        else if (completeTime > 0 && System.currentTimeMillis() - completeTime < 3000) {
            renderCompleteHud(graphics, screenWidth);
        }
    }

    /**
     * 渲染进度HUD
     */
    private static void renderProgressHud(GuiGraphics graphics, int screenWidth) {
        int x = screenWidth - 160;
        int y = 10;

        // 背景
        graphics.fill(x - 5, y - 5, x + 155, y + 45, 0x80000000);

        // 标题
        graphics.drawString(Minecraft.getInstance().font,
                "§e铁轨生成中", x, y, 0xFFFFFF);

        // 进度条
        int barWidth = 140;
        int barHeight = 10;
        int barY = y + 12;

        // 进度条背景
        graphics.fill(x, barY, x + barWidth, barY + barHeight, 0xFF333333);

        // 进度条填充（颜色根据群系变化）
        int progressColor = getBiomeColor();
        int progress = totalChunks > 0 ? (currentChunk * barWidth / totalChunks) : 0;
        graphics.fill(x, barY, x + progress, barY + barHeight, progressColor);

        // 进度文字
        String progressText = String.format("%d/%d 区块", currentChunk, totalChunks);
        graphics.drawString(Minecraft.getInstance().font,
                progressText, x, barY + barHeight + 3, 0xAAAAAA);

        // 当前构建类型
        String buildText = "当前构建: " + terrainType;
        graphics.drawString(Minecraft.getInstance().font,
                buildText, x, barY + barHeight + 14, 0x88FFFF);
    }

    /**
     * 渲染完成HUD（淡出效果）
     */
    private static void renderCompleteHud(GuiGraphics graphics, int screenWidth) {
        long elapsed = System.currentTimeMillis() - completeTime;
        float alpha = 1.0f - (elapsed / 3000.0f);

        int x = screenWidth - 160;
        int y = 10;

        int bgAlpha = (int) (128 * alpha);
        int textAlpha = (int) (255 * alpha);

        // 背景
        graphics.fill(x - 5, y - 5, x + 155, y + 35, (bgAlpha << 24));

        // 完成文字
        int color = (textAlpha << 24) | 0x00FF00;
        graphics.drawString(Minecraft.getInstance().font,
                "§a✓ 生成完成", x, y, color);

        String statsText = String.format("%d轨道 %d转弯 %d站台",
                completedTracks, completedCurves, completedStations);
        graphics.drawString(Minecraft.getInstance().font,
                statsText, x, y + 12, (textAlpha << 24) | 0xFFFFFF);
    }

    /**
     * 根据群系获取进度条颜色
     */
    private static int getBiomeColor() {
        return switch (biomeCategory) {
            case "desert" -> 0xFFD4A017;      // 暖黄色
            case "taiga", "jungle" -> 0xFF228B22;  // 翠绿色
            case "snowy" -> 0xFFFFFFFF;       // 纯白色
            case "nether" -> 0xFF8B0000;      // 暗红色
            case "end" -> 0xFF9932CC;         // 紫色
            default -> 0xFF4169E1;            // 蓝色
        };
    }
}
