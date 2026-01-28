package com.skua.createrailsprawl.client.map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * 铁路网地图界面
 */
public class RailwayMapScreen extends Screen {
    private static final ResourceLocation MAP_TEXTURE = new ResourceLocation("createrailsprawl", "textures/gui/railway_map.png");
    
    // 颜色常量
    private static final int COLOR_BACKGROUND = 0xFF0F111A;
    private static final int COLOR_PANEL = 0xFF151A26;
    private static final int COLOR_PANEL_SOFT = 0xCC151A26;
    private static final int COLOR_ACCENT = 0xFF4CC9F0;
    private static final int COLOR_GRID = 0x26FFFFFF;
    private static final int COLOR_STATION = 0xFFFFD369;
    private static final int COLOR_PLANNED = 0xFF7B8094;
    private static final int COLOR_GENERATING = 0xFFFFB86B;
    private static final int COLOR_COMPLETED = 0xFF6EE7B7;
    private static final int COLOR_PLAYER = 0xFFF87171;
    private static final int COLOR_TEXT = 0xFFE5E7EB;
    private static final int COLOR_TEXT_MUTED = 0xFF9AA0AE;

    private RailwayMapSnapshot snapshot = RailwayMapSnapshot.empty();
    
    // 视图状态
    private double viewCenterX, viewCenterZ;
    private double viewScale = 1.0;
    private double targetViewCenterX, targetViewCenterZ;
    private double targetViewScale = 1.0;
    private static final double MIN_SCALE = 0.1;
    private static final double MAX_SCALE = 10.0;
    
    // 拖拽状态
    private boolean dragging = false;
    private double dragStartX, dragStartY;
    private double dragStartCenterX, dragStartCenterZ;
    
    // 布局
    private int mapX, mapY, mapW, mapH;
    private int barX, barY, barW, barH;
    private int toolbarX, toolbarY, toolbarW, toolbarH;
    private static final int PADDING = 20;
    private static final int BAR_HEIGHT = 28;
    private static final int TOOLBAR_W = 38;
    private static final int TOOLBAR_GAP = 6;
    private static final int TOOLBAR_BTN = 22;
    
    private Button refreshButton;
    private Button centerButton;
    private Button fitButton;
    private Button settingsButton;
    private Button followButton;
    private Button zoomInButton;
    private Button zoomOutButton;
    private Button resetButton;
    private Button gridToggleButton;
    private Button stationToggleButton;
    private Button railToggleButton;
    private Button linkToggleButton;
    private Button legendToggleButton;

