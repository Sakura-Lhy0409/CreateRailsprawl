/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.client;

import com.skua.createrailsprawl.CreateRailsprawl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 铁轨生成编辑器GUI
 * 允许玩家配置生成参数并触发生成
 */
@OnlyIn(Dist.CLIENT)
public class RailwayEditorScreen extends Screen {

    private EditBox radiusInput;
    private int radius = 3;

    public RailwayEditorScreen() {
        super(Component.translatable("gui.createrailsprawl.editor.title"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 半径输入框
        radiusInput = new EditBox(this.font, centerX - 50, centerY - 30, 100, 20,
                Component.translatable("gui.createrailsprawl.editor.radius"));
        radiusInput.setValue(String.valueOf(radius));
        radiusInput.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.addRenderableWidget(radiusInput);

        // 生成按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.createrailsprawl.editor.generate"),
                btn -> onGenerate()
        ).bounds(centerX - 60, centerY + 10, 120, 20).build());

        // 取消按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.createrailsprawl.editor.cancel"),
                btn -> onClose()
        ).bounds(centerX - 60, centerY + 35, 120, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        // 标题
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // 半径标签
        graphics.drawString(this.font,
                Component.translatable("gui.createrailsprawl.editor.radius"),
                this.width / 2 - 50, this.height / 2 - 42, 0xAAAAAA);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void onGenerate() {
        try {
            radius = Integer.parseInt(radiusInput.getValue());
            radius = Math.max(1, Math.min(radius, 16));
        } catch (NumberFormatException e) {
            radius = 3;
        }

        // 发送命令到服务器
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.connection.sendCommand("railway generate " + radius);
        }

        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
