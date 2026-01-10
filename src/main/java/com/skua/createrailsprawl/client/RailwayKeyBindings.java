/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端快捷键绑定
 */
public class RailwayKeyBindings {

    public static final KeyMapping OPEN_EDITOR = new KeyMapping(
            "key.createrailsprawl.open_editor",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.createrailsprawl"
    );

    public static final KeyMapping TOGGLE_HUD = new KeyMapping(
            "key.createrailsprawl.toggle_hud",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.createrailsprawl"
    );

    public static void register() {
        // 由 FMLClientSetupEvent 调用
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_EDITOR);
        event.register(TOGGLE_HUD);
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        if (OPEN_EDITOR.consumeClick()) {
            mc.setScreen(new RailwayEditorScreen());
        }

        if (TOGGLE_HUD.consumeClick()) {
            RailwayHudRenderer.toggleHud();
        }
    }
}
