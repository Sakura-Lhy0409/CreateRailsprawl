package com.skua.createrailsprawl.client.map;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 铁路网地图界面
 */
public class RailwayMapScreen extends Screen {
    private static final ResourceLocation MAP_TEXTURE = new ResourceLocation("createrailsprawl", "textures/gui/railway_map.png");
    
    // 颜色常量
    private static final int COLOR_BACKGROUND = 0xFF1a1a2e;
    private static final int COLOR_GRID = 0x40FFFFFF;
    private static final int COLOR_STATION = 0xFFFFD700;
    private static final int COLOR_PLANNED = 0xFF888888;
    private static final int COLOR_GENERATING = 0xFFFFAA00;
    private static final int COLOR_COMPLETED = 0xFF00FF00;
    private static final int COLOR_PLAYER = 0xFFFF4444;
    private static final int COLOR_TEXT = 0xFFFFFFFF;

    private RailwayMapSnapshot snapshot = RailwayMapSnapshot.empty();
    
    // 视图状态
    private double viewCenterX, viewCenterZ;
    private double viewScale = 1.0;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 10.0;
    
    // 拖拽状态
    private boolean dragging = false;
    private double dragStartX, dragStartY;
    private double dragStartCenterX, dragStartCenterZ;
    
    // 布局
    private int mapX, mapY, mapW, mapH;
    private static final int PADDING = 20;

    public RailwayMapScreen() {
        super(Component.translatable("gui.createrailsprawl.railway_map.title"));
    }

    @Override
    protected void init() {
        super.init();
        computeMapRect();
        
        Minecraft mc = this.minecraft;
        if (mc != null && mc.player != null) {
            viewCenterX = mc.player.getX();
            viewCenterZ = mc.player.getZ();
        }
        
        requestSnapshot();
    }

    private void computeMapRect() {
        mapX = PADDING;
        mapY = PADDING;
        mapW = this.width - PADDING * 2;
        mapH = this.height - PADDING * 2;
    }

