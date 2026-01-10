/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.integration;

import com.skua.createrailsprawl.CreateRailsprawl;
import net.minecraftforge.fml.ModList;

/**
 * Tectonic 地形mod兼容层
 * 适配 Tectonic 的垂直缩放、扩展高度等特性
 */
public class TectonicIntegration {

    private static boolean tectonicLoaded = false;

    /**
     * 初始化检测 Tectonic mod
     */
    public static void init() {
        tectonicLoaded = ModList.get().isLoaded("tectonic");
        if (tectonicLoaded) {
            CreateRailsprawl.LOGGER.info("[RailwayMod] 检测到 Tectonic，已启用地形适配");
        }
    }

    /**
     * 检查 Tectonic 是否已加载
     */
    public static boolean isTectonicLoaded() {
        return tectonicLoaded;
    }

    /**
     * 获取适配后的采样步长
     * Tectonic 地形更陡峭，需要更密集的采样
     */
    public static int getTerrainSampleStep() {
        return tectonicLoaded ? 2 : 4;
    }

    /**
     * 获取适配后的坡度阈值
     * Tectonic 地形坡度更大，提高隧道触发阈值
     */
    public static int getSlopeThreshold() {
        return tectonicLoaded ? 8 : 4;
    }
}
