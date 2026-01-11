package com.skua.createrailsprawl.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.client.map.RailwayMapScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 快捷键绑定
 */
@Mod.EventBusSubscriber(modid = CreateRailsprawl.MOD_ID, value = Dist.CLIENT)
public class ModKeyBindings {
    
    public static final KeyMapping OPEN_RAILWAY_MAP = new KeyMapping(
        "key.createrailsprawl.open_railway_map",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_N,
        "key.categories.createrailsprawl"
    );

    @Mod.EventBusSubscriber(modid = CreateRailsprawl.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEvents {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_RAILWAY_MAP);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        
        if (OPEN_RAILWAY_MAP.consumeClick()) {
            mc.setScreen(new RailwayMapScreen());
        }
    }
}