    private void requestSnapshot() {
        Minecraft mc = this.minecraft;
        if (mc == null) return;
        
        MinecraftServer server = mc.getSingleplayerServer();
        if (server != null) {
            ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
            if (level != null) {
                final ServerLevel levelFinal = level;
                CompletableFuture.supplyAsync(() -> RailwayMapDataCollector.build(levelFinal))
                    .thenAccept(snap -> mc.execute(() -> this.snapshot = snap));
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        
        // 绘制地图背景
        g.fill(mapX, mapY, mapX + mapW, mapY + mapH, COLOR_BACKGROUND);
        
        // 标题
        g.drawCenteredString(this.font, this.getTitle(), this.width / 2, 6, COLOR_TEXT);

        g.enableScissor(mapX, mapY, mapX + mapW, mapY + mapH);
        
        // 绘制网格
        renderGrid(g);
        
        // 绘制连接线
        renderConnections(g);
        
        // 绘制铁路折线
        renderRailPolylines(g);
        
        // 绘制车站
        renderStations(g, mouseX, mouseY);
        
        // 绘制玩家位置
        renderPlayer(g);
        
        g.disableScissor();

        // 绘制图例
        renderLegend(g);
        
        // 绘制悬停提示
        renderHoverTooltip(g, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderGrid(GuiGraphics g) {
        int gridSize = (int) (256 * viewScale);
        if (gridSize < 20) gridSize = (int) (512 * viewScale);
        if (gridSize < 20) gridSize = (int) (1024 * viewScale);
        if (gridSize < 10) return;

        int startWorldX = (int) (viewCenterX - mapW / 2.0 / viewScale);
        int startWorldZ = (int) (viewCenterZ - mapH / 2.0 / viewScale);
        int gridStartX = (startWorldX / 256) * 256;
        int gridStartZ = (startWorldZ / 256) * 256;

        for (int wx = gridStartX; wx < startWorldX + mapW / viewScale + 256; wx += 256) {
            int sx = worldToScreenX(wx);
            if (sx >= mapX && sx <= mapX + mapW) {
                g.vLine(sx, mapY, mapY + mapH, COLOR_GRID);
            }
        }
        for (int wz = gridStartZ; wz < startWorldZ + mapH / viewScale + 256; wz += 256) {
            int sy = worldToScreenY(wz);
            if (sy >= mapY && sy <= mapY + mapH) {
                g.hLine(mapX, mapX + mapW, sy, COLOR_GRID);
            }
        }
    }

    private void renderConnections(GuiGraphics g) {
        for (RailwayMapSnapshot.ConnectionInfo conn : snapshot.connections()) {
            int x1 = worldToScreenX(conn.from().getX());
            int y1 = worldToScreenY(conn.from().getZ());
            int x2 = worldToScreenX(conn.to().getX());
            int y2 = worldToScreenY(conn.to().getZ());
            
            int color = switch (conn.status()) {
                case PLANNED -> COLOR_PLANNED;
                case GENERATING -> COLOR_GENERATING;
                case COMPLETED -> COLOR_COMPLETED;
            };
            
            drawLine(g, x1, y1, x2, y2, color);
        }
    }

    private void renderRailPolylines(GuiGraphics g) {
        for (List<BlockPos> polyline : snapshot.railPolylines()) {
            for (int i = 0; i < polyline.size() - 1; i++) {
                BlockPos p1 = polyline.get(i);
                BlockPos p2 = polyline.get(i + 1);
                int x1 = worldToScreenX(p1.getX());
                int y1 = worldToScreenY(p1.getZ());
                int x2 = worldToScreenX(p2.getX());
                int y2 = worldToScreenY(p2.getZ());
                drawLine(g, x1, y1, x2, y2, COLOR_COMPLETED);
            }
        }
    }

    private void renderStations(GuiGraphics g, int mouseX, int mouseY) {
        int pointSize = Math.max(4, (int) (6 * viewScale));
        for (RailwayMapSnapshot.StationInfo station : snapshot.stations()) {
            int sx = worldToScreenX(station.pos().getX());
            int sy = worldToScreenY(station.pos().getZ());
            
            if (sx >= mapX - pointSize && sx <= mapX + mapW + pointSize &&
                sy >= mapY - pointSize && sy <= mapY + mapH + pointSize) {
                g.fill(sx - pointSize/2, sy - pointSize/2, sx + pointSize/2, sy + pointSize/2, COLOR_STATION);
            }
        }
    }

    private void renderPlayer(GuiGraphics g) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        
        int px = worldToScreenX((int) this.minecraft.player.getX());
        int py = worldToScreenY((int) this.minecraft.player.getZ());
        
        if (px >= mapX && px <= mapX + mapW && py >= mapY && py <= mapY + mapH) {
            int size = 6;
            g.fill(px - size/2, py - size/2, px + size/2, py + size/2, COLOR_PLAYER);
        }
    }

    private void renderLegend(GuiGraphics g) {
        int legendX = mapX + mapW - 100;
        int legendY = mapY + 10;
        int lineHeight = 12;
        
        g.drawString(this.font, "● 车站: " + snapshot.stationCount(), legendX, legendY, COLOR_STATION);
        legendY += lineHeight;
        g.drawString(this.font, "— 已完成: " + snapshot.completedCount(), legendX, legendY, COLOR_COMPLETED);
        legendY += lineHeight;
        g.drawString(this.font, "— 规划中: " + snapshot.plannedCount(), legendX, legendY, COLOR_PLANNED);
    }

    private void renderHoverTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (mouseX < mapX || mouseX > mapX + mapW || mouseY < mapY || mouseY > mapY + mapH) return;
        
        int pointSize = Math.max(4, (int) (6 * viewScale));
        for (RailwayMapSnapshot.StationInfo station : snapshot.stations()) {
            int sx = worldToScreenX(station.pos().getX());
            int sy = worldToScreenY(station.pos().getZ());
            
            if (Math.abs(mouseX - sx) <= pointSize && Math.abs(mouseY - sy) <= pointSize) {
                String name = station.name() != null ? station.name() : "车站";
                String coords = String.format("(%d, %d)", station.pos().getX(), station.pos().getZ());
                g.renderTooltip(this.font, List.of(Component.literal(name), Component.literal(coords)), 
                    java.util.Optional.empty(), mouseX, mouseY);
                break;
            }
        }
    }

    private void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        // 简单的线段绘制（使用填充矩形模拟）
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps == 0) {
            g.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }
        
        float xInc = (x2 - x1) / (float) steps;
        float yInc = (y2 - y1) / (float) steps;
        float x = x1, y = y1;
        
        for (int i = 0; i <= steps; i++) {
            g.fill((int) x, (int) y, (int) x + 2, (int) y + 2, color);
            x += xInc;
            y += yInc;
        }
    }

    private int worldToScreenX(int worldX) {
        return mapX + mapW / 2 + (int) ((worldX - viewCenterX) * viewScale);
    }

    private int worldToScreenY(int worldZ) {
        return mapY + mapH / 2 + (int) ((worldZ - viewCenterZ) * viewScale);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= mapX && mouseX <= mapX + mapW && mouseY >= mapY && mouseY <= mapY + mapH) {
            double oldScale = viewScale;
            viewScale *= (delta > 0) ? 1.2 : 0.8;
            viewScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, viewScale));
            
            // 以鼠标位置为中心缩放
            if (viewScale != oldScale) {
                double worldX = viewCenterX + (mouseX - mapX - mapW / 2.0) / oldScale;
                double worldZ = viewCenterZ + (mouseY - mapY - mapH / 2.0) / oldScale;
                viewCenterX = worldX - (mouseX - mapX - mapW / 2.0) / viewScale;
                viewCenterZ = worldZ - (mouseY - mapY - mapH / 2.0) / viewScale;
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= mapX && mouseX <= mapX + mapW && mouseY >= mapY && mouseY <= mapY + mapH) {
            dragging = true;
            dragStartX = mouseX;
            dragStartY = mouseY;
            dragStartCenterX = viewCenterX;
            dragStartCenterZ = viewCenterZ;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            viewCenterX = dragStartCenterX - (mouseX - dragStartX) / viewScale;
            viewCenterZ = dragStartCenterZ - (mouseY - dragStartY) / viewScale;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 按 R 键重置视图到玩家位置
        if (keyCode == 82) { // R key
            if (this.minecraft != null && this.minecraft.player != null) {
                viewCenterX = this.minecraft.player.getX();
                viewCenterZ = this.minecraft.player.getZ();
                viewScale = 1.0;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
