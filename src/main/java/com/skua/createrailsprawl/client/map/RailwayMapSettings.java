package com.skua.createrailsprawl.client.map;

public final class RailwayMapSettings {
    public enum RenderQuality { HIGH, MEDIUM, LOW }
    public enum Theme { DARK, LIGHT, HOLO }
    private static final RailwayMapSettings INSTANCE = new RailwayMapSettings();
    private boolean showGrid = true;
    private boolean showStations = true;
    private boolean showConnections = true;
    private boolean showRailPolylines = true;
    private boolean showLegend = true;
    private boolean showPlayer = true;
    private boolean followPlayer = false;
    private boolean optimizeRender = true;
    private RenderQuality renderQuality = RenderQuality.MEDIUM;
    private boolean showLoadedOnly = true;
    private int viewRadiusBlocks = 2048;
    private Theme theme = Theme.DARK;
    private int backgroundBrightness = 180;
    private boolean fullscreenDefault = true;
    private int zoomSpeed = 2;

    private RailwayMapSettings() {}
    public static RailwayMapSettings get() { return INSTANCE; }

    public boolean isShowGrid() { return showGrid; }
    public void setShowGrid(boolean v) { showGrid = v; }
    public boolean isShowStations() { return showStations; }
    public void setShowStations(boolean v) { showStations = v; }
    public boolean isShowConnections() { return showConnections; }
    public void setShowConnections(boolean v) { showConnections = v; }
    public boolean isShowRailPolylines() { return showRailPolylines; }
    public void setShowRailPolylines(boolean v) { showRailPolylines = v; }
    public boolean isShowLegend() { return showLegend; }
    public void setShowLegend(boolean v) { showLegend = v; }
    public boolean isShowPlayer() { return showPlayer; }
    public void setShowPlayer(boolean v) { showPlayer = v; }
    public boolean isFollowPlayer() { return followPlayer; }
    public void setFollowPlayer(boolean v) { followPlayer = v; }
    public boolean isOptimizeRender() { return optimizeRender; }
    public void setOptimizeRender(boolean v) { optimizeRender = v; }
    public RenderQuality getRenderQuality() { return renderQuality; }
    public void setRenderQuality(RenderQuality q) { renderQuality = q; }
    public boolean isShowLoadedOnly() { return showLoadedOnly; }
    public void setShowLoadedOnly(boolean v) { showLoadedOnly = v; }
    public int getViewRadiusBlocks() { return viewRadiusBlocks; }
    public void setViewRadiusBlocks(int v) { viewRadiusBlocks = Math.max(256, Math.min(8192, v)); }
    public Theme getTheme() { return theme; }
    public void setTheme(Theme t) { theme = t; }
    public int getBackgroundBrightness() { return backgroundBrightness; }
    public void setBackgroundBrightness(int v) { backgroundBrightness = Math.max(0, Math.min(255, v)); }
    public boolean isFullscreenDefault() { return fullscreenDefault; }
    public void setFullscreenDefault(boolean v) { fullscreenDefault = v; }
    public int getZoomSpeed() { return zoomSpeed; }
    public void setZoomSpeed(int v) { zoomSpeed = Math.max(1, Math.min(3, v)); }
}
