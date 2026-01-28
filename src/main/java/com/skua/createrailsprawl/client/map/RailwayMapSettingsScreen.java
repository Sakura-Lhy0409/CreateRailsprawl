package com.skua.createrailsprawl.client.map;

import com.skua.createrailsprawl.RailwayConfig;
import com.skua.createrailsprawl.runtime.ThreadPoolManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RailwayMapSettingsScreen extends Screen {
    private final Screen parent;
    private final RailwayMapSettings settings = RailwayMapSettings.get();
    private int contentX, contentY, contentW, contentH;
    private Button radiusLabel;
    private Button computeLabel;
    private Button generationLabel;
    private Button dutyLabel;
    private Button spawnerEnableLabel;
    private Button spawnerGenerateLabel;
    private Button spawnerUseLabel;
    private Button themeLabel;
    private Button fullscreenLabel;
    private Button bgLabel;
    private Button zoomLabel;

    public RailwayMapSettingsScreen(Screen parent) {
        super(Component.translatable("gui.createrailsprawl.map_settings.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        contentW = Math.min(720, this.width - 40);
        contentH = Math.min(520, this.height - 40);
        contentX = (this.width - contentW) / 2;
        contentY = (this.height - contentH) / 2;

        int h = 20;
        int gap = 26;
        int colGap = 16;
        int colW = (contentW - colGap * 4) / 3;
        int leftX = contentX + colGap;
        int midX = leftX + colW + colGap;
        int rightX = midX + colW + colGap;
        int leftY = contentY + 40;
        int midY = contentY + 40;
        int rightY = contentY + 40;

        int y = leftY + 18;
        this.addRenderableWidget(ModernButton.create(toggleLabel("gui.createrailsprawl.map_settings.show_grid", settings.isShowGrid()), b -> {
            settings.setShowGrid(!settings.isShowGrid());
            b.setMessage(toggleLabel("gui.createrailsprawl.map_settings.show_grid", settings.isShowGrid()));
        }).bounds(leftX, y, colW, h).build());
        y += gap;

        this.addRenderableWidget(ModernButton.create(toggleLabel("gui.createrailsprawl.map_settings.show_stations", settings.isShowStations()), b -> {
            settings.setShowStations(!settings.isShowStations());
            b.setMessage(toggleLabel("gui.createrailsprawl.map_settings.show_stations", settings.isShowStations()));
        }).bounds(leftX, y, colW, h).build());
        y += gap;

        this.addRenderableWidget(ModernButton.create(toggleLabel("gui.createrailsprawl.map_settings.show_connections", settings.isShowConnections()), b -> {
            settings.setShowConnections(!settings.isShowConnections());
            b.setMessage(toggleLabel("gui.createrailsprawl.map_settings.show_connections", settings.isShowConnections()));
        }).bounds(leftX, y, colW, h).build());
        y += gap;

        this.addRenderableWidget(ModernButton.create(toggleLabel("gui.createrailsprawl.map_settings.show_polylines", settings.isShowRailPolylines()), b -> {
            settings.setShowRailPolylines(!settings.isShowRailPolylines());
            b.setMessage(toggleLabel("gui.createrailsprawl.map_settings.show_polylines", settings.isShowRailPolylines()));
        }).bounds(leftX, y, colW, h).build());
        y += gap;

        this.addRenderableWidget(ModernButton.create(toggleLabel("gui.createrailsprawl.map_settings.show_legend", settings.isShowLegend()), b -> {
            settings.setShowLegend(!settings.isShowLegend());
            b.setMessage(toggleLabel("gui.createrailsprawl.map_settings.show_legend", settings.isShowLegend()));
        }).bounds(leftX, y, colW, h).build());
        y += gap;

        this.addRenderableWidget(ModernButton.create(toggleLabel("gui.createrailsprawl.map_settings.show_player", settings.isShowPlayer()), b -> {
            settings.setShowPlayer(!settings.isShowPlayer());
            b.setMessage(toggleLabel("gui.createrailsprawl.map_settings.show_player", settings.isShowPlayer()));
        }).bounds(leftX, y, colW, h).build());
        y += gap;

        this.addRenderableWidget(ModernButton.create(toggleLabel("gui.createrailsprawl.map_settings.follow_player", settings.isFollowPlayer()), b -> {
            settings.setFollowPlayer(!settings.isFollowPlayer());
            b.setMessage(toggleLabel("gui.createrailsprawl.map_settings.follow_player", settings.isFollowPlayer()));
        }).bounds(leftX, y, colW, h).build());

        y = midY + 18;
        this.addRenderableWidget(ModernButton.create(toggleLabel("gui.createrailsprawl.map_settings.optimize", settings.isOptimizeRender()), b -> {
            settings.setOptimizeRender(!settings.isOptimizeRender());
            b.setMessage(toggleLabel("gui.createrailsprawl.map_settings.optimize", settings.isOptimizeRender()));
        }).bounds(midX, y, colW, h).build());
        y += gap;

        this.addRenderableWidget(ModernButton.create(renderQualityLabel(), b -> {
            settings.setRenderQuality(nextQuality(settings.getRenderQuality()));
            b.setMessage(renderQualityLabel());
        }).bounds(midX, y, colW, h).build());
        y += gap;

        zoomLabel = this.addRenderableWidget(ModernButton.create(zoomSpeedLabelText(), b -> {
            settings.setZoomSpeed(nextZoomSpeed(settings.getZoomSpeed()));
            zoomLabel.setMessage(zoomSpeedLabelText());
        }).bounds(midX, y, colW, h).build());
        y += gap;

        this.addRenderableWidget(ModernButton.create(toggleLabel("gui.createrailsprawl.map_settings.loaded_only", settings.isShowLoadedOnly()), b -> {
            settings.setShowLoadedOnly(!settings.isShowLoadedOnly());
            b.setMessage(toggleLabel("gui.createrailsprawl.map_settings.loaded_only", settings.isShowLoadedOnly()));
            refreshParentSnapshot();
        }).bounds(midX, y, colW, h).build());
        y += gap;

        radiusLabel = this.addRenderableWidget(ModernButton.create(radiusLabelText(), b -> {
            settings.setViewRadiusBlocks(settings.getViewRadiusBlocks() + 256);
            updateRadiusLabel();
            refreshParentSnapshot();
        }).bounds(midX, y, colW, h).build());
        y += gap;

        this.addRenderableWidget(ModernButton.create(Component.translatable("gui.createrailsprawl.map_settings.radius_dec"), b -> {
            settings.setViewRadiusBlocks(settings.getViewRadiusBlocks() - 256);
            updateRadiusLabel();
            refreshParentSnapshot();
        }).bounds(midX, y, (colW - 4) / 2, h).build());
        this.addRenderableWidget(ModernButton.create(Component.translatable("gui.createrailsprawl.map_settings.radius_inc"), b -> {
            settings.setViewRadiusBlocks(settings.getViewRadiusBlocks() + 256);
            updateRadiusLabel();
            refreshParentSnapshot();
        }).bounds(midX + (colW - 4) / 2 + 4, y, (colW - 4) / 2, h).build());

        int ry = rightY + 18;
        computeLabel = this.addRenderableWidget(ModernButton.create(threadLabel("gui.createrailsprawl.map_settings.compute_threads", RailwayConfig.computeThreads), b -> {
            RailwayConfig.computeThreads = nextComputeThreads(RailwayConfig.computeThreads);
            ThreadPoolManager.resizeComputePool(RailwayConfig.computeThreads);
            b.setMessage(threadLabel("gui.createrailsprawl.map_settings.compute_threads", RailwayConfig.computeThreads));
        }).bounds(rightX, ry, colW, h).build());
        ry += gap;
        generationLabel = this.addRenderableWidget(ModernButton.create(threadLabel("gui.createrailsprawl.map_settings.generation_threads", RailwayConfig.generationThreads), b -> {
            RailwayConfig.generationThreads = nextGenerationThreads(RailwayConfig.generationThreads);
            ThreadPoolManager.resizeGenerationPool(RailwayConfig.generationThreads);
            b.setMessage(threadLabel("gui.createrailsprawl.map_settings.generation_threads", RailwayConfig.generationThreads));
        }).bounds(rightX, ry, colW, h).build());
        ry += gap;
        dutyLabel = this.addRenderableWidget(ModernButton.create(threadLabel("gui.createrailsprawl.map_settings.thread_duty", RailwayConfig.threadDutyCycle), b -> {
            RailwayConfig.threadDutyCycle = nextDutyCycle(RailwayConfig.threadDutyCycle);
            b.setMessage(threadLabel("gui.createrailsprawl.map_settings.thread_duty", RailwayConfig.threadDutyCycle));
        }).bounds(rightX, ry, colW, h).build());
        ry += gap;
        spawnerEnableLabel = this.addRenderableWidget(ModernButton.create(toggleLabel("gui.createrailsprawl.map_settings.enable_spawner", RailwayConfig.enableTrackSpawner), b -> {
            RailwayConfig.enableTrackSpawner = !RailwayConfig.enableTrackSpawner;
            b.setMessage(toggleLabel("gui.createrailsprawl.map_settings.enable_spawner", RailwayConfig.enableTrackSpawner));
        }).bounds(rightX, ry, colW, h).build());
        ry += gap;
        spawnerGenerateLabel = this.addRenderableWidget(ModernButton.create(toggleLabel("gui.createrailsprawl.map_settings.generate_spawner", RailwayConfig.generateTrackSpawner), b -> {
            RailwayConfig.generateTrackSpawner = !RailwayConfig.generateTrackSpawner;
            b.setMessage(toggleLabel("gui.createrailsprawl.map_settings.generate_spawner", RailwayConfig.generateTrackSpawner));
        }).bounds(rightX, ry, colW, h).build());
        ry += gap;
        spawnerUseLabel = this.addRenderableWidget(ModernButton.create(toggleLabel("gui.createrailsprawl.map_settings.use_spawner", RailwayConfig.useTrackSpawnerPlaceTrack), b -> {
            RailwayConfig.useTrackSpawnerPlaceTrack = !RailwayConfig.useTrackSpawnerPlaceTrack;
            b.setMessage(toggleLabel("gui.createrailsprawl.map_settings.use_spawner", RailwayConfig.useTrackSpawnerPlaceTrack));
        }).bounds(rightX, ry, colW, h).build());
        ry += gap;

        themeLabel = this.addRenderableWidget(ModernButton.create(themeLabelText(), b -> {
            settings.setTheme(nextTheme(settings.getTheme()));
            b.setMessage(themeLabelText());
        }).bounds(rightX, ry, colW, h).build());
        ry += gap;
        fullscreenLabel = this.addRenderableWidget(ModernButton.create(toggleLabel("gui.createrailsprawl.map_settings.fullscreen_default", settings.isFullscreenDefault()), b -> {
            settings.setFullscreenDefault(!settings.isFullscreenDefault());
            b.setMessage(toggleLabel("gui.createrailsprawl.map_settings.fullscreen_default", settings.isFullscreenDefault()));
        }).bounds(rightX, ry, colW, h).build());
        ry += gap;
        bgLabel = this.addRenderableWidget(ModernButton.create(backgroundLabelText(), b -> {
            settings.setBackgroundBrightness(Math.min(255, settings.getBackgroundBrightness() + 10));
            bgLabel.setMessage(backgroundLabelText());
        }).bounds(rightX, ry, colW, h).build());
        ry += gap;
        this.addRenderableWidget(ModernButton.create(Component.translatable("gui.createrailsprawl.map_settings.bg_dec"), b -> {
            settings.setBackgroundBrightness(Math.max(0, settings.getBackgroundBrightness() - 10));
            bgLabel.setMessage(backgroundLabelText());
        }).bounds(rightX, ry, (colW - 4) / 2, h).build());
        this.addRenderableWidget(ModernButton.create(Component.translatable("gui.createrailsprawl.map_settings.bg_inc"), b -> {
            settings.setBackgroundBrightness(Math.min(255, settings.getBackgroundBrightness() + 10));
            bgLabel.setMessage(backgroundLabelText());
        }).bounds(rightX + (colW - 4) / 2 + 4, ry, (colW - 4) / 2, h).build());

        int bx = contentX + (contentW - 80) / 2; // Center back button
        int by = contentY + contentH - 30;
        this.addRenderableWidget(ModernButton.create(Component.translatable("gui.createrailsprawl.map_settings.back"), b -> {
            if (this.minecraft != null) this.minecraft.setScreen(parent);
        }).bounds(bx, by, 80, 20).build());
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xB0000000);
        int panelColor = switch (settings.getTheme()) {
            case DARK -> 0xFF151A26;
            case LIGHT -> 0xFFF7F9FC;
            case HOLO -> 0xFF0B2137;
        };
        int groupColor = switch (settings.getTheme()) {
            case DARK -> 0xCC151A26;
            case LIGHT -> 0xCCFFFFFF;
            case HOLO -> 0xCC0E2A45;
        };
        int titleColor = switch (settings.getTheme()) {
            case DARK -> 0xFFE5E7EB;
            case LIGHT -> 0xFF1F2937;
            case HOLO -> 0xFF9EE7FF;
        };
        int subtitleColor = switch (settings.getTheme()) {
            case DARK -> 0xFF9AA0AE;
            case LIGHT -> 0xFF6B7280;
            case HOLO -> 0xFF74D2FF;
        };
        MapUIUtils.drawRoundedPanel(g, contentX, contentY, contentW, contentH, panelColor);
        g.drawCenteredString(this.font, this.getTitle(), this.width / 2, contentY + 14, titleColor);
        int colGap = 16;
        int colW = (contentW - colGap * 4) / 3;
        int leftX = contentX + colGap;
        int midX = leftX + colW + colGap;
        int rightX = midX + colW + colGap;
        int boxY = contentY + 36;
        int boxH = contentH - 80;
        MapUIUtils.drawRoundedPanel(g, leftX - 6, boxY, colW + 12, boxH, groupColor);
        MapUIUtils.drawRoundedPanel(g, midX - 6, boxY, colW + 12, boxH, groupColor);
        MapUIUtils.drawRoundedPanel(g, rightX - 6, boxY, colW + 12, boxH, groupColor);
        g.drawCenteredString(this.font, Component.translatable("gui.createrailsprawl.map_settings.group_display"), leftX + colW / 2, boxY - 12, subtitleColor);
        g.drawCenteredString(this.font, Component.translatable("gui.createrailsprawl.map_settings.group_view"), midX + colW / 2, boxY - 12, subtitleColor);
        g.drawCenteredString(this.font, Component.translatable("gui.createrailsprawl.map_settings.group_perf"), rightX + colW / 2, boxY - 12, subtitleColor);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private Component toggleLabel(String key, boolean value) {
        return Component.translatable(key, Component.translatable(value ? "options.on" : "options.off"));
    }

    private Component renderQualityLabel() {
        return Component.translatable("gui.createrailsprawl.map_settings.render_quality",
            Component.translatable(switch (settings.getRenderQuality()) {
                case HIGH -> "gui.createrailsprawl.map_settings.quality_high";
                case MEDIUM -> "gui.createrailsprawl.map_settings.quality_medium";
                case LOW -> "gui.createrailsprawl.map_settings.quality_low";
            }));
    }

    private Component radiusLabelText() {
        return Component.translatable("gui.createrailsprawl.map_settings.radius_label", settings.getViewRadiusBlocks());
    }

    private void updateRadiusLabel() {
        if (radiusLabel != null) {
            radiusLabel.setMessage(radiusLabelText());
        }
    }
    private Component themeLabelText() {
        String key = switch (settings.getTheme()) {
            case DARK -> "gui.createrailsprawl.map_settings.theme_dark";
            case LIGHT -> "gui.createrailsprawl.map_settings.theme_light";
            case HOLO -> "gui.createrailsprawl.map_settings.theme_holo";
        };
        return Component.translatable("gui.createrailsprawl.map_settings.theme", Component.translatable(key));
    }
    private Component backgroundLabelText() {
        return Component.translatable("gui.createrailsprawl.map_settings.background_brightness", settings.getBackgroundBrightness());
    }
    private Component zoomSpeedLabelText() {
        String key = switch (settings.getZoomSpeed()) {
            case 1 -> "gui.createrailsprawl.map_settings.zoom_slow";
            case 2 -> "gui.createrailsprawl.map_settings.zoom_normal";
            default -> "gui.createrailsprawl.map_settings.zoom_fast";
        };
        return Component.translatable("gui.createrailsprawl.map_settings.zoom_speed", Component.translatable(key));
    }

    private void refreshParentSnapshot() {
        if (parent instanceof RailwayMapScreen map) {
            map.refreshSnapshot();
        }
    }

    private Component threadLabel(String key, int value) {
        return Component.translatable(key, value);
    }

    private int nextComputeThreads(int current) {
        int[] options = new int[] {0, 1, 2, 4, 6, 8};
        return nextOption(options, current);
    }

    private int nextGenerationThreads(int current) {
        int[] options = new int[] {1, 2, 4, 6, 8};
        return nextOption(options, current);
    }

    private int nextDutyCycle(int current) {
        int[] options = new int[] {30, 50, 70, 90};
        return nextOption(options, current);
    }

    private int nextOption(int[] options, int current) {
        for (int i = 0; i < options.length; i++) {
            if (options[i] == current) {
                return options[(i + 1) % options.length];
            }
        }
        return options[0];
    }

    private RailwayMapSettings.RenderQuality nextQuality(RailwayMapSettings.RenderQuality q) {
        return switch (q) {
            case HIGH -> RailwayMapSettings.RenderQuality.MEDIUM;
            case MEDIUM -> RailwayMapSettings.RenderQuality.LOW;
            case LOW -> RailwayMapSettings.RenderQuality.HIGH;
        };
    }
    private RailwayMapSettings.Theme nextTheme(RailwayMapSettings.Theme t) {
        return switch (t) {
            case DARK -> RailwayMapSettings.Theme.LIGHT;
            case LIGHT -> RailwayMapSettings.Theme.HOLO;
            case HOLO -> RailwayMapSettings.Theme.DARK;
        };
    }
    private int nextZoomSpeed(int v) {
        return v >= 3 ? 1 : v + 1;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