    private final RailwayMapSettings settings = RailwayMapSettings.get();
    private boolean fullscreen = true;
    private int[] starX;
    private int[] starY;
    private int[] starA;
    private int[] starS;

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
            targetViewCenterX = viewCenterX;
            targetViewCenterZ = viewCenterZ;
        }
        fullscreen = settings.isFullscreenDefault();
        initStars();
        
        refreshSnapshot();
        initButtons();
    }

    private void computeMapRect() {
        if (fullscreen) {
            toolbarX = PADDING;
            toolbarY = PADDING + BAR_HEIGHT + 8;
            toolbarW = TOOLBAR_W;
            toolbarH = this.height - PADDING * 2 - BAR_HEIGHT - 8;
            mapX = toolbarX + TOOLBAR_W + TOOLBAR_GAP;
            mapY = toolbarY;
            mapW = this.width - PADDING * 2 - TOOLBAR_W - TOOLBAR_GAP;
            mapH = this.height - PADDING * 2 - BAR_HEIGHT - 8;
            barX = PADDING;
            barY = PADDING;
            barW = this.width - PADDING * 2;
            barH = BAR_HEIGHT;
        } else {
            int winW = (int) (this.width * 0.8);
            int winH = (int) (this.height * 0.75);
            int winX = (this.width - winW) / 2;
            int winY = (this.height - winH) / 2;
            barX = winX + 12;
            barY = winY + 12;
            barW = winW - 24;
            barH = BAR_HEIGHT;
            toolbarW = TOOLBAR_W;
            toolbarX = winX + 12;
            toolbarY = barY + BAR_HEIGHT + 8;
            toolbarH = winH - 24 - BAR_HEIGHT - 8;
            mapX = toolbarX + TOOLBAR_W + TOOLBAR_GAP;
            mapY = toolbarY;
            mapW = winW - 24 - TOOLBAR_W - TOOLBAR_GAP;
            mapH = winH - 24 - BAR_HEIGHT - 8;
        }
    }

    public void refreshSnapshot() {
        Minecraft mc = this.minecraft;
        if (mc == null) return;
        
        MinecraftServer server = mc.getSingleplayerServer();
        if (server != null) {
            ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
            if (level != null) {
                final ServerLevel levelFinal = level;
                int centerX = mc.player != null ? (int) mc.player.getX() : levelFinal.getSharedSpawnPos().getX();
                int centerZ = mc.player != null ? (int) mc.player.getZ() : levelFinal.getSharedSpawnPos().getZ();
                int radius = settings.getViewRadiusBlocks();
                boolean onlyLoaded = settings.isShowLoadedOnly();
                CompletableFuture.supplyAsync(() -> RailwayMapDataCollector.build(levelFinal, centerX, centerZ, radius, onlyLoaded))
                    .thenAccept(snap -> mc.execute(() -> this.snapshot = snap));
            }
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 平滑插值
        float lerpFactor = 0.2f; // 调节平滑度
        viewScale = MapUIUtils.lerp(viewScale, targetViewScale, lerpFactor);
        viewCenterX = MapUIUtils.lerp(viewCenterX, targetViewCenterX, lerpFactor);
        viewCenterZ = MapUIUtils.lerp(viewCenterZ, targetViewCenterZ, lerpFactor);

        this.renderBackground(g);
        
        int bgBase = switch (settings.getTheme()) {
            case DARK -> 0xFF0F111A;
            case LIGHT -> 0xFFF0F3F8;
            case HOLO -> 0xFF081B2B;
        };
        int brightness = settings.getBackgroundBrightness();
        int bgColor = adjustColor(bgBase, brightness);
        g.fill(0, 0, this.width, this.height, bgColor);
        renderStars(g);
        renderAmbientLines(g);
        g.fillGradient(barX, barY, barX + barW, barY + barH, COLOR_PANEL, COLOR_PANEL_SOFT);
        g.drawString(this.font, this.getTitle(), barX + 10, barY + 9, COLOR_TEXT);
        MapUIUtils.drawRoundedPanel(g, toolbarX, toolbarY, toolbarW, toolbarH, COLOR_PANEL_SOFT);
        g.drawString(this.font, Component.translatable("gui.createrailsprawl.map_toolbar.title"), toolbarX + 6, toolbarY + 4, COLOR_TEXT_MUTED);
        
        int drawX = mapX, drawY = mapY, drawW = mapW, drawH = mapH;
        g.fill(drawX, drawY, drawX + drawW, drawY + drawH, COLOR_PANEL);
        g.fill(drawX, drawY, drawX + drawW, drawY + 1, COLOR_ACCENT);

        g.enableScissor(drawX, drawY, drawX + drawW, drawY + drawH);
        
        renderBackgroundDetails(g); // 新增背景细节
        if (settings.isShowGrid()) {
            renderGrid(g);
            renderRulers(g); // 新增坐标尺
        }
        renderTerrainTexture(g);
        if (settings.isShowConnections()) renderConnections(g);
        if (settings.isShowRailPolylines()) renderRailPolylines(g);
        if (settings.isShowStations()) renderStations(g, mouseX, mouseY);
        if (settings.isShowPlayer()) renderPlayer(g);
        
        g.disableScissor();

        renderMapHud(g, mouseX, mouseY);
        renderStatusBar(g);
        
        if (settings.isShowStations()) renderHoverTooltip(g, mouseX, mouseY);

        // 渲染组件（按钮等）
        // 通过调用 super.render 渲染子控件，并覆盖 renderBackground 禁用默认背景
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        // 禁用默认背景渲染，由 render 方法手动处理
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
        int step = getPolylineStep();
        for (List<BlockPos> polyline : snapshot.railPolylines()) {
            for (int i = 0; i < polyline.size() - 1; i += step) {
                BlockPos p1 = polyline.get(i);
                BlockPos p2 = polyline.get(Math.min(i + step, polyline.size() - 1));
                int x1 = worldToScreenX(p1.getX());
                int y1 = worldToScreenY(p1.getZ());
                int x2 = worldToScreenX(p2.getX());
                int y2 = worldToScreenY(p2.getZ());
                if (settings.isOptimizeRender() && !isLineInView(x1, y1, x2, y2)) continue;
                drawLine(g, x1, y1, x2, y2, COLOR_COMPLETED);
            }
        }
    }

    private void renderStations(GuiGraphics g, int mouseX, int mouseY) {
        int pointSize = Math.max(4, (int) (7 * viewScale));
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

    private void renderBackgroundDetails(GuiGraphics g) {
        // 在顶部中央绘制 N
        g.pose().pushPose();
        float scale = 1.5f;
        g.pose().scale(scale, scale, 1.0f);
        int cx = (int) ((mapX + mapW / 2) / scale);
        int cy = (int) ((mapY + 15) / scale);
        g.drawCenteredString(this.font, Component.literal("N"), cx, cy, COLOR_TEXT_MUTED);
        g.pose().popPose();

        // 渲染生成状态水印
        if (snapshot.generatingCount() > 0) {
            g.pose().pushPose();
            g.pose().translate(mapX + mapW - 20, mapY + 20, 0);
            g.pose().scale(0.8f, 0.8f, 1.0f);
            String text = "GENERATING: " + snapshot.generatingCount();
            int tw = this.font.width(text);
            g.drawString(this.font, text, -tw, 0, COLOR_GENERATING);
            g.pose().popPose();
        }
    }

    private void renderRulers(GuiGraphics g) {
        int rulerColor = 0x804CC9F0; // 半透明青色
        int textColor = 0xB0E5E7EB;
        
        // 动态计算步长
        // 屏幕上每隔约 80-150 像素显示一个刻度
        int pixelStep = 100;
        int worldStep = (int) (pixelStep / viewScale);
        
        // 取整逻辑：比如 worldStep 是 123，我们希望取 100；是 256，取 250 或 500
        int step = 100;
        if (worldStep > 10000) step = 10000;
        else if (worldStep > 5000) step = 5000;
        else if (worldStep > 2000) step = 2000;
        else if (worldStep > 1000) step = 1000;
        else if (worldStep > 500) step = 500;
        else if (worldStep > 200) step = 200;
        else if (worldStep > 100) step = 100;
        else if (worldStep > 50) step = 50;
        else step = 10;
        
        // 顶部 X 轴
        int startWorldX = (int) (viewCenterX - mapW / 2.0 / viewScale);
        int endWorldX = (int) (viewCenterX + mapW / 2.0 / viewScale);
        
        int gridStartX = (startWorldX / step) * step;
        
        for (int wx = gridStartX; wx <= endWorldX + step; wx += step) {
            int sx = worldToScreenX(wx);
            if (sx >= mapX && sx <= mapX + mapW) {
                // 刻度线
                g.vLine(sx, mapY, mapY + 5, rulerColor);
                // 数字
                String s = String.valueOf(wx);
                g.pose().pushPose();
                g.pose().translate(sx, mapY + 8, 0);
                g.pose().scale(0.75f, 0.75f, 1.0f);
                g.drawCenteredString(this.font, s, 0, 0, textColor);
                g.pose().popPose();
            }
        }
        
        // 左侧 Z 轴
        int startWorldZ = (int) (viewCenterZ - mapH / 2.0 / viewScale);
        int endWorldZ = (int) (viewCenterZ + mapH / 2.0 / viewScale);
        
        int gridStartZ = (startWorldZ / step) * step;
        
        for (int wz = gridStartZ; wz <= endWorldZ + step; wz += step) {
            int sy = worldToScreenY(wz);
            if (sy >= mapY && sy <= mapY + mapH) {
                // 刻度线
                g.hLine(mapX, mapX + 5, sy, rulerColor);
                // 数字
                String s = String.valueOf(wz);
                g.pose().pushPose();
                g.pose().translate(mapX + 8, sy - 3, 0);
                g.pose().scale(0.75f, 0.75f, 1.0f);
                g.drawString(this.font, s, 0, 0, textColor);
                g.pose().popPose();
            }
        }
    }

    private void renderLegend(GuiGraphics g) {
        // Deprecated, logic moved to renderMapHud
    }

    private void renderStatusBar(GuiGraphics g) {
        // 胶囊状态栏
        int barWidth = 400; // 估算宽度
        int barHeight = 24;
        int x = mapX + (mapW - barWidth) / 2;
        int y = mapY + mapH - barHeight - 10; // 悬浮在底部
        
        MapUIUtils.drawCapsule(g, x, y, barWidth, barHeight, 0xE6151A26); // 半透明深色
        
        int textX = x + 12;
        int textY = y + 8;
        
        if (this.minecraft != null && this.minecraft.player != null) {
            int px = (int) this.minecraft.player.getX();
            int pz = (int) this.minecraft.player.getZ();
            g.drawString(this.font, Component.translatable("gui.createrailsprawl.map_status.pos", px, pz), textX, textY, COLOR_TEXT);
            textX += 90;
        }
        g.drawString(this.font, Component.translatable("gui.createrailsprawl.map_status.scale", String.format("%.2fx", viewScale)), textX, textY, COLOR_TEXT_MUTED);
        textX += 70;
        g.drawString(this.font, Component.translatable("gui.createrailsprawl.map_status.radius", settings.getViewRadiusBlocks()), textX, textY, COLOR_TEXT_MUTED);
        textX += 85;
        g.drawString(this.font, Component.translatable("gui.createrailsprawl.map_status.loaded", Component.translatable(settings.isShowLoadedOnly() ? "options.on" : "options.off")), textX, textY, COLOR_TEXT_MUTED);
    }

    private void renderHoverTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (mouseX < mapX || mouseX > mapX + mapW || mouseY < mapY || mouseY > mapY + mapH) return;
        
        int pointSize = Math.max(4, (int) (7 * viewScale));
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

    private void renderMapHud(GuiGraphics g, int mouseX, int mouseY) {
        // 右上角：设置与状态信息
        int rightPanelW = 168;
        int rightPanelH = 72;
        int rightX = mapX + mapW - rightPanelW - 10;
        int rightY = mapY + 26;
        MapUIUtils.drawRoundedPanel(g, rightX, rightY, rightPanelW, rightPanelH, 0xE6151A26);
        int ry = rightY + 8;
        g.drawString(this.font, Component.translatable("gui.createrailsprawl.map_hud.scale", String.format("%.2fx", viewScale)), rightX + 8, ry, COLOR_TEXT_MUTED);
        ry += 14;
        g.drawString(this.font, Component.translatable("gui.createrailsprawl.map_hud.range", settings.getViewRadiusBlocks()), rightX + 8, ry, COLOR_TEXT_MUTED);
        ry += 14;
        g.drawString(this.font, Component.translatable("gui.createrailsprawl.map_hud.quality", renderQualityText()), rightX + 8, ry, COLOR_TEXT_MUTED);
        ry += 14;
        g.drawString(this.font, Component.translatable("gui.createrailsprawl.map_hud.loaded", Component.translatable(settings.isShowLoadedOnly() ? "options.on" : "options.off")), rightX + 8, ry, COLOR_TEXT_MUTED);

        // 右下角：图例与统计 (原左上角内容移至此处)
        int legendPanelW = 158;
        int legendPanelH = 72;
        int legendX = mapX + mapW - legendPanelW - 10;
        int legendY = mapY + mapH - legendPanelH - 40; // 位于底部状态栏上方
        
        MapUIUtils.drawRoundedPanel(g, legendX, legendY, legendPanelW, legendPanelH, 0xE6151A26);
        int ly = legendY + 8;
        g.drawString(this.font, Component.translatable("gui.createrailsprawl.map_hud.stations", snapshot.stationCount()), legendX + 8, ly, COLOR_STATION);
        ly += 14;
        g.drawString(this.font, Component.translatable("gui.createrailsprawl.map_hud.completed", snapshot.completedCount()), legendX + 8, ly, COLOR_COMPLETED);
        ly += 14;
        g.drawString(this.font, Component.translatable("gui.createrailsprawl.map_hud.planned", snapshot.plannedCount()), legendX + 8, ly, COLOR_PLANNED);
        ly += 14;
        g.drawString(this.font, Component.translatable("gui.createrailsprawl.map_hud.legend"), legendX + 8, ly, COLOR_TEXT_MUTED);

        // 比例尺 (左下角)
        int scaleBarBlocks = 100;
        int barPx = (int) (scaleBarBlocks * viewScale);
        barPx = Math.max(50, Math.min(160, barPx));
        int barX = mapX + 12;
        int barY = mapY + mapH - 22;
        g.hLine(barX, barX + barPx, barY, COLOR_ACCENT);
        g.hLine(barX, barX + barPx, barY + 1, COLOR_ACCENT);
        int actualBlocks = Math.max(1, (int) (barPx / Math.max(0.01, viewScale)));
        g.drawString(this.font, Component.translatable("gui.createrailsprawl.map_hud.scale_bar", actualBlocks), barX, barY - 10, COLOR_TEXT_MUTED);

        // 鼠标位置提示 (右下角，位于图例下方，或者合并)
        // 由于空间原因，鼠标位置可以放左下角比例尺上方，或者跟随鼠标
        if (mouseX >= mapX && mouseX <= mapX + mapW && mouseY >= mapY && mouseY <= mapY + mapH) {
            int wx = (int) Math.round(viewCenterX + (mouseX - mapX - mapW / 2.0) / viewScale);
            int wz = (int) Math.round(viewCenterZ + (mouseY - mapY - mapH / 2.0) / viewScale);
            // 显示在比例尺上方
            g.drawString(this.font, String.format("X: %d Z: %d", wx, wz), barX, barY - 24, COLOR_ACCENT);
        }
    }

    private Component renderQualityText() {
        return Component.translatable(switch (settings.getRenderQuality()) {
            case HIGH -> "gui.createrailsprawl.map_settings.quality_high";
            case MEDIUM -> "gui.createrailsprawl.map_settings.quality_medium";
            case LOW -> "gui.createrailsprawl.map_settings.quality_low";
        });
    }

    private void renderTerrainTexture(GuiGraphics g) {
        int step = Math.max(12, (int) (48 / Math.max(0.2, viewScale)));
        int startWorldX = (int) (viewCenterX - mapW / 2.0 / viewScale);
        int startWorldZ = (int) (viewCenterZ - mapH / 2.0 / viewScale);
        int endWorldX = (int) (viewCenterX + mapW / 2.0 / viewScale);
        int endWorldZ = (int) (viewCenterZ + mapH / 2.0 / viewScale);
        int majorStep = step * 4;
        for (int wz = (startWorldZ / step) * step; wz <= endWorldZ; wz += step) {
            for (int wx = (startWorldX / step) * step; wx <= endWorldX; wx += step) {
                int hash = wx * 73428767 ^ wz * 912931;
                int v = (hash >>> 4) & 0xFF;
                if (v < 160) continue;
                int sx = worldToScreenX(wx);
                int sy = worldToScreenY(wz);
                if (sx < mapX || sx > mapX + mapW || sy < mapY || sy > mapY + mapH) continue;
                int alpha = 0x12 + (v & 0x1F);
                int color = (alpha << 24) | 0x7FA6B8;
                int size = (v & 0x07) == 0 ? 3 : 2;
                g.fill(sx, sy, sx + size, sy + size, color);
                if ((wx % majorStep == 0) || (wz % majorStep == 0)) {
                    g.fill(sx, sy, sx + size + 1, sy + size + 1, 0x1A4CC9F0);
                }
            }
        }
    }

    private void drawLine(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
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
            double oldScale = targetViewScale;
            double factor = switch (settings.getZoomSpeed()) {
                case 1 -> 1.08;
                case 2 -> 1.15;
                default -> 1.25;
            };
            targetViewScale *= (delta > 0) ? factor : 1.0 / factor;
            targetViewScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, targetViewScale));
            
            // 以鼠标位置为中心缩放
            if (targetViewScale != oldScale) {
                double worldX = targetViewCenterX + (mouseX - mapX - mapW / 2.0) / oldScale;
                double worldZ = targetViewCenterZ + (mouseY - mapY - mapH / 2.0) / oldScale;
                targetViewCenterX = worldX - (mouseX - mapX - mapW / 2.0) / targetViewScale;
                targetViewCenterZ = worldZ - (mouseY - mapY - mapH / 2.0) / targetViewScale;
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
        if (keyCode == 82) { // R key
            if (this.minecraft != null && this.minecraft.player != null) {
                targetViewCenterX = this.minecraft.player.getX();
                targetViewCenterZ = this.minecraft.player.getZ();
                targetViewScale = 1.0;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();
        if (settings.isFollowPlayer() && this.minecraft != null && this.minecraft.player != null) {
            targetViewCenterX = this.minecraft.player.getX();
            targetViewCenterZ = this.minecraft.player.getZ();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void initButtons() {
        int btnH = 20;
        int y = barY + 4;
        int x = barX + barW - 10;

        settingsButton = ModernButton.create(Component.translatable("gui.createrailsprawl.map_settings.title"), b -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new RailwayMapSettingsScreen(this));
            }
        }).bounds(x - 48, y, 48, btnH).build();
        settingsButton.setTooltip(Tooltip.create(Component.translatable("gui.createrailsprawl.map_action.settings_tip")));
        x -= 52;

        followButton = ModernButton.create(followLabel(), b -> {
            settings.setFollowPlayer(!settings.isFollowPlayer());
            b.setMessage(followLabel());
        }).bounds(x - 56, y, 56, btnH).build();
        followButton.setTooltip(Tooltip.create(Component.translatable("gui.createrailsprawl.map_action.follow_tip")));
        x -= 60;

        Button fullscreenBtn = ModernButton.create(fullscreenLabel(), b -> {
            fullscreen = !fullscreen;
            computeMapRect();
            this.clearWidgets();
            initButtons();
        }).bounds(x - 42, y, 42, btnH).build();
        fullscreenBtn.setTooltip(Tooltip.create(Component.translatable("gui.createrailsprawl.map_action.fullscreen")));
        this.addRenderableWidget(fullscreenBtn);
        x -= 46;

        fitButton = ModernButton.create(Component.translatable("gui.createrailsprawl.map_action.fit"), b -> fitToSnapshot())
            .bounds(x - 42, y, 42, btnH).build();
        fitButton.setTooltip(Tooltip.create(Component.translatable("gui.createrailsprawl.map_action.fit_tip")));
        x -= 46;

        centerButton = ModernButton.create(Component.translatable("gui.createrailsprawl.map_action.center"), b -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                targetViewCenterX = this.minecraft.player.getX();
                targetViewCenterZ = this.minecraft.player.getZ();
                targetViewScale = 1.0;
            }
        }).bounds(x - 42, y, 42, btnH).build();
        centerButton.setTooltip(Tooltip.create(Component.translatable("gui.createrailsprawl.map_action.center_tip")));
        x -= 46;

        refreshButton = ModernButton.create(Component.translatable("gui.createrailsprawl.map_action.refresh"), b -> refreshSnapshot())
            .bounds(x - 42, y, 42, btnH).build();
        refreshButton.setTooltip(Tooltip.create(Component.translatable("gui.createrailsprawl.map_action.refresh_tip")));

        this.addRenderableWidget(refreshButton);
        this.addRenderableWidget(centerButton);
        this.addRenderableWidget(fitButton);
        this.addRenderableWidget(followButton);
        this.addRenderableWidget(settingsButton);

        int tx = toolbarX + 8;
        int ty = toolbarY + 16;
        zoomInButton = ModernButton.create(Component.translatable("gui.createrailsprawl.map_toolbar.zoom_in"), b -> {
            targetViewScale = Math.min(MAX_SCALE, targetViewScale * 1.2);
        }).bounds(tx, ty, TOOLBAR_BTN, TOOLBAR_BTN).build();
        zoomInButton.setTooltip(Tooltip.create(Component.translatable("gui.createrailsprawl.map_toolbar.zoom_in_tip")));
        ty += TOOLBAR_BTN + 4;
        zoomOutButton = ModernButton.create(Component.translatable("gui.createrailsprawl.map_toolbar.zoom_out"), b -> {
            targetViewScale = Math.max(MIN_SCALE, targetViewScale * 0.8);
        }).bounds(tx, ty, TOOLBAR_BTN, TOOLBAR_BTN).build();
        zoomOutButton.setTooltip(Tooltip.create(Component.translatable("gui.createrailsprawl.map_toolbar.zoom_out_tip")));
        ty += TOOLBAR_BTN + 8;
        resetButton = ModernButton.create(Component.translatable("gui.createrailsprawl.map_toolbar.reset"), b -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                targetViewCenterX = this.minecraft.player.getX();
                targetViewCenterZ = this.minecraft.player.getZ();
                targetViewScale = 1.0;
            }
        }).bounds(tx, ty, TOOLBAR_BTN, TOOLBAR_BTN).build();
        resetButton.setTooltip(Tooltip.create(Component.translatable("gui.createrailsprawl.map_toolbar.reset_tip")));
        ty += TOOLBAR_BTN + 10;

        gridToggleButton = ModernButton.create(toolbarToggleLabel("gui.createrailsprawl.map_toolbar.grid", settings.isShowGrid()), b -> {
            settings.setShowGrid(!settings.isShowGrid());
            b.setMessage(toolbarToggleLabel("gui.createrailsprawl.map_toolbar.grid", settings.isShowGrid()));
        }).bounds(tx, ty, TOOLBAR_BTN, TOOLBAR_BTN).build();
        gridToggleButton.setTooltip(Tooltip.create(Component.translatable("gui.createrailsprawl.map_toolbar.grid_tip")));
        ty += TOOLBAR_BTN + 4;
        stationToggleButton = ModernButton.create(toolbarToggleLabel("gui.createrailsprawl.map_toolbar.station", settings.isShowStations()), b -> {
            settings.setShowStations(!settings.isShowStations());
            b.setMessage(toolbarToggleLabel("gui.createrailsprawl.map_toolbar.station", settings.isShowStations()));
        }).bounds(tx, ty, TOOLBAR_BTN, TOOLBAR_BTN).build();
        stationToggleButton.setTooltip(Tooltip.create(Component.translatable("gui.createrailsprawl.map_toolbar.station_tip")));
        ty += TOOLBAR_BTN + 4;
        railToggleButton = ModernButton.create(toolbarToggleLabel("gui.createrailsprawl.map_toolbar.rail", settings.isShowRailPolylines()), b -> {
            settings.setShowRailPolylines(!settings.isShowRailPolylines());
            b.setMessage(toolbarToggleLabel("gui.createrailsprawl.map_toolbar.rail", settings.isShowRailPolylines()));
        }).bounds(tx, ty, TOOLBAR_BTN, TOOLBAR_BTN).build();
        railToggleButton.setTooltip(Tooltip.create(Component.translatable("gui.createrailsprawl.map_toolbar.rail_tip")));
        ty += TOOLBAR_BTN + 4;
        linkToggleButton = ModernButton.create(toolbarToggleLabel("gui.createrailsprawl.map_toolbar.link", settings.isShowConnections()), b -> {
            settings.setShowConnections(!settings.isShowConnections());
            b.setMessage(toolbarToggleLabel("gui.createrailsprawl.map_toolbar.link", settings.isShowConnections()));
        }).bounds(tx, ty, TOOLBAR_BTN, TOOLBAR_BTN).build();
        linkToggleButton.setTooltip(Tooltip.create(Component.translatable("gui.createrailsprawl.map_toolbar.link_tip")));
        ty += TOOLBAR_BTN + 4;
        legendToggleButton = ModernButton.create(toolbarToggleLabel("gui.createrailsprawl.map_toolbar.legend", settings.isShowLegend()), b -> {
            settings.setShowLegend(!settings.isShowLegend());
            b.setMessage(toolbarToggleLabel("gui.createrailsprawl.map_toolbar.legend", settings.isShowLegend()));
        }).bounds(tx, ty, TOOLBAR_BTN, TOOLBAR_BTN).build();
        legendToggleButton.setTooltip(Tooltip.create(Component.translatable("gui.createrailsprawl.map_toolbar.legend_tip")));

        this.addRenderableWidget(zoomInButton);
        this.addRenderableWidget(zoomOutButton);
        this.addRenderableWidget(resetButton);
        this.addRenderableWidget(gridToggleButton);
        this.addRenderableWidget(stationToggleButton);
        this.addRenderableWidget(railToggleButton);
        this.addRenderableWidget(linkToggleButton);
        this.addRenderableWidget(legendToggleButton);
    }

    private Component followLabel() {
        return Component.translatable("gui.createrailsprawl.map_action.follow",
            Component.translatable(settings.isFollowPlayer() ? "options.on" : "options.off"));
    }

    private Component toolbarToggleLabel(String key, boolean enabled) {
        return Component.translatable(key, Component.translatable(enabled ? "options.on" : "options.off"));
    }
    
    private Component fullscreenLabel() {
        return Component.translatable(fullscreen ? "gui.createrailsprawl.map_action.windowed" : "gui.createrailsprawl.map_action.fullscreen_on");
    }

    private void fitToSnapshot() {
        double centerX = (snapshot.minX() + snapshot.maxX()) / 2.0;
        double centerZ = (snapshot.minZ() + snapshot.maxZ()) / 2.0;
        double spanX = Math.max(1, snapshot.maxX() - snapshot.minX());
        double spanZ = Math.max(1, snapshot.maxZ() - snapshot.minZ());
        double scaleX = (mapW - 40) / spanX;
        double scaleZ = (mapH - 40) / spanZ;
        viewScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, Math.min(scaleX, scaleZ)));
        viewCenterX = centerX;
        viewCenterZ = centerZ;
        targetViewScale = viewScale;
        targetViewCenterX = viewCenterX;
        targetViewCenterZ = viewCenterZ;
    }
    
    private int adjustColor(int color, int brightness) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        float f = Math.max(0.1f, Math.min(1.0f, brightness / 255.0f));
        int nr = (int) (r * f);
        int ng = (int) (g * f);
        int nb = (int) (b * f);
        return (color & 0xFF000000) | (nr << 16) | (ng << 8) | nb;
    }
    
    private void initStars() {
        int count = Math.max(60, (this.width * this.height) / 30000);
        starX = new int[count];
        starY = new int[count];
        starA = new int[count];
        starS = new int[count];
        Random r = new Random(31937);
        for (int i = 0; i < count; i++) {
            starX[i] = r.nextInt(Math.max(1, this.width));
            starY[i] = r.nextInt(Math.max(1, this.height));
            starA[i] = 80 + r.nextInt(120);
            starS[i] = 1 + r.nextInt(2);
        }
    }
    
    private void renderStars(GuiGraphics g) {
        if (starX == null) return;
        int base = switch (settings.getTheme()) {
            case DARK -> 0xFFFFFF;
            case LIGHT -> 0x8CA0B5;
            case HOLO -> 0x89E0FF;
        };
        for (int i = 0; i < starX.length; i++) {
            int color = (starA[i] << 24) | base;
            g.fill(starX[i], starY[i], starX[i] + starS[i], starY[i] + starS[i], color);
        }
    }
    
    private void renderAmbientLines(GuiGraphics g) {
        int color = switch (settings.getTheme()) {
            case DARK -> 0x0AFFFFFF;
            case LIGHT -> 0x0A000000;
            case HOLO -> 0x1200E5FF;
        };
        for (int y = 0; y < this.height; y += 28) {
            g.hLine(0, this.width, y, color);
        }
    }
    
    private int getPolylineStep() {
        int base = switch (settings.getRenderQuality()) {
            case HIGH -> 1;
            case MEDIUM -> 2;
            case LOW -> 4;
        };
        if (!settings.isOptimizeRender()) return Math.max(1, base);
        if (viewScale < 0.2) return base * 4;
        if (viewScale < 0.4) return base * 2;
        if (viewScale > 2.0) return Math.max(1, base / 2);
        return base;
    }

    private boolean isLineInView(int x1, int y1, int x2, int y2) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        return maxX >= mapX && minX <= mapX + mapW && maxY >= mapY && minY <= mapY + mapH;
    }
}
