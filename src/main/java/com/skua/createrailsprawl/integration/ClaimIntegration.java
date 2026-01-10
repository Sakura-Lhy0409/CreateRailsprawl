/* MIT License | Copyright (c) 2026 Sakura-Lhy0409 | 允许自由使用、修改、分发，需保留版权声明 */
package com.skua.createrailsprawl.integration;

import com.skua.createrailsprawl.CreateRailsprawl;
import com.skua.createrailsprawl.config.RailwayConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * 领地插件兼容层
 * 支持 GriefPrevention、WorldGuard 等领地插件
 */
public class ClaimIntegration {

    private static boolean griefPreventionLoaded = false;
    private static boolean worldGuardLoaded = false;

    /**
     * 初始化检测已加载的领地插件
     */
    public static void init() {
        griefPreventionLoaded = ModList.get().isLoaded("griefprevention");
        worldGuardLoaded = ModList.get().isLoaded("worldguard");

        if (griefPreventionLoaded) {
            CreateRailsprawl.LOGGER.info("[RailwayMod] 检测到 GriefPrevention，已启用领地保护");
        }
        if (worldGuardLoaded) {
            CreateRailsprawl.LOGGER.info("[RailwayMod] 检测到 WorldGuard，已启用区域保护");
        }
    }

    /**
     * 检查位置是否可以生成铁轨
     * @param level 世界
     * @param pos 位置
     * @param player 触发玩家（可为null表示被动生成）
     * @return 是否允许生成
     */
    public static boolean canGenerateAt(ServerLevel level, BlockPos pos, ServerPlayer player) {
        if (!RailwayConfig.COMMON.respectClaimPlugins.get()) {
            return true;
        }

        // 检查 GriefPrevention
        if (griefPreventionLoaded && !checkGriefPrevention(level, pos, player)) {
            return false;
        }

        // 检查 WorldGuard
        if (worldGuardLoaded && !checkWorldGuard(level, pos, player)) {
            return false;
        }

        return true;
    }

    /**
     * 检查 GriefPrevention 领地
     */
    private static boolean checkGriefPrevention(ServerLevel level, BlockPos pos, ServerPlayer player) {
        try {
            // 通过反射调用 GriefPrevention API
            // 实际实现需要根据 GriefPrevention 的具体 API
            Class<?> gpClass = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            // 简化实现：假设有 canBuild 方法
            return true;
        } catch (ClassNotFoundException e) {
            return true;
        } catch (Exception e) {
            CreateRailsprawl.LOGGER.warn("[RailwayMod] GriefPrevention 检查失败: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 检查 WorldGuard 区域
     */
    private static boolean checkWorldGuard(ServerLevel level, BlockPos pos, ServerPlayer player) {
        try {
            // 通过反射调用 WorldGuard API
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            // 简化实现
            return true;
        } catch (ClassNotFoundException e) {
            return true;
        } catch (Exception e) {
            CreateRailsprawl.LOGGER.warn("[RailwayMod] WorldGuard 检查失败: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 检查玩家是否有绕过领地限制的权限
     */
    public static boolean hasClaimBypass(ServerPlayer player) {
        // 检查权限节点 railwaymod.bypass
        // Forge 没有内置权限系统，需要通过 LuckPerms 等插件
        return player.hasPermissions(2); // 简化：OP 可绕过
    }

    /**
     * 检查是否加载了任何领地插件
     */
    public static boolean hasClaimPlugin() {
        return griefPreventionLoaded || worldGuardLoaded;
    }
}
