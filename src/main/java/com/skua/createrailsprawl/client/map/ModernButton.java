package com.skua.createrailsprawl.client.map;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

public class ModernButton extends Button {
    protected ModernButton(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration onCreateNarration) {
        super(x, y, width, height, message, onPress, onCreateNarration);
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int color;
        if (this.active) {
            color = this.isHoveredOrFocused() ? 0xFF4CC9F0 : 0xFF3A86E0; // Accent color
        } else {
            color = 0xFF2D3748;
        }

        MapUIUtils.drawRoundedPanel(g, this.getX(), this.getY(), this.width, this.height, color);
        
        int textColor = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
        g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);
    }

    public static Builder create(Component message, OnPress onPress) {
        return new Builder(message, onPress);
    }

    public static class Builder {
        private final Component message;
        private final OnPress onPress;
        private int x, y, width, height;
        private CreateNarration createNarration = Button.DEFAULT_NARRATION;
        private Tooltip tooltip;

        public Builder(Component message, OnPress onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }
        
        public Builder tooltip(Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public ModernButton build() {
            ModernButton btn = new ModernButton(x, y, width, height, message, onPress, createNarration);
            if (tooltip != null) btn.setTooltip(tooltip);
            return btn;
        }
    }
}
