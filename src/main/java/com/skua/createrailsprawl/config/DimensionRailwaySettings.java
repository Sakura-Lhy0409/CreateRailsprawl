package com.skua.createrailsprawl.config;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 维度铁路配置 - 支持按维度覆盖全局配置
 */
public class DimensionRailwaySettings {
    private static final Map<ResourceLocation, DimensionRailwaySettings> DIMENSION_SETTINGS = new ConcurrentHashMap<>();

    // 维度特定配置（null 表示使用全局配置）
    private Boolean enabled;
    private Double elevationWeight;
    private Double heuristicWeight;

    public DimensionRailwaySettings() {}

    /**
     * 获取维度配置，不存在则返回 null
     */
    public static DimensionRailwaySettings get(ResourceLocation dimension) {
        return DIMENSION_SETTINGS.get(dimension);
    }

    /**
     * 设置维度配置
     */
    public static void set(ResourceLocation dimension, DimensionRailwaySettings settings) {
        if (dimension != null && settings != null) {
            DIMENSION_SETTINGS.put(dimension, settings);
        }
    }

    /**
     * 移除维度配置
     */
    public static void remove(ResourceLocation dimension) {
        DIMENSION_SETTINGS.remove(dimension);
    }

    /**
     * 清除所有维度配置
     */
    public static void clearAll() {
        DIMENSION_SETTINGS.clear();
    }

    /**
     * 检查维度是否启用铁路生成
     */
    public static boolean isEnabled(ResourceLocation dimension) {
        DimensionRailwaySettings settings = get(dimension);
        if (settings != null && settings.enabled != null) {
            return settings.enabled;
        }
        return com.skua.createrailsprawl.RailwayConfig.enableTrackSpawner;
    }

    /**
     * 获取维度的高度权重
     */
    public static double getElevationWeight(ResourceLocation dimension) {
        DimensionRailwaySettings settings = get(dimension);
        if (settings != null && settings.elevationWeight != null) {
            return settings.elevationWeight;
        }
        return com.skua.createrailsprawl.RailwayConfig.elevationWeight;
    }

    /**
     * 获取维度的启发式权重
     */
    public static double getHeuristicWeight(ResourceLocation dimension) {
        DimensionRailwaySettings settings = get(dimension);
        if (settings != null && settings.heuristicWeight != null) {
            return settings.heuristicWeight;
        }
        return com.skua.createrailsprawl.RailwayConfig.heuristicWeight;
    }

    // Getters and Setters
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Double getElevationWeight() { return elevationWeight; }
    public void setElevationWeight(Double elevationWeight) { this.elevationWeight = elevationWeight; }

    public Double getHeuristicWeight() { return heuristicWeight; }
    public void setHeuristicWeight(Double heuristicWeight) { this.heuristicWeight = heuristicWeight; }
}
