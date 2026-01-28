package com.skua.createrailsprawl.client.map;

import net.minecraft.client.gui.GuiGraphics;

public class MapUIUtils {
    
    public static void drawRoundedPanel(GuiGraphics g, int x, int y, int w, int h, int color) {
        int r = 4; // corner radius approximation
        // Center
        g.fill(x + r, y, x + w - r, y + h, color);
        // Left
        g.fill(x, y + r, x + r, y + h - r, color);
        // Right
        g.fill(x + w - r, y + r, x + w, y + h - r, color);
        
        // Corners - just simple pixels for now to look "rounded"
        // Top-Left
        g.fill(x + 1, y + 1, x + r, y + r, color);
        // Top-Right
        g.fill(x + w - r, y + 1, x + w - 1, y + r, color);
        // Bottom-Left
        g.fill(x + 1, y + h - r, x + r, y + h - 1, color);
        // Bottom-Right
        g.fill(x + w - r, y + h - r, x + w - 1, y + h - 1, color);
    }

    public static void drawCapsule(GuiGraphics g, int x, int y, int w, int h, int color) {
        drawRoundedPanel(g, x, y, w, h, color);
    }
    
    public static double lerp(double start, double end, double delta) {
        return start + (end - start) * delta;
    }
}
